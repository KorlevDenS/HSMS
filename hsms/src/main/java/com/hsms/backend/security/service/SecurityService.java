package com.hsms.backend.security.service;

import com.hsms.backend.auth.model.HsmsUser;
import com.hsms.backend.common.HsmsAccessService;
import com.hsms.backend.common.HsmsAuditService;
import com.hsms.backend.common.AlarmRequest;
import com.hsms.backend.common.AlarmResponse;
import com.hsms.backend.common.ClassificationRequest;
import com.hsms.backend.common.EvacuationCommandDto;
import com.hsms.backend.common.EvacuationRequest;
import com.hsms.backend.common.EvacuationStatus;
import com.hsms.backend.common.IncidentDto;
import com.hsms.backend.common.IncidentStatus;
import com.hsms.backend.common.InsuranceCaseDto;
import com.hsms.backend.common.InsuranceCaseOpenRequest;
import com.hsms.backend.common.InsuranceTrigger;
import com.hsms.backend.common.MissionDto;
import com.hsms.backend.common.MissionStatus;
import com.hsms.backend.common.RiskSnapshotDto;
import com.hsms.backend.common.RoleCode;
import com.hsms.backend.common.Severity;
import com.hsms.backend.insurance.api.InsuranceApi;
import com.hsms.backend.mission.api.MissionApi;
import com.hsms.backend.readmodel.HsmsDtoAssembler;
import com.hsms.backend.risk.api.RiskApi;
import com.hsms.backend.harvester.model.Crew;
import com.hsms.backend.security.api.SecurityApi;
import com.hsms.backend.security.model.AlarmSignal;
import com.hsms.backend.security.model.EvacuationCommand;
import com.hsms.backend.security.model.Incident;
import com.hsms.backend.security.repository.AlarmSignalRepository;
import com.hsms.backend.security.repository.EvacuationCommandRepository;
import com.hsms.backend.security.repository.IncidentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.hsms.backend.common.HsmsOps.*;

@Service
@Transactional
public class SecurityService implements SecurityApi {

    private static final Duration INCIDENT_SLA = Duration.ofMinutes(5);
    private static final Duration EVACUATION_ACK_TIMEOUT = Duration.ofMinutes(5);

    private final HsmsAccessService access;
    private final HsmsAuditService audit;
    private final HsmsDtoAssembler dto;
    private final IncidentRepository incidentRepository;
    private final AlarmSignalRepository alarmSignalRepository;
    private final EvacuationCommandRepository evacuationCommandRepository;
    private final RiskApi riskApi;
    private final MissionApi missionApi;
    private final InsuranceApi insuranceApi;
    private final Counter createdIncidents;
    private final Counter evacuationCommands;
    private final Counter slaBreaches;

    public SecurityService(
            HsmsAccessService access,
            HsmsAuditService audit,
            HsmsDtoAssembler dto,
            IncidentRepository incidentRepository,
            AlarmSignalRepository alarmSignalRepository,
            EvacuationCommandRepository evacuationCommandRepository,
            RiskApi riskApi,
            MissionApi missionApi,
            InsuranceApi insuranceApi,
            MeterRegistry meterRegistry
    ) {
        this.access = access;
        this.audit = audit;
        this.dto = dto;
        this.incidentRepository = incidentRepository;
        this.alarmSignalRepository = alarmSignalRepository;
        this.evacuationCommandRepository = evacuationCommandRepository;
        this.riskApi = riskApi;
        this.missionApi = missionApi;
        this.insuranceApi = insuranceApi;
        this.createdIncidents = meterRegistry.counter("hsms_incidents_created_total");
        this.evacuationCommands = meterRegistry.counter("hsms_evacuation_commands_total");
        this.slaBreaches = meterRegistry.counter("hsms_incidents_sla_breached_total");
    }

    @Override
    public AlarmResponse submitAlarm(String actorLogin, long missionId, AlarmRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_HARVESTER_CREW, RoleCode.ROLE_ADMINISTRATOR);
        MissionDto mission = dto.mission(missionId);
        if (mission.status() != MissionStatus.ACTIVE) {
            throw badRequest("Тревогу можно отправить только по активному рейсу", "Проверьте статус рейса.");
        }
        requireAssignedCrewActor(actor, missionId, "Отправляйте тревогу только от пользователя, связанного с экипажем рейса.");
        String normalizedExternalId = requireExternalEventId(request);
        var existing = dto.alarmByExternalId(missionId, normalizedExternalId);
        if (existing.isPresent()) {
            IncidentDto incident = dto.incident(existing.get().incidentId());
            return new AlarmResponse(existing.get().alarmId(), incident.id(), "Повторная тревога добавлена к существующему инциденту", incident);
        }

        Instant now = Instant.now();
        Incident incident = incidentRepository
                .findFirstByMissionIdAndStatusNotOrderBySlaStartedAtAscIdAsc(missionId, IncidentStatus.CLOSED)
                .orElse(null);
        boolean incidentCreated = false;
        if (incident == null) {
            incidentCreated = true;
            incident = new Incident();
            incident.setMissionId(missionId);
            incident.setStatus(IncidentStatus.OPEN);
            incident.setSeverity(Severity.HIGH);
            incident.setSlaStartedAt(now);
            incident.setSlaDeadlineAt(now.plus(INCIDENT_SLA));
            incident.setSlaBreached(false);
            incident = incidentRepository.saveAndFlush(incident);
            createdIncidents.increment();
        }

        AlarmSignal alarm = new AlarmSignal();
        alarm.setExternalEventId(normalizedExternalId);
        alarm.setMissionId(missionId);
        alarm.setSender(actor.getLogin());
        alarm.setEventTime(request.eventTime() == null ? now : request.eventTime());
        alarm.setReceivedAt(now);
        alarm.setReason(blankToDefault(request.reason(), "Тревожный сигнал экипажа"));
        alarm.setIncidentId(incident.getId());
        AlarmSignal savedAlarm = alarmSignalRepository.saveAndFlush(alarm);
        incident.setAlarmSignalId(savedAlarm.getId());
        incidentRepository.saveAndFlush(incident);

        RiskSnapshotDto risk = riskApi.recalculateAfterDomainChange(actorLogin, missionId, "Получен тревожный сигнал", true);
        missionApi.recordRiskAssessment(actorLogin, missionId, risk.id(), risk.calculatedAt());
        audit.record(actor, "ALARM_RECEIVED", "alarm_signal", savedAlarm.getId(), missionId, Map.of(
                "incidentId", incident.getId(),
                "reason", savedAlarm.getReason()
        ));
        if (incidentCreated) {
            audit.record(actor, "INCIDENT_CREATED", "incident", incident.getId(), missionId, Map.of("alarmId", savedAlarm.getId()));
        }
        return new AlarmResponse(savedAlarm.getId(), incident.getId(), "Тревога принята, инцидент зарегистрирован", dto.incident(incident.getId()));
    }

    @Override
    public List<IncidentDto> incidentQueue() {
        monitorOperationalTimers();
        return dto.incidentQueue();
    }

    @Override
    public IncidentDto incident(long incidentId) {
        Incident incident = dto.incidentEntity(incidentId);
        updateSlaIfNeeded(incident, null);
        return dto.incident(incidentId);
    }

    @Override
    public IncidentDto classifyIncident(String actorLogin, long incidentId, ClassificationRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_SECURITY_HEADQUARTERS_OPERATOR, RoleCode.ROLE_ADMINISTRATOR);
        Incident incident = dto.incidentEntity(incidentId);
        updateSlaIfNeeded(incident, actor);
        Severity severity = request == null || request.severity() == null ? Severity.MEDIUM : request.severity();
        IncidentStatus status = severity == Severity.LOW ? IncidentStatus.MONITORING : IncidentStatus.CLASSIFIED;
        String reason = blankToDefault(request == null ? null : request.reason(), "Классификация оператора штаба");
        incident.setSeverity(severity);
        incident.setStatus(status);
        incident.setClassificationReason(reason);
        incident.setOperatorLogin(actor.getLogin());
        audit.record(actor, "INCIDENT_CLASSIFIED", "incident", incidentId, incident.getMissionId(), Map.of(
                "severity", severity.name(),
                "reason", reason
        ));
        return dto.incident(incidentId);
    }

    @Override
    public EvacuationCommandDto issueEvacuation(String actorLogin, long incidentId, EvacuationRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_SECURITY_HEADQUARTERS_OPERATOR, RoleCode.ROLE_ADMINISTRATOR);
        Incident incident = dto.incidentEntity(incidentId);
        updateSlaIfNeeded(incident, actor);
        if (incident.getEvacuationCommandId() != null) {
            EvacuationCommand existing = evacuationCommandRepository.findById(incident.getEvacuationCommandId()).orElse(null);
            if (existing != null
                    && existing.getStatus() != EvacuationStatus.CANCELLED
                    && existing.getStatus() != EvacuationStatus.EXPIRED
                    && existing.getStatus() != EvacuationStatus.DELIVERY_FAILED) {
                return dto.evacuation(existing.getId()).orElseThrow();
            }
        }

        Instant now = Instant.now();
        EvacuationCommand command = new EvacuationCommand();
        command.setIncidentId(incidentId);
        command.setMissionId(incident.getMissionId());
        command.setStatus(EvacuationStatus.CREATED);
        command.setSentAt(now);
        command.setSentBy(actor.getLogin());
        command.setExpiresAt(now.plus(EVACUATION_ACK_TIMEOUT));
        EvacuationCommand saved = evacuationCommandRepository.saveAndFlush(command);
        incident.setEvacuationCommandId(saved.getId());
        incident.setStatus(IncidentStatus.EVACUATION_ORDERED);
        evacuationCommands.increment();
        String reason = blankToDefault(request == null ? null : request.reason(), "Решение штаба безопасности");
        audit.record(actor, "EVACUATION_COMMAND_CREATED", "evacuation_command", saved.getId(), incident.getMissionId(), Map.of(
                "incidentId", incidentId,
                "reason", reason
        ));
        saved.setStatus(EvacuationStatus.SENT);
        evacuationCommandRepository.saveAndFlush(saved);
        audit.record(actor, "EVACUATION_COMMAND_SENT", "evacuation_command", saved.getId(), incident.getMissionId(), Map.of(
                "incidentId", incidentId,
                "expiresAt", saved.getExpiresAt()
        ));
        return dto.evacuation(saved.getId()).orElseThrow();
    }

    @Override
    public EvacuationCommandDto markEvacuationDelivered(String actorLogin, long incidentId) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_HARVESTER_CREW, RoleCode.ROLE_ADMINISTRATOR);
        Incident incident = dto.incidentEntity(incidentId);
        requireAssignedCrewActor(actor, incident.getMissionId(), "Фиксируйте доставку эвакуации только от пользователя, связанного с экипажем рейса.");
        if (incident.getEvacuationCommandId() == null) {
            throw badRequest("Команда эвакуации еще не отправлена", "Дождитесь решения штаба безопасности.");
        }
        EvacuationCommand command = evacuationCommandRepository.findById(incident.getEvacuationCommandId())
                .orElseThrow(() -> badRequest("Команда эвакуации еще не отправлена", "Дождитесь решения штаба безопасности."));
        if (command.getStatus() == EvacuationStatus.SENT || command.getStatus() == EvacuationStatus.CREATED) {
            command.setStatus(EvacuationStatus.DELIVERED);
            command.setDeliveredAt(Instant.now());
            audit.record(actor, "EVACUATION_DELIVERED", "evacuation_command", command.getId(), incident.getMissionId(), Map.of("incidentId", incidentId));
        }
        return dto.evacuation(command.getId()).orElseThrow();
    }

    @Override
    public EvacuationCommandDto acknowledgeEvacuation(String actorLogin, long incidentId) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_HARVESTER_CREW, RoleCode.ROLE_ADMINISTRATOR);
        Incident incident = dto.incidentEntity(incidentId);
        requireAssignedCrewActor(actor, incident.getMissionId(), "Подтверждайте эвакуацию только от пользователя, связанного с экипажем рейса.");
        if (incident.getEvacuationCommandId() == null) {
            throw badRequest("Команда эвакуации еще не отправлена", "Дождитесь решения штаба безопасности.");
        }
        EvacuationCommand command = evacuationCommandRepository.findById(incident.getEvacuationCommandId())
                .orElseThrow(() -> badRequest("Команда эвакуации еще не отправлена", "Дождитесь решения штаба безопасности."));
        if (command.getStatus() == EvacuationStatus.EXPIRED || command.getStatus() == EvacuationStatus.DELIVERY_FAILED) {
            throw badRequest("Команда эвакуации не может быть подтверждена", "Повторите отправку команды после восстановления канала связи.");
        }
        Instant now = Instant.now();
        command.setStatus(EvacuationStatus.ACKNOWLEDGED);
        if (command.getDeliveredAt() == null) {
            command.setDeliveredAt(now);
        }
        command.setAcknowledgedAt(now);
        command.setAcknowledgedBy(actor.getLogin());
        incident.setStatus(IncidentStatus.EVACUATION_ACKNOWLEDGED);
        audit.record(actor, "EVACUATION_ACKNOWLEDGED", "evacuation_command", command.getId(), incident.getMissionId(), Map.of("incidentId", incidentId));
        return dto.evacuation(command.getId()).orElseThrow();
    }

    @Override
    public EvacuationCommandDto markEvacuationDeliveryFailed(String actorLogin, long incidentId, EvacuationRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_SECURITY_HEADQUARTERS_OPERATOR, RoleCode.ROLE_ADMINISTRATOR);
        Incident incident = dto.incidentEntity(incidentId);
        if (incident.getEvacuationCommandId() == null) {
            throw badRequest("Команда эвакуации еще не отправлена", "Сначала отправьте команду эвакуации.");
        }
        EvacuationCommand command = evacuationCommandRepository.findById(incident.getEvacuationCommandId())
                .orElseThrow(() -> badRequest("Команда эвакуации еще не отправлена", "Сначала отправьте команду эвакуации."));
        if (command.getStatus() == EvacuationStatus.ACKNOWLEDGED) {
            throw badRequest("Доставка уже подтверждена экипажем", "Сбой доставки нельзя отметить после подтверждения.");
        }
        if (command.getStatus() == EvacuationStatus.CANCELLED) {
            throw badRequest("Команда эвакуации отменена", "Отправьте новую команду эвакуации.");
        }
        String reason = blankToDefault(request == null ? null : request.reason(), "Канал связи не подтвердил доставку команды эвакуации");
        command.setStatus(EvacuationStatus.DELIVERY_FAILED);
        command.setDeliveryError(reason);
        command.setDeliveredAt(null);
        command.setExpiresAt(null);
        audit.record(actor, "EVACUATION_DELIVERY_FAILED", "evacuation_command", command.getId(), incident.getMissionId(), Map.of(
                "incidentId", incidentId,
                "reason", reason
        ));
        return dto.evacuation(command.getId()).orElseThrow();
    }

    @Override
    public IncidentDto closeIncident(String actorLogin, long incidentId) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_SECURITY_HEADQUARTERS_OPERATOR, RoleCode.ROLE_ADMINISTRATOR);
        Incident incident = dto.incidentEntity(incidentId);
        updateSlaIfNeeded(incident, actor);
        Instant now = Instant.now();
        incident.setStatus(IncidentStatus.CLOSED);
        incident.setClosedAt(now);
        incident.setClosedBy(actor.getLogin());
        InsuranceTrigger trigger = incident.isSlaBreached() ? InsuranceTrigger.SLA_BREACH : InsuranceTrigger.INCIDENT;
        InsuranceCaseDto insurance = insuranceApi.openInsuranceCase(actorLogin, new InsuranceCaseOpenRequest(
                incident.getMissionId(),
                incidentId,
                trigger,
                incident.getClassificationReason(),
                null,
                null,
                null,
                now,
                actor.getLogin(),
                incident.getSeverity(),
                incident.getSlaStartedAt(),
                now,
                incident.isSlaBreached(),
                actor.getLogin(),
                null
        ));
        audit.record(actor, "INCIDENT_CLOSED", "incident", incidentId, incident.getMissionId(), Map.of("insuranceCaseId", insurance.id()));
        return dto.incident(incidentId);
    }

    @Override
    @Transactional(readOnly = true)
    public String incidentReportCsv() {
        return dto.incidentReportCsv();
    }

    @Scheduled(fixedDelayString = "${hsms.operational-timer-scan-ms:1000}")
    public void monitorOperationalTimers() {
        HsmsUser system = access.systemUser();
        Instant now = Instant.now();
        incidentRepository.findByStatusNotOrderBySlaDeadlineAtAscIdAsc(IncidentStatus.CLOSED)
                .forEach(incident -> updateSlaIfNeeded(incident, system));
        evacuationCommandRepository.findByStatusInAndExpiresAtBefore(List.of(EvacuationStatus.CREATED, EvacuationStatus.SENT, EvacuationStatus.DELIVERED), now)
                .forEach(command -> {
                    command.setStatus(EvacuationStatus.EXPIRED);
                    command.setDeliveryError("Экипаж не подтвердил получение команды эвакуации до истечения тайм-аута");
                    audit.record(system, "EVACUATION_ACK_EXPIRED", "evacuation_command", command.getId(), command.getMissionId(), Map.of(
                            "incidentId", command.getIncidentId(),
                            "expiresAt", command.getExpiresAt()
                    ));
                });
    }

    private void updateSlaIfNeeded(Incident incident, HsmsUser actor) {
        if (!incident.isSlaBreached()
                && incident.getSlaDeadlineAt() != null
                && Instant.now().isAfter(incident.getSlaDeadlineAt())
                && incident.getStatus() == IncidentStatus.OPEN) {
            incident.setSlaBreached(true);
            incident.setSlaBreachedNotifiedAt(Instant.now());
            slaBreaches.increment();
            if (actor != null) {
                audit.record(actor, "INCIDENT_SLA_BREACHED", "incident", incident.getId(), incident.getMissionId(), Map.of("deadline", incident.getSlaDeadlineAt()));
            }
        }
    }

    private void requireAssignedCrewActor(HsmsUser actor, long missionId, String action) {
        if (access.roles(actor).contains(RoleCode.ROLE_ADMINISTRATOR)) {
            return;
        }
        MissionDto mission = dto.mission(missionId);
        Crew crew = dto.crewEntity(mission.crewId());
        if (!actor.getLogin().equals(crew.getAssignedLogin())) {
            throw forbidden("Экипаж не назначен на этот рейс", action);
        }
    }

    private String requireExternalEventId(AlarmRequest request) {
        String externalEventId = request == null ? null : request.externalEventId();
        if (externalEventId == null || externalEventId.isBlank()) {
            throw badRequest("Не указан идентификатор тревожного сигнала", "Передайте externalEventId для дедупликации.");
        }
        return externalEventId.trim();
    }
}
