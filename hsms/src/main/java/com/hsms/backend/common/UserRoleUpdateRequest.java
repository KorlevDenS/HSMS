package com.hsms.backend.common;

import java.util.Set;

public record UserRoleUpdateRequest(Set<RoleCode> roles) {
        public UserRoleUpdateRequest {
            roles = DomainCollections.immutableSet(roles);
        }

        @Override
        public Set<RoleCode> roles() {
            return DomainCollections.immutableSet(roles);
        }
    }
