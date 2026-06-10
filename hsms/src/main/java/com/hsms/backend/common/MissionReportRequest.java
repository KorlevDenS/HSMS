package com.hsms.backend.common;

import java.math.BigDecimal;
import java.time.Instant;

public record MissionReportRequest(
            Instant actualStart,
            Instant actualEnd,
            BigDecimal spiceAmount,
            String harvesterFinalStatus,
            String abnormalSituations
    ) {
    }
