package com.hsms.backend.readmodel;

import com.hsms.backend.common.HsmsDomain.AuditEventDto;
import com.hsms.backend.common.HsmsDomain.CrewDto;
import com.hsms.backend.common.HsmsDomain.DashboardDto;
import com.hsms.backend.common.HsmsDomain.EvacuationCommandDto;
import com.hsms.backend.common.HsmsDomain.FreshnessStatus;
import com.hsms.backend.common.HsmsDomain.HarvesterDto;
import com.hsms.backend.common.HsmsDomain.IncidentDto;
import com.hsms.backend.common.HsmsDomain.IncidentStatus;
import com.hsms.backend.common.HsmsDomain.InsuranceCaseDto;
import com.hsms.backend.common.HsmsDomain.InsuranceHistoryEvent;
import com.hsms.backend.common.HsmsDomain.InsuranceRecalculationDto;
import com.hsms.backend.common.HsmsDomain.InsuranceStatus;
import com.hsms.backend.common.HsmsDomain.MiningZoneDto;
import com.hsms.backend.common.HsmsDomain.MissionDto;
import com.hsms.backend.common.HsmsDomain.MissionPlanDto;
import com.hsms.backend.common.HsmsDomain.MissionReportDto;
import com.hsms.backend.common.HsmsDomain.MissionStatus;
import com.hsms.backend.common.HsmsDomain.MissionTimelineDto;
import com.hsms.backend.common.HsmsDomain.ResourceStatus;
import com.hsms.backend.common.HsmsDomain.RiskPolicyDto;
import com.hsms.backend.common.HsmsDomain.RiskSnapshotDto;
import com.hsms.backend.common.HsmsDomain.RoutePointDto;
import com.hsms.backend.common.HsmsDomain.Severity;
import com.hsms.backend.common.HsmsDomain.TelemetryEventDto;
import com.hsms.backend.common.model.AuditDetail;
import com.hsms.backend.common.model.AuditEvent;
import com.hsms.backend.common.model.MiningZone;
import com.hsms.backend.common.repository.AuditEventRepository;
import com.hsms.backend.common.repository.MiningZoneRepository;
import com.hsms.backend.harvester.model.Crew;
import com.hsms.backend.harvester.model.Harvester;
import com.hsms.backend.harvester.model.MissionReport;
import com.hsms.backend.harvester.model.TelemetryEvent;
import com.hsms.backend.harvester.repository.CrewRepository;
import com.hsms.backend.harvester.repository.HarvesterRepository;
import com.hsms.backend.harvester.repository.MissionReportRepository;
import com.hsms.backend.harvester.repository.TelemetryEventRepository;
import com.hsms.backend.insurance.model.InsuranceCase;
import com.hsms.backend.insurance.model.InsuranceRecalculation;
import com.hsms.backend.insurance.repository.InsuranceCaseRepository;
import com.hsms.backend.insurance.repository.InsuranceRecalculationRepository;
import com.hsms.backend.mission.model.Mission;
import com.hsms.backend.mission.model.MissionPlan;
import com.hsms.backend.mission.model.MissionRoute;
import com.hsms.backend.mission.repository.MissionPlanRepository;
import com.hsms.backend.mission.repository.MissionRepository;
import com.hsms.backend.risk.model.RiskPolicy;
import com.hsms.backend.risk.model.RiskScore;
import com.hsms.backend.risk.repository.RiskPolicyRepository;
import com.hsms.backend.risk.repository.RiskScoreRepository;
import com.hsms.backend.security.model.EvacuationCommand;
import com.hsms.backend.security.model.Incident;
import com.hsms.backend.security.repository.AlarmSignalRepository;
import com.hsms.backend.security.repository.EvacuationCommandRepository;
import com.hsms.backend.security.repository.IncidentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hsms.backend.common.HsmsOps.csv;
import static com.hsms.backend.common.HsmsOps.notFound;
import static com.hsms.backend.common.HsmsOps.round;

@Service
public class HsmsDtoAssembler {

    private static final List<MissionStatus> ACTIVE_RESOURCE_STATUSES = List.of(
            MissionStatus.DRAFT,
            MissionStatus.READY_FOR_RISK,
            MissionStatus.RISK_ASSESSED,
            MissionStatus.ACTIVE,
            MissionStatus.COMPLETED_PENDING_CLOSE
    );

    private final MiningZoneRepository zoneRepository;
    private final HarvesterRepository harvesterRepository;
    private final CrewRepository crewRepository;
    private final MissionRepository missionRepository;
    private final MissionPlanRepository missionPlanRepository;
    private final MissionReportRepository missionReportRepository;
    private final RiskPolicyRepository riskPolicyRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final TelemetryEventRepository telemetryEventRepository;
    private final IncidentRepository incidentRepository;
    private final AlarmSignalRepository alarmSignalRepository;
    private final EvacuationCommandRepository evacuationCommandRepository;
    private final InsuranceCaseRepository insuranceCaseRepository;
    private final InsuranceRecalculationRepository insuranceRecalculationRepository;
    private final AuditEventRepository auditEventRepository;

    public HsmsDtoAssembler(
            MiningZoneRepository zoneRepository,
            HarvesterRepository harvesterRepository,
            CrewRepository crewRepository,
            MissionRepository missionRepository,
            MissionPlanRepository missionPlanRepository,
            MissionReportRepository missionReportRepository,
            RiskPolicyRepository riskPolicyRepository,
            RiskScoreRepository riskScoreRepository,
            TelemetryEventRepository telemetryEventRepository,
            IncidentRepository incidentRepository,
            AlarmSignalRepository alarmSignalRepository,
            EvacuationCommandRepository evacuationCommandRepository,
            InsuranceCaseRepository insuranceCaseRepository,
            InsuranceRecalculationRepository insuranceRecalculationRepository,
            AuditEventRepository auditEventRepository
    ) {
        this.zoneRepository = zoneRepository;
        this.harvesterRepository = harvesterRepository;
        this.crewRepository = crewRepository;
        this.missionRepository = missionRepository;
        this.missionPlanRepository = missionPlanRepository;
        this.missionReportRepository = missionReportRepository;
        this.riskPolicyRepository = riskPolicyRepository;
        this.riskScoreRepository = riskScoreRepository;
        this.telemetryEventRepository = telemetryEventRepository;
        this.incidentRepository = incidentRepository;
        this.alarmSignalRepository = alarmSignalRepository;
        this.evacuationCommandRepository = evacuationCommandRepository;
        this.insuranceCaseRepository = insuranceCaseRepository;
        this.insuranceRecalculationRepository = insuranceRecalculationRepository;
        this.auditEventRepository = auditEventRepository;
    }

    public List<MiningZoneDto> zones() {
        return zoneRepository.findAllByOrderByIdAsc().stream().map(this::zone).toList();
    }

    public MiningZoneDto requireZone(long id) {
        MiningZone zone = zoneEntity(id);
        if (!zone.isActive()) {
            throw notFound("Зона добычи не найдена", "Выберите активную зону добычи.");
        }
        return zone(zone);
    }

    public MiningZone zoneEntity(long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> notFound("Зона добычи не найдена", "Выберите активную зону добычи."));
    }

    public List<HarvesterDto> harvesters() {
        return harvesterRepository.findAllByOrderByIdAsc().stream().map(this::harvester).toList();
    }

    public List<HarvesterDto> freeHarvesters() {
        return harvesterRepository.findByStatusOrderByIdAsc(ResourceStatus.READY)
                .stream()
                .filter(harvester -> missionRepository.countActiveResourceAssignments(ACTIVE_RESOURCE_STATUSES, harvester.getId(), -1L, null) == 0)
                .map(this::harvester)
                .toList();
    }

    public HarvesterDto requireHarvester(long id) {
        return harvester(harvesterEntity(id));
    }

    public Harvester harvesterEntity(long id) {
        return harvesterRepository.findById(id)
                .orElseThrow(() -> notFound("Харвестер не найден", "Выберите харвестер из справочника."));
    }

    public List<CrewDto> crews() {
        return crewRepository.findAllByOrderByIdAsc().stream().map(this::crew).toList();
    }

    public List<CrewDto> freeCrews() {
        return crewRepository.findByStatusOrderByIdAsc(ResourceStatus.READY)
                .stream()
                .filter(crew -> missionRepository.countActiveResourceAssignments(ACTIVE_RESOURCE_STATUSES, -1L, crew.getId(), null) == 0)
                .map(this::crew)
                .toList();
    }

    public CrewDto requireCrew(long id) {
        return crew(crewEntity(id));
    }

    public Crew crewEntity(long id) {
        return crewRepository.findById(id)
                .orElseThrow(() -> notFound("Экипаж не найден", "Выберите экипаж из справочника."));
    }

    public List<MissionDto> missions() {
        return missionRepository.findAllByOrderByIdAsc().stream().map(this::mission).toList();
    }

    public MissionDto mission(long id) {
        return mission(missionEntity(id));
    }

    public Mission missionEntity(long id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> notFound("Рейс не найден", "Проверьте идентификатор рейса."));
    }

    public Optional<MissionPlanDto> plan(long missionId) {
        return missionPlanRepository.findByMissionId(missionId).map(this::plan);
    }

    public RiskPolicyDto activeRiskPolicy() {
        return riskPolicyRepository.findFirstByActiveTrueOrderByActiveFromDescIdDesc()
                .map(this::riskPolicy)
                .orElseThrow(() -> notFound("Активная risk policy не найдена", "Проверьте миграции справочников риска."));
    }

    public RiskPolicy activeRiskPolicyEntity() {
        return riskPolicyRepository.findFirstByActiveTrueOrderByActiveFromDescIdDesc()
                .orElseThrow(() -> notFound("Активная risk policy не найдена", "Проверьте миграции справочников риска."));
    }

    public RiskSnapshotDto risk(long id) {
        return risk(riskScoreRepository.findById(id)
                .orElseThrow(() -> notFound("Снимок риска не найден", "Повторите расчет риска.")));
    }

    public Optional<RiskSnapshotDto> latestRisk(long missionId) {
        return riskScoreRepository.findFirstByMissionIdOrderByCalculatedAtDescIdDesc(missionId).map(this::risk);
    }

    public List<TelemetryEventDto> telemetry(long missionId) {
        requireMissionExists(missionId);
        return telemetryEventRepository.findByMissionIdOrderByEventTimeDescIdDesc(missionId).stream().map(this::telemetry).toList();
    }

    public Optional<TelemetryEventDto> latestAcceptedTelemetry(long missionId) {
        return telemetryEventRepository.findFirstByMissionIdAndFreshnessStatusOrderByEventTimeDescIdDesc(missionId, FreshnessStatus.ACCEPTED)
                .map(this::telemetry);
    }

    public Optional<TelemetryEventDto> telemetryByExternalId(long missionId, String externalEventId) {
        return telemetryEventRepository.findByMissionIdAndExternalEventId(missionId, externalEventId).map(this::telemetry);
    }

    public List<IncidentDto> incidents() {
        return incidentRepository.findAllByOrderByIdAsc().stream().map(this::incident).toList();
    }

    public List<IncidentDto> incidentQueue() {
        return incidentRepository.findAll().stream()
                .sorted(Comparator
                        .comparingInt(this::incidentQueueRank)
                        .thenComparing(Incident::getSlaDeadlineAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(incident -> severityRank(incident.getSeverity()))
                        .thenComparing(Incident::getSlaStartedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Incident::getStatus)
                        .thenComparing(Incident::getId))
                .map(this::incident)
                .toList();
    }

    public IncidentDto incident(long id) {
        return incident(incidentEntity(id));
    }

    public Incident incidentEntity(long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> notFound("Инцидент не найден", "Проверьте идентификатор инцидента."));
    }

    public Optional<IncidentDto> openIncidentForMission(long missionId) {
        return incidentRepository.findFirstByMissionIdAndStatusNotOrderBySlaStartedAtAscIdAsc(missionId, IncidentStatus.CLOSED)
                .map(this::incident);
    }

    public Optional<AlarmRecord> alarmByExternalId(long missionId, String externalEventId) {
        return alarmSignalRepository.findByMissionIdAndExternalEventId(missionId, externalEventId)
                .map(alarm -> new AlarmRecord(alarm.getId(), alarm.getIncidentId()));
    }

    public Optional<EvacuationCommandDto> evacuation(long id) {
        return evacuationCommandRepository.findById(id).map(this::evacuation);
    }

    public List<InsuranceCaseDto> insuranceCases() {
        return insuranceCaseRepository.findAllByOrderByIdAsc().stream().map(this::insuranceCase).toList();
    }

    public InsuranceCaseDto insuranceCase(long id) {
        return insuranceCase(insuranceCaseEntity(id));
    }

    public InsuranceCase insuranceCaseEntity(long id) {
        return insuranceCaseRepository.findById(id)
                .orElseThrow(() -> notFound("Страховой кейс не найден", "Проверьте идентификатор страхового кейса."));
    }

    public Optional<InsuranceCaseDto> openInsuranceCase(long missionId, Long incidentId) {
        if (incidentId == null) {
            return insuranceCaseRepository
                    .findFirstByMissionIdAndIncidentIdIsNullAndStatusNotOrderByOpenedAtDescIdDesc(missionId, InsuranceStatus.CLOSED)
                    .map(this::insuranceCase);
        }
        return insuranceCaseRepository
                .findFirstByMissionIdAndIncidentIdAndStatusNotOrderByOpenedAtDescIdDesc(missionId, incidentId, InsuranceStatus.CLOSED)
                .map(this::insuranceCase);
    }

    public List<InsuranceRecalculationDto> insuranceHistory(long caseId) {
        return insuranceRecalculationRepository.findByInsuranceCaseIdOrderByCalculatedAtDescIdDesc(caseId)
                .stream()
                .map(this::insuranceRecalculation)
                .toList();
    }

    public List<AuditEventDto> auditSnapshot() {
        return audit((Long) null);
    }

    public List<AuditEventDto> audit(Long missionId) {
        List<AuditEvent> events = missionId == null
                ? auditEventRepository.findAllByOrderByCreatedAtDescIdDesc()
                : auditEventRepository.findByMissionIdOrderByCreatedAtDescIdDesc(missionId);
        return events.stream().map(this::audit).toList();
    }

    public MissionTimelineDto missionTimeline(long missionId) {
        MissionDto mission = mission(missionId);
        InsuranceCaseDto insurance = mission.insuranceCaseId() == null ? null : insuranceCase(mission.insuranceCaseId());
        return new MissionTimelineDto(
                mission,
                telemetry(missionId),
                mission.incidentIds().stream().map(this::incident).toList(),
                insurance,
                audit(missionId)
        );
    }

    public DashboardDto dashboard(Instant from, Instant to) {
        Instant periodFrom = from == null ? Instant.EPOCH : from;
        Instant periodTo = to == null ? Instant.now() : to;
        List<MissionDto> periodMissions = missions().stream()
                .filter(mission -> !mission.createdAt().isBefore(periodFrom) && !mission.createdAt().isAfter(periodTo))
                .toList();
        List<IncidentDto> periodIncidents = incidents().stream()
                .filter(incident -> !incident.slaStartedAt().isBefore(periodFrom) && !incident.slaStartedAt().isAfter(periodTo))
                .toList();
        long active = periodMissions.stream().filter(mission -> mission.status() == MissionStatus.ACTIVE).count();
        long closed = periodMissions.stream().filter(mission -> mission.status() == MissionStatus.CLOSED || mission.status() == MissionStatus.LOST).count();
        long cancelled = periodMissions.stream().filter(mission -> mission.status() == MissionStatus.RISK_CANCELLED || mission.status() == MissionStatus.CANCELLED).count();
        long openIncidents = periodIncidents.stream().filter(incident -> incident.status() != IncidentStatus.CLOSED).count();
        long slaBreaches = periodIncidents.stream().filter(IncidentDto::slaBreached).count();
        long totalIncidents = periodIncidents.size();
        double compliance = totalIncidents == 0 ? 100.0 : (double) (totalIncidents - slaBreaches) / totalIncidents * 100.0;
        double avgReaction = periodIncidents.stream()
                .filter(incident -> incident.closedAt() != null)
                .mapToLong(incident -> Duration.between(incident.slaStartedAt(), incident.closedAt()).toSeconds())
                .average()
                .orElse(0.0);
        long openInsurance = insuranceCases().stream().filter(insurance -> insurance.status() != InsuranceStatus.CLOSED).count();
        BigDecimal avgPremium = averagePremium();
        Map<Severity, Long> bySeverity = new LinkedHashMap<>();
        for (Severity severity : Severity.values()) {
            bySeverity.put(severity, periodIncidents.stream().filter(incident -> incident.severity() == severity).count());
        }
        return new DashboardDto(active, closed, cancelled, openIncidents, slaBreaches, round(compliance),
                round(avgReaction), openInsurance, avgPremium, bySeverity, periodFrom, periodTo);
    }

    public String missionReportCsv() {
        StringBuilder builder = new StringBuilder("id,title,status,zone,harvester,crew,createdAt,updatedAt,plannedStart,plannedEnd,actualStart,reportActualStart,reportActualEnd,reportSubmittedAt,closedAt,closedBy,riskScore,pAttack,relatedIncidentIds,insuranceCaseId\n");
        missions().forEach(mission -> {
            MissionReportDto report = mission.report();
            builder.append(mission.id()).append(',')
                    .append(csv(mission.title())).append(',')
                    .append(mission.status()).append(',')
                    .append(csv(mission.zoneName())).append(',')
                    .append(csv(mission.harvesterName())).append(',')
                    .append(csv(mission.crewName())).append(',')
                    .append(mission.createdAt() == null ? "" : mission.createdAt()).append(',')
                    .append(mission.updatedAt() == null ? "" : mission.updatedAt()).append(',')
                    .append(mission.plannedStart() == null ? "" : mission.plannedStart()).append(',')
                    .append(mission.plannedEnd() == null ? "" : mission.plannedEnd()).append(',')
                    .append(mission.actualStart() == null ? "" : mission.actualStart()).append(',')
                    .append(report == null || report.actualStart() == null ? "" : report.actualStart()).append(',')
                    .append(report == null || report.actualEnd() == null ? "" : report.actualEnd()).append(',')
                    .append(report == null || report.submittedAt() == null ? "" : report.submittedAt()).append(',')
                    .append(mission.closedAt() == null ? "" : mission.closedAt()).append(',')
                    .append(csv(mission.closedBy())).append(',')
                    .append(mission.risk() == null ? "" : mission.risk().riskScore()).append(',')
                    .append(mission.risk() == null ? "" : mission.risk().pAttack()).append(',')
                    .append(csv(String.join("|", mission.incidentIds().stream().map(String::valueOf).toList()))).append(',')
                    .append(mission.insuranceCaseId() == null ? "" : mission.insuranceCaseId())
                    .append('\n');
        });
        return builder.toString();
    }

    public String incidentReportCsv() {
        StringBuilder builder = new StringBuilder("id,missionId,status,severity,slaStartedAt,slaDeadlineAt,slaBreached,closedAt,decision\n");
        incidents().stream()
                .sorted(Comparator.comparingLong(IncidentDto::id))
                .forEach(incident -> builder.append(incident.id()).append(',')
                        .append(incident.missionId()).append(',')
                        .append(incident.status()).append(',')
                        .append(incident.severity()).append(',')
                        .append(incident.slaStartedAt()).append(',')
                        .append(incident.slaDeadlineAt()).append(',')
                        .append(incident.slaBreached()).append(',')
                        .append(incident.closedAt() == null ? "" : incident.closedAt()).append(',')
                        .append(csv(incident.classificationReason()))
                        .append('\n'));
        return builder.toString();
    }

    public void requireMissionExists(long missionId) {
        if (!missionRepository.existsById(missionId)) {
            throw notFound("Рейс не найден", "Проверьте идентификатор рейса.");
        }
    }

    private MissionDto mission(Mission mission) {
        RiskSnapshotDto risk = latestRisk(mission.getId())
                .orElseGet(() -> mission.getRiskSnapshotId() == null ? null : risk(mission.getRiskSnapshotId()));
        Long insuranceCaseId = mission.getInsuranceCaseId() != null
                ? mission.getInsuranceCaseId()
                : insuranceCaseRepository.findFirstByMissionIdOrderByOpenedAtDescIdDesc(mission.getId())
                .map(InsuranceCase::getId)
                .orElse(null);
        return new MissionDto(
                mission.getId(),
                mission.getTitle(),
                mission.getStatus(),
                mission.getZone() == null ? null : mission.getZone().getId(),
                mission.getZone() == null ? null : mission.getZone().getName(),
                mission.getHarvester() == null ? null : mission.getHarvester().getId(),
                mission.getHarvester() == null ? null : mission.getHarvester().getName(),
                mission.getCrew() == null ? null : mission.getCrew().getId(),
                mission.getCrew() == null ? null : mission.getCrew().getName(),
                mission.getPlannedStart(),
                mission.getPlannedEnd(),
                mission.getActualStart(),
                mission.getClosedAt(),
                mission.getCloseReason(),
                mission.getRouteVersion(),
                mission.getRoute().stream().map(this::route).toList(),
                risk,
                plan(mission.getId()).orElse(null),
                mission.getReportId() == null ? null : missionReportRepository.findById(mission.getReportId()).map(this::report).orElse(null),
                incidentRepository.findByMissionIdOrderByIdAsc(mission.getId()).stream().map(Incident::getId).toList(),
                insuranceCaseId,
                mission.getClosedBy(),
                mission.getDraftMissingFields(),
                mission.getMonitoringPriority(),
                mission.getMonitoringContext(),
                mission.getRiskReviewRequiredAt(),
                mission.getRiskReviewReason(),
                mission.getCreatedAt(),
                mission.getUpdatedAt()
        );
    }

    private MiningZoneDto zone(MiningZone zone) {
        return new MiningZoneDto(zone.getId(), zone.getName(), zone.getRiskLevel(), zone.getCoordinates(), zone.isActive());
    }

    private HarvesterDto harvester(Harvester harvester) {
        return new HarvesterDto(harvester.getId(), harvester.getName(), harvester.getType(), harvester.getStatus(), harvester.getNoiseLevel(), harvester.getCapacity());
    }

    private CrewDto crew(Crew crew) {
        return new CrewDto(crew.getId(), crew.getName(), crew.getStatus(), crew.getContactChannel(), crew.getMemberCount(), crew.getAssignedLogin());
    }

    private RoutePointDto route(MissionRoute route) {
        return new RoutePointDto(route.getSeqNo(), route.getLat(), route.getLon());
    }

    private MissionPlanDto plan(MissionPlan plan) {
        return new MissionPlanDto(plan.getId(), plan.getMissionId(), plan.getRouteVersion(),
                plan.getSafetyContact(), plan.getPublishedAt(), plan.getAcknowledgedAt(), plan.getAcknowledgedBy());
    }

    private RiskPolicyDto riskPolicy(RiskPolicy policy) {
        return new RiskPolicyDto(policy.getId(), policy.getVersion(), policy.getWarningThreshold(),
                policy.getBlockThreshold(), policy.getFormulaDescription(), policy.getActiveFrom());
    }

    private RiskSnapshotDto risk(RiskScore risk) {
        return new RiskSnapshotDto(
                risk.getId(),
                risk.getMissionId(),
                risk.getPolicyVersion(),
                risk.getPAttack(),
                risk.getRiskScore(),
                risk.isLaunchAllowed(),
                risk.getDecisionZone(),
                risk.getBlockingReason(),
                risk.getFactors().stream().collect(LinkedHashMap::new, (map, factor) -> map.put(factor.getFactorName(), factor.getFactorValue()), Map::putAll),
                risk.getDataQuality(),
                risk.getCalculatedAt(),
                risk.getValidForRouteVersion(),
                risk.isStale(),
                risk.getStaleReason()
        );
    }

    private TelemetryEventDto telemetry(TelemetryEvent event) {
        return new TelemetryEventDto(
                event.getId(),
                event.getExternalEventId(),
                event.getMissionId(),
                event.getCrewId(),
                event.getLat(),
                event.getLon(),
                event.getEquipmentStatus(),
                event.getEventTime(),
                event.getReceivedAt(),
                event.getProcessedAt(),
                event.getFreshnessStatus()
        );
    }

    private IncidentDto incident(Incident incident) {
        return new IncidentDto(
                incident.getId(),
                incident.getMissionId(),
                incident.getAlarmSignalId(),
                incident.getStatus(),
                incident.getSeverity(),
                incident.getClassificationReason(),
                incident.getSlaStartedAt(),
                incident.getSlaDeadlineAt(),
                incident.isSlaBreached(),
                incident.getClosedAt(),
                incident.getClosedBy(),
                incident.getEvacuationCommandId() == null ? null : evacuation(incident.getEvacuationCommandId()).orElse(null)
        );
    }

    private EvacuationCommandDto evacuation(EvacuationCommand command) {
        return new EvacuationCommandDto(
                command.getId(),
                command.getIncidentId(),
                command.getMissionId(),
                command.getStatus(),
                command.getSentAt(),
                command.getDeliveredAt(),
                command.getSentBy(),
                command.getAcknowledgedAt(),
                command.getAcknowledgedBy(),
                command.getExpiresAt(),
                command.getDeliveryError()
        );
    }

    private MissionReportDto report(MissionReport report) {
        return new MissionReportDto(
                report.getId(),
                report.getMissionId(),
                report.getActualStart(),
                report.getActualEnd(),
                report.getSpiceAmount(),
                report.getHarvesterFinalStatus(),
                report.getAbnormalSituations(),
                report.getSubmittedBy(),
                report.getSubmittedAt()
        );
    }

    private InsuranceCaseDto insuranceCase(InsuranceCase insuranceCase) {
        return new InsuranceCaseDto(
                insuranceCase.getId(),
                insuranceCase.getMissionId(),
                insuranceCase.getIncidentId(),
                insuranceCase.getStatus(),
                insuranceCase.getTriggerType(),
                insuranceCase.getOpenedAt(),
                insuranceCase.getOpenedBy(),
                insuranceCase.getTriggerReason(),
                insuranceCase.getTriggerPAttack(),
                insuranceCase.getTriggerRiskScore(),
                insuranceCase.getTriggerRiskSnapshotId(),
                insuranceCase.getTriggerDecisionAt(),
                insuranceCase.getTriggerDecisionBy(),
                parseSeverity(insuranceCase.getIncidentSeverity()),
                insuranceCase.getIncidentRegisteredAt(),
                insuranceCase.getIncidentClosedAt(),
                insuranceCase.getIncidentSlaBreached(),
                insuranceCase.getIncidentOperator(),
                insuranceCase.getMissingData(),
                insuranceCase.getFinalRiskScore(),
                insuranceCase.getFinalPremium(),
                insuranceCase.getClosedAt(),
                insuranceCase.getClosedBy(),
                insuranceHistory(insuranceCase.getId())
        );
    }

    private InsuranceRecalculationDto insuranceRecalculation(InsuranceRecalculation recalculation) {
        return new InsuranceRecalculationDto(
                recalculation.getId(),
                recalculation.getInsuranceCase().getId(),
                recalculation.getEventType() == null ? InsuranceHistoryEvent.RECALCULATION : recalculation.getEventType(),
                recalculation.getRiskSnapshotId(),
                recalculation.getOldPremium(),
                recalculation.getNewPremium(),
                recalculation.getOldRiskScore(),
                recalculation.getNewRiskScore(),
                recalculation.getReason(),
                recalculation.getCalculatedAt(),
                recalculation.getCalculatedBy(),
                recalculation.getRejectedReason()
        );
    }

    private AuditEventDto audit(AuditEvent event) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (AuditDetail detail : event.getDetails()) {
            details.put(detail.getDetailKey(), detail.getDetailValue());
        }
        return new AuditEventDto(
                event.getId(),
                event.getActorLogin(),
                event.getActorRole(),
                event.getAction(),
                event.getObjectType(),
                event.getObjectId(),
                event.getMissionId(),
                event.getCreatedAt(),
                Map.copyOf(details)
        );
    }

    private BigDecimal averagePremium() {
        List<BigDecimal> premiums = insuranceCases().stream()
                .map(InsuranceCaseDto::finalPremium)
                .filter(premium -> premium != null)
                .toList();
        if (premiums.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = premiums.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(premiums.size()), 2, RoundingMode.HALF_UP);
    }

    private int severityRank(Severity severity) {
        return switch (severity) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    private int incidentQueueRank(Incident incident) {
        if (incident.getStatus() == IncidentStatus.CLOSED
                || incident.getStatus() == IncidentStatus.MONITORING
                || incident.getStatus() == IncidentStatus.EVACUATION_ACKNOWLEDGED) {
            return 2;
        }
        return incident.isSlaBreached() ? 0 : 1;
    }

    private Severity parseSeverity(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Severity.valueOf(value);
    }

    public record AlarmRecord(long alarmId, long incidentId) {
    }
}
