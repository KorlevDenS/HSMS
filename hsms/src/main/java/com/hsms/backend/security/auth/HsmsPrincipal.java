package com.hsms.backend.security.auth;

import com.hsms.backend.common.RoleCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;

public record HsmsPrincipal(
        long id,
        String login,
        String displayName,
        Set<RoleCode> roles
) implements Principal {
    public HsmsPrincipal {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    @Override
    public Set<RoleCode> roles() {
        return Set.copyOf(roles);
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toList();
    }

    @Override
    public String getName() {
        return login;
    }
}
