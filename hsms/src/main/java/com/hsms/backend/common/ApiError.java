package com.hsms.backend.common;

import java.time.Instant;

public record ApiError(String message, String action, int status, Instant timestamp) {
    }
