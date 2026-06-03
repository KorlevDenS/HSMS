package com.hsms.backend.harvester.service;

import com.hsms.backend.harvester.api.CrewResponse;
import com.hsms.backend.harvester.api.HarvesterApi;
import com.hsms.backend.harvester.api.HarvesterResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HarvesterService implements HarvesterApi {

    @Override
    public List<HarvesterResponse> getAllFreeHarvesters() {
        return List.of();
    }

    @Override
    public List<CrewResponse> getAllFreeCrews() {
        return List.of();
    }

}
