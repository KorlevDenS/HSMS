package com.hsms.backend.risk.service;

import com.hsms.backend.dispatch.api.MissionResponse;
import com.hsms.backend.risk.api.RiskApi;
import com.hsms.backend.risk.api.RiskScoreResponse;
import org.springframework.stereotype.Service;

@Service
public class RiskScoreService implements RiskApi {

    @Override
    public RiskScoreResponse calcRiskScore(MissionResponse missionResponse) {
        return null;
    }

    @Override
    public RiskScoreResponse getRiskScoreById(Long riskScoreId) {
        return null;
    }

}
