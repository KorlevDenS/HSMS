package com.hsms.backend.risk.api;

import com.hsms.backend.dispatch.api.MissionResponse;

public interface RiskApi {
    RiskResponse calcRisk(MissionResponse missionResponse);
}
