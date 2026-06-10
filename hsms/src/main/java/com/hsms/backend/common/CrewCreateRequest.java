package com.hsms.backend.common;

public record CrewCreateRequest(
            String name,
            ResourceStatus status,
            String contactChannel,
            Integer memberCount,
            String assignedLogin
    ) {
    }
