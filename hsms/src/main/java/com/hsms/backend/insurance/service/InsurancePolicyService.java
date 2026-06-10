package com.hsms.backend.insurance.service;

import com.hsms.backend.auth.model.HsmsUser;
import com.hsms.backend.common.HsmsAccessService;
import com.hsms.backend.common.HsmsAuditService;
import com.hsms.backend.common.HsmsDomain.IncidentDto;
import com.hsms.backend.common.HsmsDomain.IncidentStatus;
import com.hsms.backend.common.HsmsDomain.InsuranceCaseDto;
import com.hsms.backend.common.HsmsDomain.InsuranceCaseOpenRequest;
import com.hsms.backend.common.HsmsDomain.InsuranceCloseRequest;
import com.hsms.backend.common.HsmsDomain.InsuranceHistoryEvent;
import com.hsms.backend.common.HsmsDomain.InsuranceRecalculateRequest;
import com.hsms.backend.common.HsmsDomain.InsuranceRecalculationDto;
import com.hsms.backend.common.HsmsDomain.InsuranceRejectRequest;
import com.hsms.backend.common.HsmsDomain.InsuranceStatus;
import com.hsms.backend.common.HsmsDomain.InsuranceTermsRequest;
import com.hsms.backend.common.HsmsDomain.InsuranceTrigger;
import com.hsms.backend.common.HsmsDomain.MissionDto;
import com.hsms.backend.common.HsmsDomain.MissionStatus;
import com.hsms.backend.common.HsmsDomain.RiskSnapshotDto;
import com.hsms.backend.common.HsmsDomain.RoleCode;
import com.hsms.backend.common.HsmsDomain.Severity;
import com.hsms.backend.insurance.api.InsuranceApi;
import com.hsms.backend.insurance.model.InsuranceCase;
import com.hsms.backend.insurance.model.InsuranceRecalculation;
import com.hsms.backend.insurance.repository.InsuranceCaseRepository;
import com.hsms.backend.insurance.repository.InsuranceRecalculationRepository;
import com.hsms.backend.readmodel.HsmsDtoAssembler;
import com.hsms.backend.risk.api.RiskApi;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hsms.backend.common.HsmsOps.*;

@Service
@Transactional
public class InsurancePolicyService implements InsuranceApi {

    private final HsmsAccessService access;
    private final HsmsAuditService audit;
    private final HsmsDtoAssembler dto;
    private final InsuranceCaseRepository insuranceCaseRepository;
    private final InsuranceRecalculationRepository insuranceRecalculationRepository;
    private final RiskApi riskApi;
    private final Counter insuranceRecalculations;

    public InsurancePolicyService(
            HsmsAccessService access,
            HsmsAuditService audit,
            HsmsDtoAssembler dto,
            InsuranceCaseRepository insuranceCaseRepository,
            InsuranceRecalculationRepository insuranceRecalculationRepository,
            RiskApi riskApi,
            MeterRegistry meterRegistry
    ) {
        this.access = access;
        this.audit = audit;
        this.dto = dto;
        this.insuranceCaseRepository = insuranceCaseRepository;
        this.insuranceRecalculationRepository = insuranceRecalculationRepository;
        this.riskApi = riskApi;
        this.insuranceRecalculations = meterRegistry.counter("hsms_insurance_recalculations_total");
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceCaseDto> insuranceCases() {
        return dto.insuranceCases();
    }

    @Override
    @Transactional(readOnly = true)
    public InsuranceCaseDto insuranceCase(long caseId) {
        return dto.insuranceCase(caseId);
    }

    @Override
    public InsuranceCaseDto openInsuranceCase(String actorLogin, InsuranceCaseOpenRequest request) {
        HsmsUser actor = access.requireUser(actorLogin);
        if (request == null) {
            throw badRequest("Не переданы данные страхового кейса", "Передайте событие, по которому открывается страховой кейс.");
        }
        long missionId = request.missionId();
        Long incidentId = request.incidentId();
        dto.requireMissionExists(missionId);
        InsuranceCase existing = openInsuranceCaseEntity(missionId, incidentId);
        if (existing != null) {
            applyOpenSnapshot(existing, request);
            insuranceCaseRepository.saveAndFlush(existing);
            return dto.insuranceCase(existing.getId());
        }

        Instant now = Instant.now();
        InsuranceCase insurance = new InsuranceCase();
        insurance.setMissionId(missionId);
        insurance.setIncidentId(incidentId);
        insurance.setTriggerType(request.trigger() == null ? InsuranceTrigger.MISSION_CLOSE : request.trigger());
        insurance.setOpenedAt(now);
        insurance.setOpenedBy(actor.getLogin());
        applyOpenSnapshot(insurance, request);
        InsuranceCase saved = insuranceCaseRepository.saveAndFlush(insurance);
        audit.record(actor, "INSURANCE_CASE_OPENED", "insurance_case", saved.getId(), missionId, Map.of(
                "trigger", saved.getTriggerType().name(),
                "incidentId", incidentId == null ? "" : incidentId,
                "missingData", saved.getMissingData() == null ? "" : saved.getMissingData()
        ));
        return dto.insuranceCase(saved.getId());
    }

    @Override
    public InsuranceCaseDto recalculateInsurance(String actorLogin, long caseId, InsuranceRecalculateRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_INSURANCE_CONTOUR_OPERATOR, RoleCode.ROLE_ADMINISTRATOR);
        InsuranceCase insurance = dto.insuranceCaseEntity(caseId);
        String missing = sourceMissingData(insurance);
        if (hasText(missing)) {
            insurance.setMissingData(missing);
            insurance.setStatus(InsuranceStatus.WAITING_FOR_DATA);
            audit.record(actor, "INSURANCE_CASE_WAITING_FOR_DATA", "insurance_case", caseId, insurance.getMissionId(), Map.of("missingData", missing));
            return dto.insuranceCase(caseId);
        }
        MissionDto mission = dto.mission(insurance.getMissionId());
        String reason = blankToDefault(request == null ? null : request.reason(), "Страховой перерасчет");
        RiskSnapshotDto risk = riskApi.recalculateAfterDomainChange(
                actorLogin,
                insurance.getMissionId(),
                reason,
                insurance.getIncidentId() != null
        );
        BigDecimal oldPremium = insurance.getFinalPremium();
        Integer oldRiskScore = insurance.getFinalRiskScore();
        boolean slaBreached = insurance.getIncidentId() != null && dto.incident(insurance.getIncidentId()).slaBreached();
        BigDecimal newPremium = calculatePremium(risk.riskScore(), insurance.getIncidentId() != null, slaBreached, mission.status() == MissionStatus.LOST);
        insurance.setFinalPremium(newPremium);
        insurance.setFinalRiskScore(risk.riskScore());
        insurance.setStatus(InsuranceStatus.RECALCULATED);

        InsuranceRecalculation recalculation = new InsuranceRecalculation();
        recalculation.setEventType(InsuranceHistoryEvent.RECALCULATION);
        recalculation.setRiskSnapshotId(risk.id());
        recalculation.setOldPremium(oldPremium);
        recalculation.setNewPremium(newPremium);
        recalculation.setOldRiskScore(oldRiskScore);
        recalculation.setNewRiskScore(risk.riskScore());
        recalculation.setReason(reason);
        recalculation.setCalculatedAt(Instant.now());
        recalculation.setCalculatedBy(actor.getLogin());
        insurance.addRecalculation(recalculation);
        insurance.setMissingData(null);
        insuranceCaseRepository.saveAndFlush(insurance);

        insuranceRecalculations.increment();
        audit.record(actor, "INSURANCE_RECALCULATED", "insurance_case", caseId, insurance.getMissionId(), Map.of(
                "premium", newPremium,
                "riskScore", risk.riskScore()
        ));
        return dto.insuranceCase(caseId);
    }

    @Override
    public InsuranceCaseDto updateInsuranceTerms(String actorLogin, long caseId, InsuranceTermsRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_INSURANCE_CONTOUR_OPERATOR, RoleCode.ROLE_ADMINISTRATOR);
        InsuranceCase insurance = dto.insuranceCaseEntity(caseId);
        if (request == null || request.premium() == null) {
            throw badRequest("Не указан новый размер страховой премии", "Передайте premium после перерасчета.");
        }
        if (insurance.getFinalRiskScore() == null || insurance.getFinalPremium() == null) {
            insurance.setStatus(InsuranceStatus.WAITING_FOR_DATA);
            insurance.setMissingData("finalRiskScore, finalPremium");
            audit.record(actor, "INSURANCE_CASE_WAITING_FOR_DATA", "insurance_case", caseId, insurance.getMissionId(), Map.of("missingData", insurance.getMissingData()));
            return dto.insuranceCase(caseId);
        }
        BigDecimal oldPremium = insurance.getFinalPremium();
        insurance.setFinalPremium(request.premium());
        insurance.setStatus(InsuranceStatus.TERMS_UPDATED);
        InsuranceRecalculation termsUpdate = new InsuranceRecalculation();
        termsUpdate.setEventType(InsuranceHistoryEvent.TERMS_UPDATED);
        termsUpdate.setRiskSnapshotId(latestHistoryRiskSnapshotId(caseId));
        termsUpdate.setOldPremium(oldPremium);
        termsUpdate.setNewPremium(request.premium());
        termsUpdate.setOldRiskScore(insurance.getFinalRiskScore());
        termsUpdate.setNewRiskScore(insurance.getFinalRiskScore());
        termsUpdate.setReason(blankToDefault(request.reason(), "Обновление условий"));
        termsUpdate.setCalculatedAt(Instant.now());
        termsUpdate.setCalculatedBy(actor.getLogin());
        insurance.addRecalculation(termsUpdate);
        insurance.setMissingData(null);
        audit.record(actor, "INSURANCE_TERMS_UPDATED", "insurance_case", caseId, insurance.getMissionId(), Map.of(
                "premium", request.premium(),
                "reason", blankToDefault(request.reason(), "Обновление условий")
        ));
        return dto.insuranceCase(caseId);
    }

    @Override
    public InsuranceCaseDto rejectInsuranceRecalculation(String actorLogin, long caseId, InsuranceRejectRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_INSURANCE_CONTOUR_OPERATOR, RoleCode.ROLE_ADMINISTRATOR);
        InsuranceCase insurance = dto.insuranceCaseEntity(caseId);
        String reason = blankToDefault(request == null ? null : request.reason(), "Оператор отклонил расчет");
        insurance.setStatus(InsuranceStatus.REJECTED);
        insuranceRecalculationRepository.findFirstByInsuranceCaseIdOrderByCalculatedAtDescIdDesc(caseId)
                .ifPresent(recalculation -> recalculation.setRejectedReason(reason));
        audit.record(actor, "INSURANCE_RECALCULATION_REJECTED", "insurance_case", caseId, insurance.getMissionId(), Map.of("reason", reason));
        return dto.insuranceCase(caseId);
    }

    @Override
    public InsuranceCaseDto closeInsuranceCase(String actorLogin, long caseId, InsuranceCloseRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_INSURANCE_CONTOUR_OPERATOR, RoleCode.ROLE_ADMINISTRATOR);
        InsuranceCaseDto insuranceDto = dto.insuranceCase(caseId);
        InsuranceCase insurance = dto.insuranceCaseEntity(caseId);
        String missing = closeMissingData(insurance);
        if (hasText(missing)) {
            insurance.setMissingData(missing);
            insurance.setStatus(InsuranceStatus.WAITING_FOR_DATA);
            audit.record(actor, "INSURANCE_CASE_WAITING_FOR_DATA", "insurance_case", caseId, insurance.getMissionId(), Map.of("missingData", missing));
            return dto.insuranceCase(caseId);
        }
        if (insuranceDto.status() == InsuranceStatus.REJECTED) {
            throw badRequest("Отклоненный расчет нельзя закрыть как финальный", "Выполните повторный перерасчет и обновите условия кейса.");
        }
        MissionDto mission = dto.mission(insuranceDto.missionId());
        if (mission.status() == MissionStatus.ACTIVE || mission.status() == MissionStatus.COMPLETED_PENDING_CLOSE) {
            throw badRequest("Страховой кейс нельзя закрыть до финального статуса рейса", "Закройте рейс или оформите потерю/отмену.");
        }
        if (insuranceDto.incidentId() != null) {
            IncidentDto incident = dto.incident(insuranceDto.incidentId());
            if (incident.missionId() != mission.id()) {
                throw badRequest("Данные страхового кейса противоречивы", "Проверьте связь страхового кейса, рейса и инцидента.");
            }
            if (incident.status() != IncidentStatus.CLOSED) {
                throw badRequest("Связанный инцидент не закрыт", "Закройте инцидент перед финальным страховым решением.");
            }
            if (mission.status() == MissionStatus.LOST && incident.severity() == Severity.LOW) {
                throw badRequest("Статус рейса противоречит критичности инцидента", "Проверьте финальный статус рейса или классификацию инцидента.");
            }
        }
        Instant now = Instant.now();
        insurance.setStatus(InsuranceStatus.CLOSED);
        insurance.setClosedAt(now);
        insurance.setClosedBy(actor.getLogin());
        insurance.setMissingData(null);
        audit.record(actor, "INSURANCE_CASE_CLOSED", "insurance_case", caseId, insurance.getMissionId(), Map.of(
                "premium", insuranceDto.finalPremium(),
                "reason", blankToDefault(request == null ? null : request.reason(), "Кейс закрыт")
        ));
        return dto.insuranceCase(caseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceRecalculationDto> insuranceHistory(long caseId) {
        dto.insuranceCase(caseId);
        return dto.insuranceHistory(caseId);
    }

    private BigDecimal calculatePremium(int riskScore, boolean hasIncident, boolean slaBreached, boolean lost) {
        BigDecimal base = BigDecimal.valueOf(1000);
        BigDecimal riskFactor = BigDecimal.ONE.add(BigDecimal.valueOf(riskScore).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal incidentFactor = hasIncident ? BigDecimal.valueOf(1.25) : BigDecimal.ONE;
        BigDecimal slaFactor = slaBreached ? BigDecimal.valueOf(1.20) : BigDecimal.ONE;
        BigDecimal lossFactor = lost ? BigDecimal.valueOf(1.50) : BigDecimal.ONE;
        return base.multiply(riskFactor).multiply(incidentFactor).multiply(slaFactor).multiply(lossFactor).setScale(2, RoundingMode.HALF_UP);
    }

    private InsuranceCase openInsuranceCaseEntity(long missionId, Long incidentId) {
        if (incidentId == null) {
            return insuranceCaseRepository
                    .findFirstByMissionIdAndIncidentIdIsNullAndStatusNotOrderByOpenedAtDescIdDesc(missionId, InsuranceStatus.CLOSED)
                    .orElse(null);
        }
        return insuranceCaseRepository
                .findFirstByMissionIdAndIncidentIdAndStatusNotOrderByOpenedAtDescIdDesc(missionId, incidentId, InsuranceStatus.CLOSED)
                .orElse(null);
    }

    private void applyOpenSnapshot(InsuranceCase insurance, InsuranceCaseOpenRequest request) {
        if (insurance.getTriggerType() == null && request.trigger() != null) {
            insurance.setTriggerType(request.trigger());
        }
        if (hasText(request.reason())) {
            insurance.setTriggerReason(request.reason().trim());
        }
        if (request.pAttack() != null) {
            insurance.setTriggerPAttack(request.pAttack());
        }
        if (request.riskScore() != null) {
            insurance.setTriggerRiskScore(request.riskScore());
        }
        if (request.riskSnapshotId() != null) {
            insurance.setTriggerRiskSnapshotId(request.riskSnapshotId());
        }
        if (request.decisionAt() != null) {
            insurance.setTriggerDecisionAt(request.decisionAt());
        }
        if (hasText(request.decisionBy())) {
            insurance.setTriggerDecisionBy(request.decisionBy().trim());
        }
        if (request.incidentSeverity() != null) {
            insurance.setIncidentSeverity(request.incidentSeverity().name());
        }
        if (request.incidentRegisteredAt() != null) {
            insurance.setIncidentRegisteredAt(request.incidentRegisteredAt());
        }
        if (request.incidentClosedAt() != null) {
            insurance.setIncidentClosedAt(request.incidentClosedAt());
        }
        if (request.incidentSlaBreached() != null) {
            insurance.setIncidentSlaBreached(request.incidentSlaBreached());
        }
        if (hasText(request.incidentOperator())) {
            insurance.setIncidentOperator(request.incidentOperator().trim());
        }
        String missing = sourceMissingData(insurance);
        insurance.setMissingData(hasText(missing) ? missing : null);
        if (insurance.getStatus() == null || insurance.getStatus() == InsuranceStatus.WAITING_FOR_DATA || insurance.getStatus() == InsuranceStatus.READY_FOR_RECALCULATION) {
            insurance.setStatus(hasText(missing) ? InsuranceStatus.WAITING_FOR_DATA : InsuranceStatus.READY_FOR_RECALCULATION);
        }
    }

    private String closeMissingData(InsuranceCase insurance) {
        List<String> missing = missingData(insurance);
        if (insurance.getFinalRiskScore() == null) {
            missing.add("finalRiskScore");
        }
        if (insurance.getFinalPremium() == null) {
            missing.add("finalPremium");
        }
        return String.join(", ", missing);
    }

    private String sourceMissingData(InsuranceCase insurance) {
        return String.join(", ", missingData(insurance));
    }

    private List<String> missingData(InsuranceCase insurance) {
        List<String> missing = new ArrayList<>();
        if (insurance.getMissionId() == null) {
            missing.add("missionId");
        }
        if (insurance.getTriggerType() == InsuranceTrigger.RISK_CANCELLATION) {
            if (!hasText(insurance.getTriggerReason())) {
                missing.add("cancelReason");
            }
            if (insurance.getTriggerPAttack() == null) {
                missing.add("pAttack");
            }
            if (insurance.getTriggerRiskScore() == null) {
                missing.add("riskScore");
            }
            if (insurance.getTriggerDecisionAt() == null) {
                missing.add("decisionAt");
            }
            if (!hasText(insurance.getTriggerDecisionBy())) {
                missing.add("dispatcherId");
            }
        }
        if (insurance.getTriggerType() == InsuranceTrigger.INCIDENT || insurance.getTriggerType() == InsuranceTrigger.SLA_BREACH) {
            if (insurance.getIncidentId() == null) {
                missing.add("incidentId");
            }
            if (!hasText(insurance.getIncidentSeverity())) {
                missing.add("severity");
            }
            if (insurance.getIncidentRegisteredAt() == null) {
                missing.add("incidentRegisteredAt");
            }
            if (insurance.getIncidentClosedAt() == null) {
                missing.add("incidentClosedAt");
            }
            if (insurance.getIncidentSlaBreached() == null) {
                missing.add("slaBreached");
            }
            if (!hasText(insurance.getIncidentOperator())) {
                missing.add("operatorId");
            }
        }
        return missing;
    }

    private Long latestHistoryRiskSnapshotId(long caseId) {
        return insuranceRecalculationRepository.findFirstByInsuranceCaseIdOrderByCalculatedAtDescIdDesc(caseId)
                .map(InsuranceRecalculation::getRiskSnapshotId)
                .orElse(null);
    }
}
