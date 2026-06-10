package com.hsms.backend.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record DashboardDto(
            long activeMissions,
            long closedMissions,
            long cancelledMissions,
            long openIncidents,
            long slaBreaches,
            double slaCompliancePercent,
            double averageReactionSeconds,
            long openInsuranceCases,
            BigDecimal averagePremium,
            Map<Severity, Long> incidentsBySeverity,
            Instant periodFrom,
            Instant periodTo
    ) {
        public DashboardDto {
            incidentsBySeverity = DomainCollections.immutableMap(incidentsBySeverity);
        }

        @Override
        public Map<Severity, Long> incidentsBySeverity() {
            return DomainCollections.immutableMap(incidentsBySeverity);
        }
    }
