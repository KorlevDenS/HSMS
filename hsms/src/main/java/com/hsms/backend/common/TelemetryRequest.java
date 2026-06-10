package com.hsms.backend.common;

import java.time.Instant;

public record TelemetryRequest(
            String externalEventId,
            Instant eventTime,
            double lat,
            double lon,
            String equipmentStatus
    ) {
    }
