package com.hsms.backend.simulation.api;

import com.hsms.backend.common.HsmsDomain.DesertSimulationRequest;
import com.hsms.backend.common.HsmsDomain.DesertSimulationResult;

public interface SimulationApi {
    DesertSimulationResult runDesertScenario(String actorLogin, DesertSimulationRequest request);
}
