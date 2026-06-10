package com.hsms.backend.common;

import java.time.Instant;

public record AlarmRequest(String externalEventId, Instant eventTime, String reason) {
    }
