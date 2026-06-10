package com.hsms.backend.api_gateway.controller;

import com.hsms.backend.common.HsmsDomain.DesertSimulationRequest;
import com.hsms.backend.common.HsmsDomain.DesertSimulationResult;
import com.hsms.backend.simulation.api.SimulationApi;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SimulationController extends HsmsControllerSupport {

    private final SimulationApi simulationApi;

    public SimulationController(SimulationApi simulationApi) {
        this.simulationApi = simulationApi;
    }

    @PostMapping("/simulation/desert-scenarios")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATOR')")
    public DesertSimulationResult runDesertSimulation(
            Authentication authentication,
            @RequestBody(required = false) DesertSimulationRequest request
    ) {
        return simulationApi.runDesertScenario(actor(authentication), request);
    }
}
