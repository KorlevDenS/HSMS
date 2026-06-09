package com.hsms.backend.api_gateway;

import com.hsms.backend.harvester.api.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/harvester")
public class HarvesterController {

    private final HarvesterApi harvesterApi;

    public HarvesterController(HarvesterApi harvesterApi) {
        this.harvesterApi = harvesterApi;
    }

    @GetMapping("/{harvesterId}")
    public ResponseEntity<HarvesterResponse> getHarvesterById(@PathVariable Long harvesterId) {
        return ResponseEntity.ok(harvesterApi.getHarvesterById(harvesterId));
    }

    @GetMapping("/crew/{crewId}")
    public ResponseEntity<CrewResponse> getCrewById(@PathVariable Long crewId) {
        return ResponseEntity.ok(harvesterApi.getCrewById(crewId));
    }

    @PostMapping("/")
    public ResponseEntity<HarvesterResponse> createHarvester(@RequestBody HarvesterRequest harvester) {
        return ResponseEntity.ok(harvesterApi.createHarvester(harvester));
    }

    @PostMapping("/crew")
    public ResponseEntity<CrewResponse> createCrew(@RequestBody CrewRequest crew) {
        return ResponseEntity.ok(harvesterApi.createCrew(crew));
    }

    @GetMapping("/all/free")
    public ResponseEntity<List<HarvesterResponse>> getFreeHarvesters() {
        return ResponseEntity.ok(harvesterApi.getAllFreeHarvesters());
    }

    @GetMapping("/crew/all/free")
    public ResponseEntity<List<CrewResponse>> getFreeCrews() {
        return ResponseEntity.ok(harvesterApi.getAllFreeCrews());
    }

}
