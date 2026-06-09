package com.hsms.backend.security.auth;

import com.hsms.backend.common.HsmsDomain.RoleCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Set;

public record HsmsPrincipal(
        long id,
        String login,
        String displayName,
        Set<RoleCode> roles
) {
    public Collection<? extends GrantedAuthority> authorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toList();
    }
}
