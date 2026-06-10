package com.hsms.backend.common;

import java.time.Instant;
import java.util.Map;

public record AuditEventDto(
            long id,
            String actorLogin,
            RoleCode actorRole,
            String action,
            String objectType,
            long objectId,
            Long missionId,
            Instant createdAt,
            Map<String, Object> details
    ) {
        public AuditEventDto {
            details = DomainCollections.immutableMap(details);
        }

        @Override
        public Map<String, Object> details() {
            return DomainCollections.immutableMap(details);
        }
    }
