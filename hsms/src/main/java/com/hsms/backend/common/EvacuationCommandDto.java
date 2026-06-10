package com.hsms.backend.common;

import java.time.Instant;

public record EvacuationCommandDto(
            long id,
            long incidentId,
            long missionId,
            EvacuationStatus status,
            Instant sentAt,
            Instant deliveredAt,
            String sentBy,
            Instant acknowledgedAt,
            String acknowledgedBy,
            Instant expiresAt,
            String deliveryError
    ) {
    }
