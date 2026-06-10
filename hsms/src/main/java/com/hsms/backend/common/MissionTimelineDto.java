package com.hsms.backend.common;

import java.util.List;

public record MissionTimelineDto(
            MissionDto mission,
            List<TelemetryEventDto> telemetry,
            List<IncidentDto> incidents,
            InsuranceCaseDto insuranceCase,
            List<AuditEventDto> audit
    ) {
        public MissionTimelineDto {
            telemetry = DomainCollections.immutableList(telemetry);
            incidents = DomainCollections.immutableList(incidents);
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
        public List<AuditEventDto> audit() {
            return DomainCollections.immutableList(audit);
        }
    }
