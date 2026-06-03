package com.hsms.backend.api_gateway;

import com.hsms.backend.auth.api.AuthApi;
import com.hsms.backend.auth.api.RoleResponse;
import com.hsms.backend.dispatch.DataReceivedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiGatewayController {

    private final ApplicationEventPublisher events;
    private final AuthApi authApi;

    public ApiGatewayController(ApplicationEventPublisher events, AuthApi authApi) {
        this.events = events;
        this.authApi = authApi;
    }

    @Deprecated
    @Transactional
    @PostMapping("/process")
    public String process(@RequestBody String data) {
        events.publishEvent(new DataReceivedEvent(data));
        log.info("Sent");
        return "OK";
    }

    // for testing purposes
    @Deprecated
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(authApi.getAllRoles());
    }

}
