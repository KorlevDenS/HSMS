package com.hsms.backend.common;

public record DesertSimulationRequest(
            long zoneId,
            long harvesterId,
            long crewId,
            int telemetryPoints,
            Severity threatSeverity,
            boolean includeAlarm
    ) {
    }
