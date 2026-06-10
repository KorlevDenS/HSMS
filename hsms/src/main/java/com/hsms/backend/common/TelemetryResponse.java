package com.hsms.backend.common;

public record TelemetryResponse(
            long id,
            FreshnessStatus status,
            boolean riskMarkedStale,
            TelemetryEventDto event
    ) {
    }
