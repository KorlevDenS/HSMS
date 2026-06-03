package com.hsms.backend.dispatch.service;

import com.hsms.backend.dispatch.api.DispatchApi;
import com.hsms.backend.dispatch.api.MissionRequest;
import com.hsms.backend.dispatch.api.MissionResponse;
import org.springframework.stereotype.Service;

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


}
