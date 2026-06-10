package com.hsms.backend.common;

import com.hsms.backend.auth.model.HsmsUser;
import com.hsms.backend.common.HsmsDomain.RoleCode;
import com.hsms.backend.common.model.AuditEvent;
import com.hsms.backend.common.repository.AuditEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class HsmsAuditService {

    private final AuditEventRepository auditEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Counter auditEvents;

    public HsmsAuditService(
            AuditEventRepository auditEventRepository,
            ApplicationEventPublisher eventPublisher,
            MeterRegistry meterRegistry
    ) {
        this.auditEventRepository = auditEventRepository;
        this.eventPublisher = eventPublisher;
        this.auditEvents = meterRegistry.counter("hsms_audit_events_total");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(HsmsUser actor, String action, String objectType, long objectId, Long missionId, Map<String, ?> details) {
        RoleCode actorRole = actor.getRoles().stream()
                .findFirst()
                .map(role -> RoleCode.valueOf(role.getName()))
                .orElse(RoleCode.ROLE_OPERATIONS_MANAGEMENT);
        AuditEvent event = new AuditEvent();
        event.setActorLogin(actor.getLogin());
        event.setActorRole(actorRole);
        event.setAction(action);
        event.setObjectType(objectType);
        event.setObjectId(objectId);
        event.setMissionId(missionId);
        event.setCreatedAt(Instant.now());
        if (details != null) {
            details.forEach((key, value) -> event.addDetail(key, value == null ? "" : value));
        }
        auditEventRepository.save(event);
        auditEvents.increment();
        eventPublisher.publishEvent(new HsmsDomainEvent(action, objectType, objectId, missionId));
    }
}
