package com.hsms.backend.common;

public record AlarmResponse(long alarmId, long incidentId, String acknowledgement, IncidentDto incident) {
    }
