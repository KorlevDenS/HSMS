package com.hsms.backend.dispatch;

import org.jmolecules.event.types.DomainEvent;

@Deprecated
public record DataReceivedEvent(String payload) implements DomainEvent {
}
