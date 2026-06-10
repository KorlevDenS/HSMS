package com.hsms.backend.common;

import java.util.Set;

public record UserCreateRequest(
            String login,
            String password,
            String displayName,
            String email,
            String phone,
            Set<RoleCode> roles
    ) {
        public UserCreateRequest {
            roles = DomainCollections.immutableSet(roles);
        }

        @Override
        public Set<RoleCode> roles() {
            return DomainCollections.immutableSet(roles);
        }
    }
