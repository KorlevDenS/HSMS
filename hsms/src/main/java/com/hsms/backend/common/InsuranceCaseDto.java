package com.hsms.backend.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InsuranceCaseDto(
            long id,
            long missionId,
            Long incidentId,
            InsuranceStatus status,
            InsuranceTrigger triggerType,
            Instant openedAt,
            String openedBy,
            String triggerReason,
            Double triggerPAttack,
            Integer triggerRiskScore,
            Long triggerRiskSnapshotId,
            Instant triggerDecisionAt,
            String triggerDecisionBy,
            Severity incidentSeverity,
            Instant incidentRegisteredAt,
            Instant incidentClosedAt,
            Boolean incidentSlaBreached,
            String incidentOperator,
            String missingData,
            Integer finalRiskScore,
            BigDecimal finalPremium,
            Instant closedAt,
            String closedBy,
            List<InsuranceRecalculationDto> history
    ) {
        public InsuranceCaseDto {
            history = DomainCollections.immutableList(history);
        }

        @Override
        public List<InsuranceRecalculationDto> history() {
            return DomainCollections.immutableList(history);
        }
    }
