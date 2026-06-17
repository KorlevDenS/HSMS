package com.hsms.backend.mission.service;

import com.hsms.backend.auth.model.HsmsUser;
import com.hsms.backend.common.HsmsAccessService;
import com.hsms.backend.common.HsmsAuditService;
import com.hsms.backend.common.CrewDto;
import com.hsms.backend.common.DecisionZone;
import com.hsms.backend.common.HarvesterDto;
import com.hsms.backend.common.IncidentStatus;
import com.hsms.backend.common.LaunchRequest;
import com.hsms.backend.common.MissionCloseRequest;
import com.hsms.backend.common.MissionCreateRequest;
import com.hsms.backend.common.MissionDto;
import com.hsms.backend.common.MissionPatchRequest;
import com.hsms.backend.common.MissionPlanDto;
import com.hsms.backend.common.MissionReportDto;
import com.hsms.backend.common.MissionReportRequest;
import com.hsms.backend.common.MissionStatus;
import com.hsms.backend.common.MissionTimelineDto;
import com.hsms.backend.common.ResourceStatus;
import com.hsms.backend.common.RiskCancelRequest;
import com.hsms.backend.common.RiskSnapshotDto;
import com.hsms.backend.common.RoleCode;
import com.hsms.backend.common.RoutePointDto;
import com.hsms.backend.common.HsmsException;
import com.hsms.backend.harvester.api.HarvesterApi;
import com.hsms.backend.harvester.model.MissionReport;
import com.hsms.backend.harvester.repository.MissionReportRepository;
import com.hsms.backend.mission.api.MissionApi;
import com.hsms.backend.mission.model.Mission;
import com.hsms.backend.mission.model.MissionPlan;
import com.hsms.backend.mission.model.MissionRoute;
import com.hsms.backend.mission.repository.MissionPlanRepository;
import com.hsms.backend.mission.repository.MissionRepository;
import com.hsms.backend.readmodel.HsmsDtoAssembler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import static com.hsms.backend.common.HsmsOps.*;

@Service
@Transactional
public class MissionService implements MissionApi {

    private static final List<MissionStatus> ACTIVE_RESOURCE_STATUSES = List.of(
            MissionStatus.DRAFT,
            MissionStatus.READY_FOR_RISK,
            MissionStatus.RISK_ASSESSED,
            MissionStatus.ACTIVE,
            MissionStatus.COMPLETED_PENDING_CLOSE
    );

    private final HsmsAccessService access;
    private final HsmsAuditService audit;
    private final HsmsDtoAssembler dto;
    private final MissionRepository missionRepository;
    private final MissionPlanRepository missionPlanRepository;
    private final MissionReportRepository missionReportRepository;
    private final Counter blockedLaunches;
    private final Counter riskCancelledMissions;
    private final HarvesterApi harvesterApi;

    public MissionService(
            HsmsAccessService access,
            HsmsAuditService audit,
            HsmsDtoAssembler dto,
            MissionRepository missionRepository,
            MissionPlanRepository missionPlanRepository,
            MissionReportRepository missionReportRepository,
            MeterRegistry meterRegistry,
            HarvesterApi harvesterApi) {
        this.access = access;
        this.audit = audit;
        this.dto = dto;
        this.missionRepository = missionRepository;
        this.missionPlanRepository = missionPlanRepository;
        this.missionReportRepository = missionReportRepository;
        this.blockedLaunches = meterRegistry.counter("hsms_risk_blocked_launch_total");
        this.riskCancelledMissions = meterRegistry.counter("hsms_missions_risk_cancelled_total");
        this.harvesterApi = harvesterApi;
    }

    @Override
    public boolean isMissionOfThisCrew(Long missionId, Long userId) {
        Mission mission = missionRepository.findById(missionId).orElseThrow();
        CrewDto crewDto = harvesterApi.crewByUser(userId);
        return mission.getCrew().getId() == crewDto.id();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionDto> missions() {
        return dto.missions();
    }

    @Override
    @Transactional(readOnly = true)
    public MissionDto mission(long missionId) {
        return dto.mission(missionId);
    }

    @Override
    public MissionDto createMission(String actorLogin, MissionCreateRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_SUPPLY_MANAGER, RoleCode.ROLE_ADMINISTRATOR);
        List<String> missingFields = missingMissionFields(request);
        if (missingFields.isEmpty()) {
            requireMissionInput(request);
            ensureResourcesFree(request.harvesterId(), request.crewId(), null);
        }
        Instant now = Instant.now();
        Mission mission = new Mission();
        mission.setTitle(request != null && hasText(request.title()) ? request.title().trim() : "Черновик рейса без названия");
        mission.setStatus(MissionStatus.DRAFT);
        mission.setZone(resolveZone(request == null ? null : request.zoneId()));
        mission.setHarvester(resolveHarvester(request == null ? null : request.harvesterId()));
        mission.setCrew(resolveCrew(request == null ? null : request.crewId()));
        mission.setPlannedStart(request == null ? null : request.plannedStart());
        mission.setPlannedEnd(request == null ? null : request.plannedEnd());
        mission.setRouteVersion(1);
        mission.setDraftMissingFields(missingFields.isEmpty() ? null : String.join(", ", missingFields));
        mission.setCreatedBy(actor.getLogin());
        mission.setCreatedAt(now);
        mission.setUpdatedAt(now);
        replaceRoute(mission, request == null ? List.of() : nullToEmpty(request.route()));
        Mission saved = missionRepository.save(mission);
        audit.record(actor, "MISSION_CREATED", "mission", saved.getId(), saved.getId(), Map.of(
                "status", MissionStatus.DRAFT.name(),
                "missingFields", saved.getDraftMissingFields() == null ? "" : saved.getDraftMissingFields()
        ));
        return dto.mission(saved.getId());
    }

    @Override
    public MissionDto updateMission(String actorLogin, long missionId, MissionPatchRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_SUPPLY_MANAGER, RoleCode.ROLE_ADMINISTRATOR);
        Mission mission = dto.missionEntity(missionId);
        if (mission.getStatus() == MissionStatus.ACTIVE
                || mission.getStatus() == MissionStatus.CLOSED
                || mission.getStatus() == MissionStatus.LOST
                || mission.getStatus() == MissionStatus.CANCELLED
                || mission.getStatus() == MissionStatus.RISK_CANCELLED) {
            throw badRequest("Активный или закрытый рейс нельзя редактировать", "Создайте новый рейс или закройте текущий сценарий.");
        }
        if (request == null) {
            throw badRequest("Не переданы изменения рейса", "Измените хотя бы одно поле рейса.");
        }
        String title = hasText(request.title()) ? request.title().trim() : mission.getTitle();
        Long zoneId = request.zoneId() == null ? id(mission.getZone()) : normalizeId(request.zoneId());
        Long harvesterId = request.harvesterId() == null ? id(mission.getHarvester()) : normalizeId(request.harvesterId());
        Long crewId = request.crewId() == null ? id(mission.getCrew()) : normalizeId(request.crewId());
        Instant plannedStart = request.plannedStart() == null ? mission.getPlannedStart() : request.plannedStart();
        Instant plannedEnd = request.plannedEnd() == null ? mission.getPlannedEnd() : request.plannedEnd();
        List<RoutePointDto> route = request.route() == null || request.route().isEmpty()
                ? mission.getRoute().stream().map(point -> new RoutePointDto(point.getSeqNo(), point.getLat(), point.getLon())).toList()
                : request.route();
        List<String> missingFields = missingMissionFields(title, zoneId, harvesterId, crewId, plannedStart, plannedEnd, route);
        if (missingFields.isEmpty()) {
            requireMissionInput(title, zoneId, harvesterId, crewId, plannedStart, plannedEnd, route);
            ensureResourcesFree(harvesterId, crewId, missionId);
        }

        boolean routeChanged = request.route() != null && !request.route().isEmpty();
        boolean riskRelevantChanged = routeChanged
                || !Objects.equals(zoneId, id(mission.getZone()))
                || !Objects.equals(harvesterId, id(mission.getHarvester()))
                || !Objects.equals(plannedStart, mission.getPlannedStart())
                || !Objects.equals(plannedEnd, mission.getPlannedEnd());
        int routeVersion = routeChanged ? mission.getRouteVersion() + 1 : mission.getRouteVersion();
        Instant now = Instant.now();
        mission.setTitle(title);
        mission.setStatus(missingFields.isEmpty() ? MissionStatus.READY_FOR_RISK : MissionStatus.DRAFT);
        mission.setZone(resolveZone(zoneId));
        mission.setHarvester(resolveHarvester(harvesterId));
        mission.setCrew(resolveCrew(crewId));
        mission.setPlannedStart(plannedStart);
        mission.setPlannedEnd(plannedEnd);
        mission.setRouteVersion(routeVersion);
        mission.setDraftMissingFields(missingFields.isEmpty() ? null : String.join(", ", missingFields));
        if (riskRelevantChanged) {
            mission.setRiskReviewRequiredAt(now);
            mission.setRiskReviewReason(missingFields.isEmpty() ? "Параметры рейса изменены" : "Черновик рейса изменен и требует заполнения обязательных полей");
        }
        mission.setUpdatedAt(now);
        if (routeChanged) {
            replacePersistedRoute(mission, route);
        }
        missionRepository.saveAndFlush(mission);
        audit.record(actor, "MISSION_UPDATED", "mission", missionId, missionId, Map.of(
                "routeVersion", routeVersion,
                "riskRecalculationRequired", riskRelevantChanged && missingFields.isEmpty(),
                "missingFields", mission.getDraftMissingFields() == null ? "" : mission.getDraftMissingFields()
        ));
        return dto.mission(missionId);
    }

    @Override
    public MissionDto launchMission(String actorLogin, long missionId, LaunchRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_SUPPLY_MANAGER, RoleCode.ROLE_ADMINISTRATOR);
        Mission mission = dto.missionEntity(missionId);
        RiskSnapshotDto risk = dto.latestRisk(missionId)
                .orElseThrow(() -> notFound("Снимок риска не найден", "Повторите расчет риска."));
        if (risk.stale() || risk.validForRouteVersion() != mission.getRouteVersion()) {
            throw badRequest("Оценка риска устарела", "Повторите расчет риска для текущего маршрута.");
        }
        if (risk.decisionZone() == DecisionZone.BLOCKING) {
            blockedLaunches.increment();
            throw badRequest("Запуск заблокирован: " + risk.blockingReason(), "Измените маршрут или оформите риск-отмену.");
        }
        if (risk.decisionZone() == DecisionZone.WARNING && (request == null || !request.confirmWarning())) {
            throw badRequest("Risk-score требует явного подтверждения диспетчера", "Передайте confirmWarning=true и укажите основание решения.");
        }
        if (risk.decisionZone() == DecisionZone.WARNING && !hasText(request.reason())) {
            throw badRequest("Не указано основание запуска при предупреждении", "Опишите, почему рейс запускается при warning risk-score.");
        }
        ensureResourcesFree(mission.getHarvester().getId(), mission.getCrew().getId(), missionId);
        Instant now = Instant.now();
        mission.setStatus(MissionStatus.ACTIVE);
        mission.setActualStart(now);
        mission.setUpdatedAt(now);
        mission.getHarvester().setStatus(ResourceStatus.BUSY);
        mission.getCrew().setStatus(ResourceStatus.BUSY);
        MissionPlan plan = missionPlanRepository.findByMissionId(missionId).orElseGet(MissionPlan::new);
        plan.setMissionId(missionId);
        plan.setRouteVersion(mission.getRouteVersion());
        plan.setSafetyContact("Штаб безопасности: +7-900-HSMS");
        plan.setPublishedAt(now);
        plan.setAcknowledgedAt(null);
        plan.setAcknowledgedBy(null);
        missionPlanRepository.save(plan);
        audit.record(actor, "MISSION_LAUNCHED", "mission", missionId, missionId, Map.of(
                "riskScore", risk.riskScore(),
                "confirmedWarning", request != null && request.confirmWarning()
        ));
        return dto.mission(missionId);
    }

    @Override
    public MissionDto recordRiskAssessment(String actorLogin, long missionId, long riskSnapshotId, Instant calculatedAt) {
        access.requireAny(actorLogin, RoleCode.ROLE_SUPPLY_MANAGER, RoleCode.ROLE_HARVESTER_CREW, RoleCode.ROLE_SECURITY_HEADQUARTERS_OPERATOR, RoleCode.ROLE_ADMINISTRATOR);
        Mission mission = dto.missionEntity(missionId);
        MissionStatus nextStatus = mission.getStatus() == MissionStatus.ACTIVE ? MissionStatus.ACTIVE : MissionStatus.RISK_ASSESSED;
        mission.setRiskSnapshotId(riskSnapshotId);
        mission.setStatus(nextStatus);
        mission.setDraftMissingFields(null);
        mission.setRiskReviewRequiredAt(null);
        mission.setRiskReviewReason(null);
        mission.setUpdatedAt(calculatedAt == null ? Instant.now() : calculatedAt);
        return dto.mission(missionId);
    }

    @Override
    public MissionDto riskCancelMission(String actorLogin, long missionId, RiskCancelRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_SUPPLY_MANAGER, RoleCode.ROLE_ADMINISTRATOR);
        Mission missionEntity = dto.missionEntity(missionId);
        Instant now = Instant.now();
        String reason = blankToDefault(request == null ? null : request.reason(), "Отмена рейса из-за риска");
        missionEntity.setStatus(MissionStatus.RISK_CANCELLED);
        missionEntity.setCloseReason(reason);
        missionEntity.setClosedBy(actor.getLogin());
        missionEntity.setClosedAt(now);
        missionEntity.setRiskReviewRequiredAt(null);
        missionEntity.setRiskReviewReason(null);
        missionEntity.setUpdatedAt(now);
        releaseResources(missionEntity);
        missionRepository.saveAndFlush(missionEntity);
        riskCancelledMissions.increment();
        audit.record(actor, "MISSION_RISK_CANCELLED", "mission", missionId, missionId, Map.of(
                "reason", reason
        ));
        return dto.mission(missionId);
    }

    @Override
    @Transactional(readOnly = true)
    public MissionPlanDto missionPlan(long missionId) {
        return loadMissionPlan(missionId);
    }

    private MissionPlanDto loadMissionPlan(long missionId) {
        dto.requireMissionExists(missionId);
        return dto.plan(missionId)
                .orElseThrow(() -> new HsmsException(404, "План рейса еще не опубликован", "Запустите рейс после актуального расчета риска."));
    }

    @Override
    public MissionPlanDto missionPlan(String actorLogin, long missionId) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_HARVESTER_CREW, RoleCode.ROLE_SUPPLY_MANAGER, RoleCode.ROLE_ADMINISTRATOR);
        if (access.roles(actor).contains(RoleCode.ROLE_HARVESTER_CREW)
                && !access.roles(actor).contains(RoleCode.ROLE_SUPPLY_MANAGER)
                && !access.roles(actor).contains(RoleCode.ROLE_ADMINISTRATOR)) {
            requireAssignedCrewActor(actor, missionId, "Получайте план только по рейсу назначенного экипажа.");
        }
        return loadMissionPlan(missionId);
    }

    @Override
    public MissionPlanDto acknowledgePlan(String actorLogin, long missionId) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_HARVESTER_CREW, RoleCode.ROLE_ADMINISTRATOR);
        requireAssignedCrewActor(actor, missionId, "Подтверждайте план только по рейсу назначенного экипажа.");
        MissionPlanDto plan = loadMissionPlan(missionId);
        Instant now = Instant.now();
        MissionPlan entity = missionPlanRepository.findById(plan.id())
                .orElseThrow(() -> notFound("План рейса еще не опубликован", "Запустите рейс после актуального расчета риска."));
        entity.setAcknowledgedAt(now);
        entity.setAcknowledgedBy(actor.getLogin());
        audit.record(actor, "MISSION_PLAN_ACKNOWLEDGED", "mission_plan", plan.id(), missionId, Map.of("crew", actor.getLogin()));
        return loadMissionPlan(missionId);
    }

    @Override
    public MissionDto submitMissionReport(String actorLogin, long missionId, MissionReportRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_HARVESTER_CREW, RoleCode.ROLE_ADMINISTRATOR);
        MissionDto mission = dto.mission(missionId);
        if (mission.status() != MissionStatus.ACTIVE && mission.status() != MissionStatus.COMPLETED_PENDING_CLOSE) {
            throw badRequest("Итоговый отчет можно передать только по активному или завершаемому рейсу", "Проверьте статус рейса.");
        }
        requireAssignedCrewActor(actor, missionId, "Передавайте итоговый отчет только по рейсу назначенного экипажа.");
        requireCompleteMissionReport(request);
        Mission missionEntity = dto.missionEntity(missionId);
        Instant now = Instant.now();
        MissionReport report = missionReportRepository.findByMissionId(missionId).orElseGet(MissionReport::new);
        report.setMissionId(missionId);
        report.setActualStart(request.actualStart());
        report.setActualEnd(request.actualEnd());
        report.setSpiceAmount(request.spiceAmount());
        report.setHarvesterFinalStatus(request.harvesterFinalStatus().trim());
        report.setAbnormalSituations(request.abnormalSituations().trim());
        report.setSubmittedBy(actor.getLogin());
        report.setSubmittedAt(now);
        MissionReport savedReport = missionReportRepository.saveAndFlush(report);
        missionEntity.setReportId(savedReport.getId());
        missionEntity.setStatus(MissionStatus.COMPLETED_PENDING_CLOSE);
        missionEntity.setUpdatedAt(now);
        audit.record(actor, "MISSION_REPORT_SUBMITTED", "mission_report", savedReport.getId(), missionId, Map.of("spiceAmount", request.spiceAmount()));
        return dto.mission(missionId);
    }

    @Override
    public MissionDto closeMission(String actorLogin, long missionId, MissionCloseRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_SUPPLY_MANAGER, RoleCode.ROLE_ADMINISTRATOR);
        MissionDto mission = dto.mission(missionId);
        Mission missionEntity = dto.missionEntity(missionId);
        MissionStatus finalStatus = request == null || request.finalStatus() == null ? MissionStatus.CLOSED : request.finalStatus();
        if (finalStatus != MissionStatus.CLOSED && finalStatus != MissionStatus.CANCELLED && finalStatus != MissionStatus.LOST) {
            throw badRequest("Недопустимый финальный статус рейса", "Выберите CLOSED, CANCELLED или LOST.");
        }
        if (finalStatus == MissionStatus.CLOSED && mission.report() == null) {
            throw badRequest("Закрытие завершенного рейса невозможно без итогового отчета экипажа", "Сначала передайте итоговый отчет.");
        }
        if (finalStatus == MissionStatus.CLOSED) {
            requireCompleteMissionReport(mission.report());
        }
        boolean hasOpenIncident = mission.incidentIds().stream()
                .map(incidentId -> dto.incident(incidentId.longValue()))
                .anyMatch(incident -> incident.status() != IncidentStatus.CLOSED);
        if (hasOpenIncident && finalStatus == MissionStatus.CLOSED) {
            throw badRequest("Связанный инцидент еще не закрыт", "Закройте инцидент или выберите финальный статус LOST/CANCELLED.");
        }
        Instant now = Instant.now();
        String reason = blankToDefault(request == null ? null : request.reason(), "Рейс закрыт диспетчером");
        missionEntity.setStatus(finalStatus);
        missionEntity.setCloseReason(reason);
        missionEntity.setClosedBy(actor.getLogin());
        missionEntity.setClosedAt(now);
        missionEntity.setRiskReviewRequiredAt(null);
        missionEntity.setRiskReviewReason(null);
        missionEntity.setUpdatedAt(now);
        releaseResources(missionEntity);
        missionRepository.saveAndFlush(missionEntity);
        audit.record(actor, "MISSION_CLOSED", "mission", missionId, missionId, Map.of(
                "status", finalStatus.name(),
                "reason", reason
        ));
        return dto.mission(missionId);
    }

    @Override
    @Transactional(readOnly = true)
    public MissionTimelineDto missionTimeline(long missionId) {
        return dto.missionTimeline(missionId);
    }

    @Override
    @Transactional(readOnly = true)
    public String missionReportCsv() {
        return dto.missionReportCsv();
    }

    private void requireMissionInput(MissionCreateRequest request) {
        if (request == null) {
            throw badRequest("Не переданы параметры рейса", "Заполните обязательные параметры рейса.");
        }
        requireMissionInput(request.title(), request.zoneId(), request.harvesterId(), request.crewId(), request.plannedStart(), request.plannedEnd(), request.route());
    }

    private void requireMissionInput(String title, Long zoneId, Long harvesterId, Long crewId, Instant start, Instant end, List<RoutePointDto> route) {
        if (!hasText(title)) {
            throw badRequest("Не указано название рейса", "Заполните обязательные параметры рейса.");
        }
        dto.requireZone(zoneId);
        dto.requireHarvester(harvesterId);
        dto.requireCrew(crewId);
        requirePlanningWindow(start, end);
        requireRoute(route);
    }

    private List<String> missingMissionFields(MissionCreateRequest request) {
        if (request == null) {
            return List.of("title", "zoneId", "harvesterId", "crewId", "plannedStart", "plannedEnd", "route");
        }
        return missingMissionFields(request.title(), request.zoneId(), request.harvesterId(), request.crewId(), request.plannedStart(), request.plannedEnd(), request.route());
    }

    private List<String> missingMissionFields(String title, Long zoneId, Long harvesterId, Long crewId, Instant start, Instant end, List<RoutePointDto> route) {
        List<String> fields = new ArrayList<>();
        if (!hasText(title)) {
            fields.add("title");
        }
        if (zoneId == null || zoneId <= 0) {
            fields.add("zoneId");
        }
        if (harvesterId == null || harvesterId <= 0) {
            fields.add("harvesterId");
        }
        if (crewId == null || crewId <= 0) {
            fields.add("crewId");
        }
        if (start == null) {
            fields.add("plannedStart");
        }
        if (end == null) {
            fields.add("plannedEnd");
        }
        if (route == null || route.isEmpty()) {
            fields.add("route");
        }
        return fields;
    }

    private void requireCompleteMissionReport(MissionReportRequest request) {
        List<String> missing = new ArrayList<>();
        if (request == null) {
            missing.addAll(List.of("actualStart", "actualEnd", "spiceAmount", "harvesterFinalStatus", "abnormalSituations"));
        } else {
            if (request.actualStart() == null) {
                missing.add("actualStart");
            }
            if (request.actualEnd() == null) {
                missing.add("actualEnd");
            }
            if (request.spiceAmount() == null) {
                missing.add("spiceAmount");
            } else if (request.spiceAmount().signum() < 0) {
                missing.add("spiceAmount >= 0");
            }
            if (!hasText(request.harvesterFinalStatus())) {
                missing.add("harvesterFinalStatus");
            }
            if (!hasText(request.abnormalSituations())) {
                missing.add("abnormalSituations");
            }
            if (request.actualStart() != null && request.actualEnd() != null && !request.actualEnd().isAfter(request.actualStart())) {
                throw badRequest("Фактическое время добычи заполнено неверно", "Укажите фактическое окончание позже фактического начала добычи.");
            }
        }
        if (!missing.isEmpty()) {
            throw badRequest("Итоговый отчет заполнен не полностью", "Заполните поля: " + String.join(", ", missing) + ".");
        }
    }

    private void requireCompleteMissionReport(MissionReportDto report) {
        List<String> missing = new ArrayList<>();
        if (report == null) {
            missing.addAll(List.of("actualStart", "actualEnd", "spiceAmount", "harvesterFinalStatus", "abnormalSituations"));
        } else {
            if (report.actualStart() == null) {
                missing.add("actualStart");
            }
            if (report.actualEnd() == null) {
                missing.add("actualEnd");
            }
            if (report.spiceAmount() == null) {
                missing.add("spiceAmount");
            }
            if (!hasText(report.harvesterFinalStatus())) {
                missing.add("harvesterFinalStatus");
            }
            if (!hasText(report.abnormalSituations())) {
                missing.add("abnormalSituations");
            }
            if (report.actualStart() != null && report.actualEnd() != null && !report.actualEnd().isAfter(report.actualStart())) {
                throw badRequest("Фактическое время добычи в отчете заполнено неверно", "Исправьте фактическое окончание добычи перед закрытием рейса.");
            }
        }
        if (!missing.isEmpty()) {
            throw badRequest("Закрытие завершенного рейса невозможно без полного итогового отчета", "Заполните поля отчета: " + String.join(", ", missing) + ".");
        }
    }

    private void ensureResourcesFree(long harvesterId, long crewId, Long currentMissionId) {
        HarvesterDto harvester = dto.requireHarvester(harvesterId);
        CrewDto crew = dto.requireCrew(crewId);
        if (harvester.status() == ResourceStatus.MAINTENANCE || harvester.status() == ResourceStatus.LOST) {
            throw badRequest("Выбранный харвестер недоступен", "Выберите свободный харвестер.");
        }
        if (crew.status() == ResourceStatus.MAINTENANCE || crew.status() == ResourceStatus.LOST) {
            throw badRequest("Выбранный экипаж недоступен", "Выберите свободный экипаж.");
        }
        long count = missionRepository.countActiveResourceAssignments(ACTIVE_RESOURCE_STATUSES, harvesterId, crewId, currentMissionId);
        if (count > 0) {
            throw badRequest("Харвестер или экипаж уже назначены на активный рейс", "Выберите другой ресурс.");
        }
    }

    private void replaceRoute(Mission mission, List<RoutePointDto> route) {
        mission.replaceRoute(routePoints(route));
    }

    private void replacePersistedRoute(Mission mission, List<RoutePointDto> route) {
        mission.clearRoute();
        missionRepository.flush();
        mission.appendRoute(routePoints(route));
    }

    private List<MissionRoute> routePoints(List<RoutePointDto> route) {
        List<MissionRoute> points = new ArrayList<>();
        for (int index = 0; index < route.size(); index++) {
            RoutePointDto point = route.get(index);
            MissionRoute entity = new MissionRoute();
            entity.setSeqNo(index + 1);
            entity.setLat(point.lat());
            entity.setLon(point.lon());
            points.add(entity);
        }
        return points;
    }

    private com.hsms.backend.common.model.MiningZone resolveZone(Long zoneId) {
        Long normalized = normalizeId(zoneId);
        return normalized == null ? null : dto.zoneEntity(normalized);
    }

    private com.hsms.backend.harvester.model.Harvester resolveHarvester(Long harvesterId) {
        Long normalized = normalizeId(harvesterId);
        return normalized == null ? null : dto.harvesterEntity(normalized);
    }

    private com.hsms.backend.harvester.model.Crew resolveCrew(Long crewId) {
        Long normalized = normalizeId(crewId);
        return normalized == null ? null : dto.crewEntity(normalized);
    }

    private Long normalizeId(Long id) {
        return id == null || id <= 0 ? null : id;
    }

    private Long id(Object entity) {
        if (entity instanceof com.hsms.backend.common.model.MiningZone zone) {
            return zone.getId();
        }
        if (entity instanceof com.hsms.backend.harvester.model.Harvester harvester) {
            return harvester.getId();
        }
        if (entity instanceof com.hsms.backend.harvester.model.Crew crew) {
            return crew.getId();
        }
        return null;
    }

    private void requireAssignedCrewActor(HsmsUser actor, long missionId, String action) {
        if (access.roles(actor).contains(RoleCode.ROLE_ADMINISTRATOR)) {
            return;
        }
        Mission mission = dto.missionEntity(missionId);
        if (mission.getCrew() == null) {
            throw badRequest("Экипаж рейса не назначен", "Сначала заполните экипаж рейса.");
        }
        if (!actor.getLogin().equals(mission.getCrew().getAssignedLogin())) {
            throw forbidden("Экипаж не назначен на этот рейс", action);
        }
    }

    private List<RoutePointDto> nullToEmpty(List<RoutePointDto> route) {
        return route == null ? List.of() : route;
    }

    private void releaseResources(Mission mission) {
        if (mission.getHarvester() != null && mission.getHarvester().getStatus() == ResourceStatus.BUSY) {
            mission.getHarvester().setStatus(mission.getStatus() == MissionStatus.LOST ? ResourceStatus.LOST : ResourceStatus.READY);
        }
        if (mission.getCrew() != null && mission.getCrew().getStatus() == ResourceStatus.BUSY) {
            mission.getCrew().setStatus(ResourceStatus.READY);
        }
    }
}
