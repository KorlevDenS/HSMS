package com.hsms.backend.common;

public record HarvesterCreateRequest(
            String name,
            String type,
            ResourceStatus status,
            Double noiseLevel,
            Integer capacity
    ) {
    }
