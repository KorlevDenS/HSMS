package com.hsms.backend.common;

import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RealtimeEventService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe(String login) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        send(emitter, "CONNECTED", Map.of("login", login, "connectedAt", Instant.now().toString()));
        return emitter;
    }

    public void publish(HsmsDomainEvent event) {
        Map<String, Object> payload = Map.of(
                "action", event.action(),
                "objectType", event.objectType(),
                "objectId", event.objectId(),
                "missionId", event.missionId() == null ? "" : event.missionId(),
                "createdAt", Instant.now().toString()
        );
        emitters.forEach(emitter -> send(emitter, event.action(), payload));
    }

    @EventListener
    public void onDomainEvent(HsmsDomainEvent event) {
        publish(event);
    }

    private void send(SseEmitter emitter, String name, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(name).data(payload));
        } catch (IOException | IllegalStateException error) {
            emitters.remove(emitter);
        }
    }
}
