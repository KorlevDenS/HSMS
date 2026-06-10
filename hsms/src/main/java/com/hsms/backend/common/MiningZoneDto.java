package com.hsms.backend.common;

public record MiningZoneDto(long id, String name, double riskLevel, String coordinates, boolean active) {
    }
