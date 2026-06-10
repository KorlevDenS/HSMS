package com.hsms.backend.api_gateway.controller;

import com.hsms.backend.security.auth.HsmsPrincipal;
import org.springframework.security.core.Authentication;

abstract class HsmsControllerSupport {

    protected String actor(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof HsmsPrincipal principal) {
            return principal.login();
        }
        throw new IllegalStateException("Authenticated HSMS principal is missing");
    }
}
