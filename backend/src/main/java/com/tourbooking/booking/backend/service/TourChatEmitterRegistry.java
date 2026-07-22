package com.tourbooking.booking.backend.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TourChatEmitterRegistry {

    private final Map<Long, Map<String, SseEmitter>> registry = new ConcurrentHashMap<>();

    public SseEmitter register(Long groupId, String clientKey) {
        SseEmitter emitter = new SseEmitter(0L); // 0 = no timeout
        registry.computeIfAbsent(groupId, k -> new ConcurrentHashMap<>())
                .put(clientKey, emitter);
        
        Runnable cleanup = () -> remove(groupId, clientKey);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        
        return emitter;
    }

    public void broadcast(Long groupId, Object data) {
        Map<String, SseEmitter> clients = registry.getOrDefault(groupId, Map.of());
        List<String> dead = new ArrayList<>();
        
        for (Map.Entry<String, SseEmitter> entry : clients.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event().name("group-message").data(data));
            } catch (IOException e) {
                // connection aborted by client, safe to ignore and remove
                dead.add(entry.getKey());
            } catch (Exception e) {
                dead.add(entry.getKey());
            }
        }
        
        dead.forEach(clients::remove);
    }

    private void remove(Long groupId, String key) {
        Map<String, SseEmitter> clients = registry.get(groupId);
        if (clients != null) {
            clients.remove(key);
            if (clients.isEmpty()) {
                registry.remove(groupId);
            }
        }
    }
}
