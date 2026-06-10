package com.hsms.backend.api_gateway;

import com.hsms.backend.common.IncidentDto;
import com.hsms.backend.common.InsuranceCaseOpenRequest;
import com.hsms.backend.common.InsuranceTrigger;
import com.hsms.backend.common.MissionCloseRequest;
import com.hsms.backend.common.MissionDto;
import com.hsms.backend.common.MissionPatchRequest;
import com.hsms.backend.common.MissionStatus;
import com.hsms.backend.common.RiskCancelRequest;
import com.hsms.backend.common.RiskSnapshotDto;
import com.hsms.backend.insurance.api.InsuranceApi;
import com.hsms.backend.mission.api.MissionApi;
import com.hsms.backend.risk.api.RiskApi;
import com.hsms.backend.security.api.SecurityApi;
import org.springframework.stereotype.Service;

@Service
public class HsmsMissionWorkflowService {

    private final MissionApi missionApi;
    private final RiskApi riskApi;
    private final SecurityApi securityApi;
    private final InsuranceApi insuranceApi;

    public HsmsMissionWorkflowService(
            MissionApi missionApi,
            RiskApi riskApi,
            SecurityApi securityApi,
            InsuranceApi insuranceApi
    ) {
        this.missionApi = missionApi;
        this.riskApi = riskApi;
        this.securityApi = securityApi;
        this.insuranceApi = insuranceApi;
    }

    public MissionDto updateMission(String actor, long missionId, MissionPatchRequest request) {
        MissionDto updated = missionApi.updateMission(actor, missionId, request);
        if (updated.draftMissingFields() != null && !updated.draftMissingFields().isBlank()) {
            riskApi.markRiskStaleAfterDomainChange(actor, missionId, "Черновик рейса изменен и требует заполнения обязательных полей");
            return missionApi.mission(missionId);
        }
        RiskSnapshotDto risk = riskApi.recalculateAfterDomainChange(actor, missionId, "Параметры рейса изменены диспетчером", false);
        missionApi.recordRiskAssessment(actor, missionId, risk.id(), risk.calculatedAt());
        return missionApi.mission(missionId);
    }

    public RiskSnapshotDto assessRisk(String actor, long missionId) {
        RiskSnapshotDto risk = riskApi.assessRisk(actor, missionId);
        missionApi.recordRiskAssessment(actor, missionId, risk.id(), risk.calculatedAt());
        return risk;
    }

    public MissionDto riskCancelMission(String actor, long missionId, RiskCancelRequest request) {
        RiskSnapshotDto risk = riskApi.assessRisk(actor, missionId);
        missionApi.recordRiskAssessment(actor, missionId, risk.id(), risk.calculatedAt());
        MissionDto cancelled = missionApi.riskCancelMission(actor, missionId, request);
        insuranceApi.openInsuranceCase(actor, new InsuranceCaseOpenRequest(
                missionId,
                null,
                InsuranceTrigger.RISK_CANCELLATION,
                cancelled.closeReason(),
                risk.pAttack(),
                risk.riskScore(),
                risk.id(),
                cancelled.closedAt(),
                actor,
                null,
                null,
                null,
                null,
                null,
                cancelled.status()
        ));
        return missionApi.mission(missionId);
    }

    public MissionDto closeMission(String actor, long missionId, MissionCloseRequest request) {
        MissionDto closed = missionApi.closeMission(actor, missionId, request);
        if (!closed.incidentIds().isEmpty() || closed.status() == MissionStatus.LOST) {
            Long incidentId = closed.incidentIds().isEmpty() ? null : closed.incidentIds().get(closed.incidentIds().size() - 1);
            InsuranceTrigger trigger = closed.status() == MissionStatus.LOST ? InsuranceTrigger.MISSION_LOSS : InsuranceTrigger.MISSION_CLOSE;
            IncidentDto incident = incidentId == null ? null : securityApi.incident(incidentId);
            insuranceApi.openInsuranceCase(actor, new InsuranceCaseOpenRequest(
                    missionId,
                    incidentId,
                    trigger,
                    closed.closeReason(),
                    closed.risk() == null ? null : closed.risk().pAttack(),
                    closed.risk() == null ? null : closed.risk().riskScore(),
                    closed.risk() == null ? null : closed.risk().id(),
                    closed.closedAt(),
                    actor,
                    incident == null ? null : incident.severity(),
                    incident == null ? null : incident.slaStartedAt(),
                    incident == null ? null : incident.closedAt(),
                    incident == null ? null : incident.slaBreached(),
                    incident == null ? null : incident.closedBy(),
                    closed.status()
            ));
        }
        return missionApi.mission(missionId);
    }
}
