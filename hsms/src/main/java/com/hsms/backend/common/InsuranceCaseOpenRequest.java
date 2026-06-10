package com.hsms.backend.common;

import java.time.Instant;

public record InsuranceCaseOpenRequest(
            long missionId,
            Long incidentId,
            InsuranceTrigger trigger,
            String reason,
            Double pAttack,
            Integer riskScore,
            Long riskSnapshotId,
            Instant decisionAt,
            String decisionBy,
            Severity incidentSeverity,
            Instant incidentRegisteredAt,
            Instant incidentClosedAt,
            Boolean incidentSlaBreached,
            String incidentOperator,
            MissionStatus missionStatus
    ) {
    }
