package com.hsms.backend.common;

public record CrewDto(long id, String name, ResourceStatus status, String contactChannel, int memberCount, String assignedLogin) {
    }
