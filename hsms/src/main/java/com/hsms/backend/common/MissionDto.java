package com.hsms.backend.common;

import java.time.Instant;
import java.util.List;

public record MissionDto(
            long id,
            String title,
            MissionStatus status,
            Long zoneId,
            String zoneName,
            Long harvesterId,
            String harvesterName,
            Long crewId,
            String crewName,
            Instant plannedStart,
            Instant plannedEnd,
            Instant actualStart,
            Instant closedAt,
            String closeReason,
            int routeVersion,
            List<RoutePointDto> route,
            RiskSnapshotDto risk,
            MissionPlanDto plan,
            MissionReportDto report,
            List<Long> incidentIds,
            Long insuranceCaseId,
            String closedBy,
            String draftMissingFields,
            int monitoringPriority,
            String monitoringContext,
            Instant riskReviewRequiredAt,
            String riskReviewReason,
            Instant createdAt,
            Instant updatedAt
    ) {
        public MissionDto {
            route = DomainCollections.immutableList(route);
            incidentIds = DomainCollections.immutableList(incidentIds);
        }

        @Override
        public List<RoutePointDto> route() {
            return DomainCollections.immutableList(route);
        }

        @Override
        public List<Long> incidentIds() {
            return DomainCollections.immutableList(incidentIds);
        }
    }
