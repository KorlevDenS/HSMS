package com.hsms.backend.common;

import java.math.BigDecimal;
import java.time.Instant;

public record MissionReportDto(
            long id,
            long missionId,
            Instant actualStart,
            Instant actualEnd,
            BigDecimal spiceAmount,
            String harvesterFinalStatus,
            String abnormalSituations,
            String submittedBy,
            Instant submittedAt
    ) {
    }
