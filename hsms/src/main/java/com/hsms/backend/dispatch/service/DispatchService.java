package com.hsms.backend.dispatch.service;

import com.hsms.backend.dispatch.api.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DispatchService implements DispatchApi {

    @Override
    public MissionResponse createMission(MissionRequest missionRequest) {
        return null;
    }

    @Override
    public MissionResponse getMissionById(Long missionId) {
        return null;
    }

    @Override
    public List<MissionResponse> getAllMissions() {
        return List.of();
    }

    @Override
    public MissionPlanResponse getPlanById(Long planId) {
        return null;
    }

    @Override
    public MissionRouteResponse getRouteById(Long routeId) {
        return null;
    }


}
