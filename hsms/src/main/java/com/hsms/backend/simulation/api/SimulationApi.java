package com.hsms.backend.simulation.api;

import com.hsms.backend.common.DesertSimulationRequest;
import com.hsms.backend.common.DesertSimulationResult;

public interface SimulationApi {
    DesertSimulationResult runDesertScenario(String actorLogin, DesertSimulationRequest request);
}
