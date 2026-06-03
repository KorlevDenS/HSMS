package com.hsms.backend.dispatch.api;

public interface DispatchApi {
    MissionResponse createMission(MissionRequest missionRequest);
    MissionResponse getMissionById(Long missionId);
}
