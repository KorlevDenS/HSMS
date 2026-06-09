package com.hsms.backend.security.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class HttpsEnforcementFilter extends OncePerRequestFilter {

    private final boolean requireHttps;

    public HttpsEnforcementFilter(@Value("${hsms.security.require-https:false}") boolean requireHttps) {
        this.requireHttps = requireHttps;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (requireHttps && !isPublicActuatorEndpoint(request) && !request.isSecure() && !"https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))) {
            response.sendError(HttpStatus.UPGRADE_REQUIRED.value(), "Требуется защищенное HTTPS-соединение.");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isPublicActuatorEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && (path.startsWith("/actuator/health") || path.equals("/actuator/prometheus") || path.equals("/actuator/info"));
    }
}
