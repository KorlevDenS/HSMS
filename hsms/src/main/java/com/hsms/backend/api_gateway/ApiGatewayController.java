package com.hsms.backend.api_gateway;

import com.hsms.backend.mission.DataReceivedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiGatewayController {

    private final ApplicationEventPublisher events;

    public ApiGatewayController(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Transactional
    @PostMapping("/process")
    public String process(@RequestBody String data) {
        events.publishEvent(new DataReceivedEvent(data));
        log.info("Sent");
        return "OK";
    }

}
