package com.hsms.backend.api_gateway.controller;

import com.hsms.backend.common.CrewCreateRequest;
import com.hsms.backend.common.CrewDto;
import com.hsms.backend.common.HarvesterCreateRequest;
import com.hsms.backend.common.HarvesterDto;
import com.hsms.backend.common.TelemetryEventDto;
import com.hsms.backend.common.TelemetryRequest;
import com.hsms.backend.common.TelemetryResponse;
import com.hsms.backend.harvester.api.HarvesterApi;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class HarvesterController extends HsmsControllerSupport {

    private final HarvesterApi harvesterApi;

    public HarvesterController(HarvesterApi harvesterApi) {
        this.harvesterApi = harvesterApi;
    }

    @GetMapping("/harvesters")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public List<HarvesterDto> harvesters() {
        return harvesterApi.harvesters();
    }

    @PostMapping("/harvesters")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public HarvesterDto createHarvester(Authentication authentication, @RequestBody HarvesterCreateRequest request) {
        return harvesterApi.createHarvester(actor(authentication), request);
    }

    @GetMapping("/harvesters/free")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public List<HarvesterDto> freeHarvesters() {
        return harvesterApi.freeHarvesters();
    }

    @GetMapping("/harvesters/{harvesterId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public HarvesterDto harvester(@PathVariable long harvesterId) {
        return harvesterApi.harvester(harvesterId);
    }

    @GetMapping("/crews")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public List<CrewDto> crews() {
        return harvesterApi.crews();
    }

    @PostMapping("/crews")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public CrewDto createCrew(Authentication authentication, @RequestBody CrewCreateRequest request) {
        return harvesterApi.createCrew(actor(authentication), request);
    }

    @GetMapping("/crews/free")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public List<CrewDto> freeCrews() {
        return harvesterApi.freeCrews();
    }

    @GetMapping("/crews/{crewId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public CrewDto crew(@PathVariable long crewId) {
        return harvesterApi.crew(crewId);
    }

    @PostMapping("/missions/{missionId}/telemetry")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_ADMINISTRATOR')")
    public TelemetryResponse submitTelemetry(
            Authentication authentication,
            @PathVariable long missionId,
            @RequestBody TelemetryRequest request
    ) {
        return harvesterApi.submitTelemetry(actor(authentication), missionId, request);
    }

    @GetMapping("/missions/{missionId}/telemetry")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public List<TelemetryEventDto> telemetry(@PathVariable long missionId) {
        return harvesterApi.telemetry(missionId);
    }

    @GetMapping("/missions/{missionId}/telemetry/latest")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public TelemetryEventDto latestTelemetry(@PathVariable long missionId) {
        return harvesterApi.latestTelemetry(missionId);
    }
}
