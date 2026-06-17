package com.hsms.backend.common;

import java.util.List;

public record MissionTimelineDto(
            MissionDto mission,
            List<TelemetryEventDto> telemetry,
            List<IncidentDto> incidents,
            List<RiskSnapshotDto> riskHistory,
            InsuranceCaseDto insuranceCase,
            List<AuditEventDto> audit
    ) {
        public MissionTimelineDto {
            telemetry = DomainCollections.immutableList(telemetry);
            incidents = DomainCollections.immutableList(incidents);
            riskHistory = DomainCollections.immutableList(riskHistory);
            audit = DomainCollections.immutableList(audit);
        }

        @Override
        public List<TelemetryEventDto> telemetry() {
            return DomainCollections.immutableList(telemetry);
        }

        @Override
        public List<IncidentDto> incidents() {
            return DomainCollections.immutableList(incidents);
        }

        @Override
        public List<RiskSnapshotDto> riskHistory() {
            return DomainCollections.immutableList(riskHistory);
        }

        @Override
        public List<AuditEventDto> audit() {
            return DomainCollections.immutableList(audit);
        }
    }
