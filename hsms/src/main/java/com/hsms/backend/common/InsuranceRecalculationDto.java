package com.hsms.backend.common;

import java.math.BigDecimal;
import java.time.Instant;

public record InsuranceRecalculationDto(
            long id,
            long insuranceCaseId,
            InsuranceHistoryEvent eventType,
            Long riskSnapshotId,
            BigDecimal oldPremium,
            BigDecimal newPremium,
            Integer oldRiskScore,
            Integer newRiskScore,
            String reason,
            Instant calculatedAt,
            String calculatedBy,
            String rejectedReason
    ) {
    }
