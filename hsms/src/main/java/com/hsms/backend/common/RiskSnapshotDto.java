package com.hsms.backend.common;

import java.time.Instant;
import java.util.Map;

public record RiskSnapshotDto(
            long id,
            long missionId,
            String policyVersion,
            double pAttack,
            int riskScore,
            boolean launchAllowed,
            DecisionZone decisionZone,
            String blockingReason,
            Map<String, Double> factors,
            DataQuality dataQuality,
            Instant calculatedAt,
            int validForRouteVersion,
            boolean stale,
            String staleReason
    ) {
        public RiskSnapshotDto {
            factors = DomainCollections.immutableMap(factors);
        }

        @Override
        public Map<String, Double> factors() {
            return DomainCollections.immutableMap(factors);
        }
    }
