package com.hsms.backend.mission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class ProcessorEventListener {

    @Async
    @ApplicationModuleListener
    void handle(DataReceivedEvent event) throws InterruptedException {
        Thread.sleep(5000);
        log.info("Processing event: {}", event.payload());
    }
}