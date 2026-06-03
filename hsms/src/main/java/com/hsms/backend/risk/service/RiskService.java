package com.hsms.backend.risk.service;

import com.hsms.backend.dispatch.api.MissionResponse;
import com.hsms.backend.risk.api.RiskApi;
import com.hsms.backend.risk.api.RiskResponse;
import org.springframework.stereotype.Service;

@Service
public class RiskService implements RiskApi {

    @Override
    public RiskResponse calcRisk(MissionResponse missionResponse) {
        return null;
    }

}
