package com.hsms.backend.common;

import java.time.Instant;

public record IncidentDto(
            long id,
            long missionId,
            long alarmSignalId,
            IncidentStatus status,
            Severity severity,
            String classificationReason,
            Instant slaStartedAt,
            Instant slaDeadlineAt,
            boolean slaBreached,
            Instant closedAt,
            String closedBy,
            EvacuationCommandDto evacuationCommand
    ) {
    }
