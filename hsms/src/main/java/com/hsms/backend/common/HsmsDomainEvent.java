package com.hsms.backend.common;

import org.jmolecules.event.types.DomainEvent;

import java.time.Instant;

public record HsmsDomainEvent(
        String action,
        String objectType,
        long objectId,
        Long missionId,
        Instant occurredAt
) implements DomainEvent {

    public HsmsDomainEvent(String action, String objectType, long objectId, Long missionId) {
        this(action, objectType, objectId, missionId, Instant.now());
    }
}
