package com.hsms.backend.risk.api;

import com.hsms.backend.common.HsmsDomain.RiskPolicyDto;
import com.hsms.backend.common.HsmsDomain.RiskPolicyUpdateRequest;
import com.hsms.backend.common.HsmsDomain.RiskSnapshotDto;

public interface RiskApi {
    RiskSnapshotDto assessRisk(String actorLogin, long missionId);

    RiskPolicyDto updateRiskPolicy(String actorLogin, RiskPolicyUpdateRequest request);

    void markRiskStaleAfterDomainChange(String actorLogin, long missionId, String reason);

    RiskSnapshotDto recalculateAfterDomainChange(String actorLogin, long missionId, String reason, boolean includeIncidentPenalty);

    RiskSnapshotDto latestRisk(long missionId);
}
