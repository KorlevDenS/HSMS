package com.hsms.backend.dispatch.api;

public record MissionRequest(
        // some other fields of Mission
        MissionPlanRequest missionPlan,
        MissionRouteRequest missionRoute
) {
}
