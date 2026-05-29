package com.hsms.backend.mission;

import org.jmolecules.event.types.DomainEvent;

public record DataReceivedEvent(String payload) implements DomainEvent {
}
