package com.hsms.backend.common;

import java.util.Set;

public record HsmsUserDto(
            long id,
            String login,
            String displayName,
            String email,
            String phone,
            Set<RoleCode> roles
    ) {
        public HsmsUserDto {
            roles = DomainCollections.immutableSet(roles);
        }

        @Override
        public Set<RoleCode> roles() {
            return DomainCollections.immutableSet(roles);
        }
    }
