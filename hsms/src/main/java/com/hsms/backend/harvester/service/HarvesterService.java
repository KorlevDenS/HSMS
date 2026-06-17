package com.hsms.backend.harvester.service;

import com.hsms.backend.auth.model.HsmsUser;
import com.hsms.backend.auth.repository.HsmsUserRepository;
import com.hsms.backend.common.HsmsAccessService;
import com.hsms.backend.common.HsmsAuditService;
import com.hsms.backend.common.CrewCreateRequest;
import com.hsms.backend.common.CrewDto;
import com.hsms.backend.common.FreshnessStatus;
import com.hsms.backend.common.HarvesterCreateRequest;
import com.hsms.backend.common.HarvesterDto;
import com.hsms.backend.common.MissionDto;
import com.hsms.backend.common.MissionStatus;
import com.hsms.backend.common.ResourceStatus;
import com.hsms.backend.common.RoleCode;
import com.hsms.backend.common.TelemetryEventDto;
import com.hsms.backend.common.TelemetryRequest;
import com.hsms.backend.common.TelemetryResponse;
import com.hsms.backend.harvester.api.HarvesterApi;
import com.hsms.backend.harvester.model.Crew;
import com.hsms.backend.harvester.model.CrewMember;
import com.hsms.backend.harvester.model.CrewMemberId;
import com.hsms.backend.harvester.model.Harvester;
import com.hsms.backend.harvester.model.TelemetryEvent;
import com.hsms.backend.harvester.repository.CrewMemberRepository;
import com.hsms.backend.harvester.repository.CrewRepository;
import com.hsms.backend.harvester.repository.HarvesterRepository;
import com.hsms.backend.harvester.repository.TelemetryEventRepository;
import com.hsms.backend.mission.model.Mission;
import com.hsms.backend.readmodel.HsmsDtoAssembler;
import com.hsms.backend.risk.api.RiskApi;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.hsms.backend.common.HsmsOps.*;

@Service
@Transactional
public class HarvesterService implements HarvesterApi {

    private final HsmsAccessService access;
    private final HsmsAuditService audit;
    private final HsmsDtoAssembler dto;
    private final HarvesterRepository harvesterRepository;
    private final CrewRepository crewRepository;
    private final TelemetryEventRepository telemetryEventRepository;
    private final RiskApi riskApi;
    private final Counter duplicateTelemetry;
    private final Counter receivedTelemetry;
    private final TelemetryService telemetryService;
    private final CrewMemberRepository crewMemberRepository;
    private final HsmsUserRepository userRepository;

    public HarvesterService(
            HsmsAccessService access,
            HsmsAuditService audit,
            HsmsDtoAssembler dto,
            HarvesterRepository harvesterRepository,
            CrewRepository crewRepository,
            TelemetryEventRepository telemetryEventRepository,
            RiskApi riskApi,
            MeterRegistry meterRegistry,
            TelemetryService telemetryService,
            CrewMemberRepository crewMemberRepository,
            HsmsUserRepository userRepository) {
        this.access = access;
        this.audit = audit;
        this.dto = dto;
        this.harvesterRepository = harvesterRepository;
        this.crewRepository = crewRepository;
        this.telemetryEventRepository = telemetryEventRepository;
        this.riskApi = riskApi;
        this.duplicateTelemetry = meterRegistry.counter("hsms_telemetry_duplicate_total");
        this.receivedTelemetry = meterRegistry.counter("hsms_telemetry_received_total");
        this.telemetryService = telemetryService;
        this.crewMemberRepository = crewMemberRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HarvesterDto> harvesters() {
        return dto.harvesters();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HarvesterDto> freeHarvesters() {
        return dto.freeHarvesters();
    }

    @Override
    @Transactional(readOnly = true)
    public HarvesterDto harvester(long harvesterId) {
        return dto.requireHarvester(harvesterId);
    }

    @Override
    public HarvesterDto createHarvester(String actorLogin, HarvesterCreateRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_SUPPLY_MANAGER, RoleCode.ROLE_ADMINISTRATOR);
        if (request == null || !hasText(request.name()) || !hasText(request.type())) {
            throw badRequest("Не указаны параметры харвестера", "Заполните название и тип харвестера.");
        }
        double noiseLevel = request.noiseLevel() == null ? 0.3 : request.noiseLevel();
        int capacity = request.capacity() == null ? 1000 : request.capacity();
        if (noiseLevel < 0 || noiseLevel > 1 || capacity <= 0) {
            throw badRequest("Параметры харвестера заполнены неверно", "Укажите noiseLevel в диапазоне 0–1 и положительную capacity.");
        }
        Harvester harvester = new Harvester();
        harvester.setName(request.name().trim());
        harvester.setType(request.type().trim());
        harvester.setStatus(request.status() == null ? ResourceStatus.READY : request.status());
        harvester.setNoiseLevel(noiseLevel);
        harvester.setCapacity(capacity);
        Harvester saved = harvesterRepository.saveAndFlush(harvester);
        audit.record(actor, "HARVESTER_CREATED", "harvester", saved.getId(), null, Map.of(
                "name", saved.getName(),
                "status", saved.getStatus().name()
        ));
        return dto.requireHarvester(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CrewDto> crews() {
        return dto.crews();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CrewDto> freeCrews() {
        return dto.freeCrews();
    }

    @Override
    @Transactional(readOnly = true)
    public CrewDto crew(long crewId) {
        return dto.requireCrew(crewId);
    }

    @Override
    public CrewDto crewByUser(Long userId) {
        return crewsByUser(userId).stream()
                .findFirst()
                .orElseThrow(() -> notFound("Экипаж пользователя не найден", "Проверьте связь пользователя с экипажем."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CrewDto> crewsByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return crewMemberRepository.findAllByIdClient(userId).stream()
                .map(CrewMember::getId)
                .map(id -> crewRepository.findById(id.getCrew())
                        .orElseThrow(() -> notFound("Экипаж пользователя не найден", "Проверьте связь пользователя с экипажем.")))
                .map(crew -> new CrewDto(
                        crew.getId(),
                        crew.getName(),
                        crew.getStatus(),
                        crew.getContactChannel(),
                        crew.getMemberCount(),
                        crew.getAssignedLogin()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCrewAssignedToUser(Long userId, Long crewId) {
        return userId != null && crewId != null && crewMemberRepository.existsByIdClientAndIdCrew(userId, crewId);
    }

    @Override
    public CrewDto createCrew(String actorLogin, CrewCreateRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_SUPPLY_MANAGER, RoleCode.ROLE_ADMINISTRATOR);
        if (request == null || !hasText(request.name()) || !hasText(request.contactChannel())) {
            throw badRequest("Не указаны параметры экипажа", "Заполните название и канал связи экипажа.");
        }
        int memberCount = request.memberCount() == null ? 4 : request.memberCount();
        if (memberCount <= 0) {
            throw badRequest("Численность экипажа заполнена неверно", "Укажите положительное число участников.");
        }
        Crew crew = new Crew();
        crew.setName(request.name().trim());
        crew.setStatus(request.status() == null ? ResourceStatus.READY : request.status());
        crew.setContactChannel(request.contactChannel().trim());
        crew.setMemberCount(memberCount);
        String assignedLogin = blankToDefault(request.assignedLogin(), "crew");
        crew.setAssignedLogin(assignedLogin);
        Crew saved = crewRepository.saveAndFlush(crew);
        linkExistingCrewUser(assignedLogin, saved);
        audit.record(actor, "CREW_CREATED", "crew", saved.getId(), null, Map.of(
                "name", saved.getName(),
                "assignedLogin", saved.getAssignedLogin(),
                "status", saved.getStatus().name()
        ));
        return dto.requireCrew(saved.getId());
    }

    private void linkExistingCrewUser(String assignedLogin, Crew crew) {
        userRepository.findByLogin(assignedLogin)
                .filter(user -> access.roles(user).contains(RoleCode.ROLE_HARVESTER_CREW))
                .ifPresent(user -> {
                    if (crewMemberRepository.existsByIdClientAndIdCrew(user.getId(), crew.getId())) {
                        return;
                    }
                    CrewMemberId id = new CrewMemberId();
                    id.setClient(user.getId());
                    id.setCrew(crew.getId());
                    CrewMember member = new CrewMember();
                    member.setId(id);
                    crewMemberRepository.save(member);
                });
    }

    @Override
    public TelemetryResponse submitTelemetry(String actorLogin, long missionId, TelemetryRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_HARVESTER_CREW, RoleCode.ROLE_ADMINISTRATOR);
        MissionDto mission = dto.mission(missionId);
        if (mission.status() != MissionStatus.ACTIVE) {
            throw badRequest("Телеметрию можно передавать только по активному рейсу", "Проверьте идентификатор и статус рейса.");
        }
        if (!access.roles(actor).contains(RoleCode.ROLE_ADMINISTRATOR)) {
            if (!isCrewAssignedToUser(actor.getId(), mission.crewId())) {
                throw forbidden("Экипаж не назначен на этот рейс", "Передавайте телеметрию только от пользователя, связанного с экипажем рейса.");
            }
        }
        if (request == null || !hasText(request.externalEventId())) {
            throw badRequest("Не указан идентификатор пакета телеметрии", "Передайте externalEventId для идемпотентной обработки.");
        }
        telemetryService.validateCoordinates(request.lat(), request.lon());
        String externalEventId = request.externalEventId().trim();
        var existing = dto.telemetryByExternalId(missionId, externalEventId);
        if (existing.isPresent()) {
            duplicateTelemetry.increment();
            return new TelemetryResponse(existing.get().id(), FreshnessStatus.DUPLICATE, false, existing.get());
        }

        Instant now = Instant.now();
        TelemetryEventDto last = dto.latestAcceptedTelemetry(missionId).orElse(null);
        Instant eventTime = request.eventTime() == null ? now : request.eventTime();
        telemetryService.validateEventTime(eventTime, now);
        FreshnessStatus freshness = (last != null && eventTime.isBefore(last.eventTime())) || telemetryService.isDelayed(eventTime, now)
                ? FreshnessStatus.STALE
                : FreshnessStatus.ACCEPTED;

        TelemetryEvent event = new TelemetryEvent();
        event.setExternalEventId(externalEventId);
        event.setMissionId(missionId);
        event.setCrewId(mission.crewId());
        event.setLat(request.lat());
        event.setLon(request.lon());
        event.setEquipmentStatus(telemetryService.normalizeEquipmentStatus(request.equipmentStatus()));
        event.setEventTime(eventTime);
        event.setReceivedAt(now);
        event.setProcessedAt(now);
        event.setFreshnessStatus(freshness);
        TelemetryEvent saved = telemetryEventRepository.saveAndFlush(event);
        receivedTelemetry.increment();

        boolean riskMarkedStale = false;
        TelemetryEventDto savedDto = dto.telemetryByExternalId(missionId, externalEventId).orElseThrow();
        if (freshness == FreshnessStatus.STALE) {
            riskMarkedStale = invalidateRiskForTelemetry(actorLogin, actor, missionId, now, "Пакет телеметрии устарел или пришел не по порядку", 70,
                    "Требуется ручная проверка: телеметрия устарела или пришла не по порядку.");
        } else if (shouldInvalidateRisk(last, savedDto)) {
            int priority = monitoringPriority(savedDto.equipmentStatus());
            riskMarkedStale = invalidateRiskForTelemetry(actorLogin, actor, missionId, now, "Факторы телеметрии изменились", priority,
                    "Телеметрия указывает на ухудшение оборудования: " + savedDto.equipmentStatus());
        }
        audit.record(actor, "TELEMETRY_RECEIVED", "telemetry_event", saved.getId(), missionId, Map.of(
                "freshness", freshness.name(),
                "riskMarkedStale", riskMarkedStale
        ));
        return new TelemetryResponse(saved.getId(), freshness, riskMarkedStale, savedDto);
    }

    private boolean invalidateRiskForTelemetry(String actorLogin, HsmsUser actor, long missionId, Instant now, String reason, int priority, String context) {
        riskApi.markRiskStaleAfterDomainChange(actorLogin, missionId, reason);
        Mission missionEntity = dto.missionEntity(missionId);
        missionEntity.setRiskReviewRequiredAt(now);
        missionEntity.setRiskReviewReason(reason);
        if (priority > missionEntity.getMonitoringPriority()) {
            missionEntity.setMonitoringPriority(priority);
            missionEntity.setMonitoringContext(context);
            audit.record(actor, "MISSION_MONITORING_PRIORITY_RAISED", "mission", missionId, missionId, Map.of(
                    "priority", priority,
                    "context", context
            ));
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TelemetryEventDto> telemetry(long missionId) {
        return dto.telemetry(missionId);
    }

    @Override
    @Transactional(readOnly = true)
    public TelemetryEventDto latestTelemetry(long missionId) {
        return dto.latestAcceptedTelemetry(missionId)
                .orElseThrow(() -> notFound("Телеметрия по рейсу не найдена", "Передайте первый пакет телеметрии."));
    }

    private boolean shouldInvalidateRisk(TelemetryEventDto previous, TelemetryEventDto current) {
        if (previous == null) {
            return true;
        }
        return !previous.equipmentStatus().equals(current.equipmentStatus())
                || Math.abs(previous.lat() - current.lat()) > 0.05
                || Math.abs(previous.lon() - current.lon()) > 0.05;
    }

    private int monitoringPriority(String equipmentStatus) {
        String normalized = blankToDefault(equipmentStatus, "NORMAL").toUpperCase(Locale.ROOT);
        if (normalized.contains("CRITICAL") || normalized.contains("DAMAGED") || normalized.contains("ПОВРЕЖ")) {
            return 90;
        }
        if (normalized.contains("WARN") || normalized.contains("DEGRADED")) {
            return 60;
        }
        return 0;
    }
}
