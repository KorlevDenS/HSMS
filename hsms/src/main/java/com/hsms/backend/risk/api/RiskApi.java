package com.hsms.backend.risk.api;

import com.hsms.backend.dispatch.api.MissionResponse;

public interface RiskApi {
    RiskScoreResponse calcRiskScore(MissionResponse missionResponse);
    RiskScoreResponse getRiskScoreById(Long riskScoreId);
}
