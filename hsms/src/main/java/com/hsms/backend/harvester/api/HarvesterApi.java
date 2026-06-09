package com.hsms.backend.harvester.api;

import java.util.List;

public interface HarvesterApi {

    HarvesterResponse createHarvester(HarvesterRequest harvester);
    CrewResponse createCrew(CrewRequest crew);

    HarvesterResponse getHarvesterById(Long harvesterId);
    CrewResponse getCrewById(Long crewId);

    // used to create new mission
    List<HarvesterResponse> getAllFreeHarvesters();
    List<CrewResponse> getAllFreeCrews();
}
