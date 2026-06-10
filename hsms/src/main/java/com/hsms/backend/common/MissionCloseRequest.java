package com.hsms.backend.common;

public record MissionCloseRequest(MissionStatus finalStatus, String reason) {
    }
