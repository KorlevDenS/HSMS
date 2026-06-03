package com.hsms.backend.harvester.api;

import java.util.List;

public interface HarvesterApi {

    // used to create new mission
    List<HarvesterResponse> getAllFreeHarvesters();
    List<CrewResponse> getAllFreeCrews();
}
