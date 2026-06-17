package com.hsms.backend.api_gateway.controller;

import org.springframework.security.core.Authentication;

abstract class HsmsControllerSupport {

    protected String actor(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new IllegalStateException("Authenticated HSMS principal is missing");
    }
}
