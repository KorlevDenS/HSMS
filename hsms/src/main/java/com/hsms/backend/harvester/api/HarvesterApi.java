package com.hsms.backend.harvester.api;

import com.hsms.backend.common.HsmsDomain.*;

import java.util.List;

public interface HarvesterApi {
    List<HarvesterDto> harvesters();

    List<HarvesterDto> freeHarvesters();

    HarvesterDto harvester(long harvesterId);

    HarvesterDto createHarvester(String actorLogin, HarvesterCreateRequest request);

    List<CrewDto> crews();

    List<CrewDto> freeCrews();

    CrewDto crew(long crewId);

    CrewDto createCrew(String actorLogin, CrewCreateRequest request);

    TelemetryResponse submitTelemetry(String actorLogin, long missionId, TelemetryRequest request);

    List<TelemetryEventDto> telemetry(long missionId);

    TelemetryEventDto latestTelemetry(long missionId);
}
