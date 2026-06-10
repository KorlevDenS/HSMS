package com.hsms.backend.common;

import java.time.Instant;

public record MissionPlanDto(
            long id,
            long missionId,
            int routeVersion,
            String safetyContact,
            Instant publishedAt,
            Instant acknowledgedAt,
            String acknowledgedBy
    ) {
    }
