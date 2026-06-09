package com.hsms.backend.dispatch.api;

import java.util.List;

public interface DispatchApi {
    MissionResponse createMission(MissionRequest missionRequest);
    MissionResponse getMissionById(Long missionId);
    List<MissionResponse> getAllMissions();

    MissionPlanResponse getPlanById(Long planId);
    MissionRouteResponse getRouteById(Long routeId);
}
