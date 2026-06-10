package com.hsms.backend.common;

public record HarvesterDto(
            long id,
            String name,
            String type,
            ResourceStatus status,
            double noiseLevel,
            int capacity
    ) {
    }
