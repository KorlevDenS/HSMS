package com.hsms.backend.harvester.service;

import com.hsms.backend.harvester.api.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HarvesterService implements HarvesterApi {

    @Override
    public HarvesterResponse createHarvester(HarvesterRequest harvester) {
        return null;
    }

    @Override
    public CrewResponse createCrew(CrewRequest crew) {
        return null;
    }

    @Override
    public HarvesterResponse getHarvesterById(Long harvesterId) {
        return null;
    }

    @Override
    public CrewResponse getCrewById(Long crewId) {
        return null;
    }

    @Override
    public List<HarvesterResponse> getAllFreeHarvesters() {
        return List.of();
    }

    @Override
    public List<CrewResponse> getAllFreeCrews() {
        return List.of();
    }

}
