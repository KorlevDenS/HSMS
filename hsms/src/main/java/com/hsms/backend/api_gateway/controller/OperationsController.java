package com.hsms.backend.api_gateway.controller;

import com.hsms.backend.auth.api.AuthApi;
import com.hsms.backend.common.AuditEventDto;
import com.hsms.backend.common.BootstrapDto;
import com.hsms.backend.common.DashboardDto;
import com.hsms.backend.common.RealtimeEventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OperationsController extends HsmsControllerSupport {

    private final AuthApi authApi;
    private final RealtimeEventService realtimeEventService;

    public OperationsController(AuthApi authApi, RealtimeEventService realtimeEventService) {
        this.authApi = authApi;
        this.realtimeEventService = realtimeEventService;
    }

    @GetMapping("/bootstrap")
    public BootstrapDto bootstrap(Authentication authentication) {
        return authApi.bootstrap(actor(authentication));
    }

    @GetMapping("/dashboard/operations")
    @PreAuthorize("hasAnyAuthority('ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public DashboardDto dashboard(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        return authApi.dashboard(from, to);
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyAuthority('ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public List<AuditEventDto> audit() {
        return authApi.auditSnapshot();
    }

    @GetMapping("/incidents/stream")
    @PreAuthorize("isAuthenticated()")
    public SseEmitter events(Authentication authentication) {
        return realtimeEventService.subscribe(actor(authentication));
    }
}
