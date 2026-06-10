package com.hsms.backend.common;

import java.time.Instant;

public record TelemetryEventDto(
            long id,
            String externalEventId,
            long missionId,
            long crewId,
            double lat,
            double lon,
            String equipmentStatus,
            Instant eventTime,
            Instant receivedAt,
            Instant processedAt,
            FreshnessStatus freshnessStatus
    ) {
    }
