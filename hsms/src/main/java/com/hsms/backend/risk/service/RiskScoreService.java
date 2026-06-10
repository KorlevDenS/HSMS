package com.hsms.backend.risk.service;

import com.hsms.backend.auth.model.HsmsUser;
import com.hsms.backend.common.HsmsAccessService;
import com.hsms.backend.common.HsmsAuditService;
import com.hsms.backend.common.DataQuality;
import com.hsms.backend.common.DecisionZone;
import com.hsms.backend.common.FreshnessStatus;
import com.hsms.backend.common.HarvesterDto;
import com.hsms.backend.common.MiningZoneDto;
import com.hsms.backend.common.MissionDto;
import com.hsms.backend.common.MissionStatus;
import com.hsms.backend.common.ResourceStatus;
import com.hsms.backend.common.RiskPolicyDto;
import com.hsms.backend.common.RiskPolicyUpdateRequest;
import com.hsms.backend.common.RiskSnapshotDto;
import com.hsms.backend.common.RoleCode;
import com.hsms.backend.common.TelemetryEventDto;
import com.hsms.backend.readmodel.HsmsDtoAssembler;
import com.hsms.backend.risk.api.RiskApi;
import com.hsms.backend.risk.model.RiskPolicy;
import com.hsms.backend.risk.model.RiskScore;
import com.hsms.backend.risk.repository.RiskPolicyRepository;
import com.hsms.backend.risk.repository.RiskScoreRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static com.hsms.backend.common.HsmsOps.*;

@Service
@Transactional
public class RiskScoreService implements RiskApi {

    private static final int MAX_BLOCKING_THRESHOLD = 90;
    private static final double ACTIVE_MISSION_TELEMETRY_GAP_PENALTY = 0.45;
    private static final double DEGRADED_TELEMETRY_PENALTY = 0.35;

    private final HsmsAccessService access;
    private final HsmsAuditService audit;
    private final HsmsDtoAssembler dto;
    private final RiskPolicyRepository riskPolicyRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final Counter riskAssessments;

    public RiskScoreService(
            HsmsAccessService access,
            HsmsAuditService audit,
            HsmsDtoAssembler dto,
            RiskPolicyRepository riskPolicyRepository,
            RiskScoreRepository riskScoreRepository,
            MeterRegistry meterRegistry
    ) {
        this.access = access;
        this.audit = audit;
        this.dto = dto;
        this.riskPolicyRepository = riskPolicyRepository;
        this.riskScoreRepository = riskScoreRepository;
        this.riskAssessments = meterRegistry.counter("hsms_risk_assessments_total");
    }

    @Override
    public RiskSnapshotDto assessRisk(String actorLogin, long missionId) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_SUPPLY_MANAGER, RoleCode.ROLE_ADMINISTRATOR, RoleCode.ROLE_SECURITY_HEADQUARTERS_OPERATOR);
        RiskScore risk = calculateAndPersist(missionId, false);
        riskAssessments.increment();
        audit.record(actor, "RISK_ASSESSED", "risk_snapshot", risk.getId(), missionId, Map.of(
                "riskScore", risk.getRiskScore(),
                "pAttack", risk.getPAttack(),
                "decisionZone", risk.getDecisionZone().name()
        ));
        return dto.risk(risk.getId());
    }

    @Override
    public RiskPolicyDto updateRiskPolicy(String actorLogin, RiskPolicyUpdateRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_ADMINISTRATOR);
        RiskPolicyDto active = dto.activeRiskPolicy();
        int warning = request == null || request.warningThreshold() == null ? active.warningThreshold() : request.warningThreshold();
        int block = request == null || request.blockThreshold() == null ? active.blockThreshold() : request.blockThreshold();
        String changeReason = requirePolicyText(request == null ? null : request.changeReason(), "основание изменения политики риска");
        String validatedScenarios = requirePolicyText(request == null ? null : request.validatedScenarios(), "проверенные эталонные сценарии риска");
        String choamImpact = requirePolicyText(request == null ? null : request.choamImpact(), "влияние политики риска на страховой контур CHOAM");
        if (warning < 0 || warning > 100 || block < 0 || block > MAX_BLOCKING_THRESHOLD || warning >= block) {
            throw badRequest("Пороговые значения риска заполнены неверно",
                    "Укажите warningThreshold меньше blockThreshold, warningThreshold в диапазоне 0–100 и blockThreshold не выше " + MAX_BLOCKING_THRESHOLD + ".");
        }

        riskPolicyRepository.findByActiveTrue().forEach(policy -> policy.setActive(false));
        Instant now = Instant.now();
        RiskPolicy policy = new RiskPolicy();
        policy.setVersion(blankToDefault(request == null ? null : request.version(), "policy-" + now.toEpochMilli()));
        policy.setWarningThreshold(warning);
        policy.setBlockThreshold(block);
        policy.setFormulaDescription(blankToDefault(request == null ? null : request.formulaDescription(), active.formulaDescription()));
        policy.setActiveFrom(now);
        policy.setChangedBy(actor.getLogin());
        policy.setChangeReason(changeReason);
        policy.setValidatedScenarios(validatedScenarios);
        policy.setChoamImpact(choamImpact);
        policy.setActive(true);
        RiskPolicy saved = riskPolicyRepository.saveAndFlush(policy);
        audit.record(actor, "RISK_POLICY_UPDATED", "risk_policy", saved.getId(), null, Map.ofEntries(
                Map.entry("owner", actor.getLogin()),
                Map.entry("reason", changeReason),
                Map.entry("previousVersion", active.version()),
                Map.entry("previousWarningThreshold", active.warningThreshold()),
                Map.entry("previousBlockThreshold", active.blockThreshold()),
                Map.entry("previousFormulaDescription", active.formulaDescription()),
                Map.entry("newVersion", saved.getVersion()),
                Map.entry("newWarningThreshold", warning),
                Map.entry("newBlockThreshold", block),
                Map.entry("newFormulaDescription", saved.getFormulaDescription()),
                Map.entry("validatedScenarios", validatedScenarios),
                Map.entry("choamImpact", choamImpact)
        ));
        return dto.activeRiskPolicy();
    }

    @Override
    public void markRiskStaleAfterDomainChange(String actorLogin, long missionId, String reason) {
        HsmsUser actor = access.requireUser(actorLogin);
        String staleReason = blankToDefault(reason, "Изменились факторы риска");
        int marked = markOpenRiskSnapshotsStale(missionId, staleReason);
        audit.record(actor, "RISK_MARKED_STALE", "mission", missionId, missionId, Map.of(
                "reason", staleReason,
                "markedSnapshots", marked
        ));
    }

    @Override
    public RiskSnapshotDto recalculateAfterDomainChange(String actorLogin, long missionId, String reason, boolean includeIncidentPenalty) {
        HsmsUser actor = access.requireUser(actorLogin);
        String staleReason = blankToDefault(reason, "Изменились факторы риска");
        markOpenRiskSnapshotsStale(missionId, staleReason);
        RiskScore risk = calculateAndPersist(missionId, includeIncidentPenalty);
        audit.record(actor, "RISK_RECALCULATED_AFTER_CHANGE", "risk_snapshot", risk.getId(), missionId, Map.of(
                "reason", staleReason,
                "riskScore", risk.getRiskScore(),
                "decisionZone", risk.getDecisionZone().name()
        ));
        return dto.risk(risk.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public RiskSnapshotDto latestRisk(long missionId) {
        return dto.latestRisk(missionId)
                .orElseThrow(() -> notFound("Снимок риска не найден", "Повторите расчет риска."));
    }

    private RiskScore calculateAndPersist(long missionId, boolean includeIncidentPenalty) {
        MissionDto mission = dto.mission(missionId);
        requireCompleteMissionForRisk(mission);
        requireRoute(mission.route());
        MiningZoneDto zone = dto.requireZone(mission.zoneId());
        HarvesterDto harvester = dto.requireHarvester(mission.harvesterId());
        RiskPolicy policy = dto.activeRiskPolicyEntity();
        TelemetryEventDto latestTelemetry = dto.latestAcceptedTelemetry(missionId).orElse(null);
        boolean telemetryMissingForActiveMission = latestTelemetry == null && mission.status() == MissionStatus.ACTIVE;

        double zoneRiskLevel = zone.riskLevel();
        double harvesterNoiseLevel = harvester.noiseLevel();
        double timeWindowRisk = timeWindowRisk(mission.plannedStart());
        double routeComplexity = Math.min(1.0, Math.max(0.0, mission.route().size() / 10.0));
        double equipmentRisk = equipmentRisk(latestTelemetry, harvester.status());
        double telemetryPenalty = telemetryPenalty(latestTelemetry, mission.status());
        double incidentPenalty = includeIncidentPenalty || !mission.incidentIds().isEmpty() ? 0.75 : 0.0;

        double pAttack = clamp(0.10
                + zoneRiskLevel * 0.35
                + harvesterNoiseLevel * 0.20
                + timeWindowRisk * 0.15
                + routeComplexity * 0.10
                + telemetryPenalty * 0.10);
        int riskScoreValue = (int) Math.round(pAttack * 70
                + equipmentRisk * 10
                + telemetryPenalty * 10
                + incidentPenalty * 10);
        riskScoreValue = Math.max(0, Math.min(100, riskScoreValue));
        DecisionZone decisionZone = decisionZone(riskScoreValue, policy);
        DataQuality quality = dataQuality(telemetryMissingForActiveMission, telemetryPenalty);

        Instant now = Instant.now();
        RiskScore risk = new RiskScore();
        risk.setMissionId(missionId);
        risk.setPolicyVersion(policy.getVersion());
        risk.setPAttack(round(pAttack));
        risk.setRiskScore(riskScoreValue);
        risk.setLaunchAllowed(decisionZone != DecisionZone.BLOCKING);
        risk.setDecisionZone(decisionZone);
        risk.setBlockingReason(decisionZone == DecisionZone.BLOCKING ? "Risk-score выше блокирующего порога" : "");
        risk.setDataQuality(quality);
        risk.setCalculatedAt(now);
        risk.setValidForRouteVersion(mission.routeVersion());
        risk.setStale(false);

        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("zoneRiskLevel", round(zoneRiskLevel));
        factors.put("harvesterNoiseLevel", round(harvesterNoiseLevel));
        factors.put("timeWindowRisk", round(timeWindowRisk));
        factors.put("routeComplexity", round(routeComplexity));
        factors.put("equipmentRisk", round(equipmentRisk));
        factors.put("telemetryQualityPenalty", round(telemetryPenalty));
        factors.put("incidentPenalty", round(incidentPenalty));
        factors.forEach((name, value) -> risk.addFactor(name, value.doubleValue()));
        return riskScoreRepository.saveAndFlush(risk);
    }

    private DecisionZone decisionZone(int riskScoreValue, RiskPolicy policy) {
        if (riskScoreValue >= policy.getBlockThreshold()) {
            return DecisionZone.BLOCKING;
        }
        if (riskScoreValue >= policy.getWarningThreshold()) {
            return DecisionZone.WARNING;
        }
        return DecisionZone.ALLOWED;
    }

    private double equipmentRisk(TelemetryEventDto latestTelemetry, ResourceStatus harvesterStatus) {
        if (latestTelemetry != null) {
            return equipmentRisk(latestTelemetry.equipmentStatus());
        }
        if (harvesterStatus == ResourceStatus.MAINTENANCE) {
            return 0.8;
        }
        return 0.1;
    }

    private double telemetryPenalty(TelemetryEventDto latestTelemetry, MissionStatus missionStatus) {
        if (latestTelemetry != null) {
            return latestTelemetry.freshnessStatus() == FreshnessStatus.ACCEPTED ? 0.0 : DEGRADED_TELEMETRY_PENALTY;
        }
        if (missionStatus == MissionStatus.ACTIVE) {
            return ACTIVE_MISSION_TELEMETRY_GAP_PENALTY;
        }
        return 0.0;
    }

    private DataQuality dataQuality(boolean telemetryMissingForActiveMission, double telemetryPenalty) {
        if (telemetryMissingForActiveMission) {
            return DataQuality.STALE;
        }
        if (telemetryPenalty >= DEGRADED_TELEMETRY_PENALTY) {
            return DataQuality.DEGRADED;
        }
        return DataQuality.FRESH;
    }

    private int markOpenRiskSnapshotsStale(long missionId, String staleReason) {
        var snapshots = riskScoreRepository.findByMissionIdAndStaleFalse(missionId);
        snapshots.forEach(risk -> {
            risk.setStale(true);
            risk.setStaleReason(staleReason);
        });
        return snapshots.size();
    }

    private void requireCompleteMissionForRisk(MissionDto mission) {
        if (mission.zoneId() == null
                || mission.harvesterId() == null
                || mission.crewId() == null
                || mission.plannedStart() == null
                || mission.plannedEnd() == null) {
            throw badRequest("Рейс не готов к расчету риска", "Заполните обязательные параметры черновика: название, зона, харвестер, экипаж и плановое окно.");
        }
    }

    private double timeWindowRisk(Instant plannedStart) {
        if (plannedStart == null) {
            return 0.5;
        }
        int hour = plannedStart.atZone(java.time.ZoneOffset.UTC).getHour();
        if (hour >= 10 && hour <= 16) {
            return 0.7;
        }
        if (hour >= 4 && hour <= 8) {
            return 0.25;
        }
        return 0.45;
    }

    private double equipmentRisk(String status) {
        String normalized = blankToDefault(status, "NORMAL").toUpperCase(Locale.ROOT);
        if (normalized.contains("CRITICAL") || normalized.contains("DAMAGED") || normalized.contains("ПОВРЕЖ")) {
            return 0.9;
        }
        if (normalized.contains("WARN") || normalized.contains("DEGRADED")) {
            return 0.5;
        }
        return 0.1;
    }

    private String requirePolicyText(String value, String field) {
        if (!hasText(value)) {
            throw badRequest("Не указано " + field, "Заполните основание, проверенные сценарии и влияние на CHOAM перед изменением risk policy.");
        }
        return value.trim();
    }
}
