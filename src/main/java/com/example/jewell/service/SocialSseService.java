package com.example.jewell.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE service for real-time chat messages.
 * Eliminates 3-second polling - users receive instant message notifications.
 */
@Service
public class SocialSseService {
    private static final Logger log = LoggerFactory.getLogger(SocialSseService.class);
    
    // Per-user chat subscribers: userId -> list of emitters
    private final Map<Long, List<SseEmitter>> chatEmitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    
    public SocialSseService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    /**
     * Subscribe to chat messages for a specific user.
     */
    public SseEmitter subscribeToChatUpdates(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        List<SseEmitter> userEmitters = chatEmitters.computeIfAbsent(
            userId, k -> new CopyOnWriteArrayList<>()
        );
        
        emitter.onCompletion(() -> {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                chatEmitters.remove(userId);
            }
            log.debug("Chat SSE client disconnected for user {}. Total: {}", userId, userEmitters.size());
        });
        
        emitter.onTimeout(() -> {
            emitter.complete();
            userEmitters.remove(emitter);
        });
        
        emitter.onError(e -> {
            userEmitters.remove(emitter);
        });
        
        userEmitters.add(emitter);
        log.info("New chat SSE client for user {}. Total: {}", userId, userEmitters.size());
        
        return emitter;
    }

    /**
     * Send new message notification to a user.
     */
    public void notifyNewMessage(Long userId, Map<String, Object> message) {
        List<SseEmitter> userEmitters = chatEmitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        String data;
        try {
            data = objectMapper.writeValueAsString(Map.of("type", "newMessage", "message", message));
        } catch (Exception e) {
            log.error("Error serializing message data", e);
            return;
        }

        log.info("Sending new message notification to user {} (emitters: {})", userId, userEmitters.size());
        
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("chatUpdate")
                        .data(data));
            } catch (IOException e) {
                emitter.completeWithError(e);
                userEmitters.remove(emitter);
            }
        }
    }

    /**
     * Notify user of friend request or friendship change.
     */
    public void notifyFriendUpdate(Long userId, String eventType, Map<String, Object> data) {
        List<SseEmitter> userEmitters = chatEmitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        String jsonData;
        try {
            jsonData = objectMapper.writeValueAsString(Map.of("type", eventType, "data", data));
        } catch (Exception e) {
            log.error("Error serializing friend update data", e);
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("socialUpdate")
                        .data(jsonData));
            } catch (IOException e) {
                emitter.completeWithError(e);
                userEmitters.remove(emitter);
            }
        }
    }

    /**
     * Check if user has active SSE connections.
     */
    public boolean isUserConnected(Long userId) {
        List<SseEmitter> userEmitters = chatEmitters.get(userId);
        return userEmitters != null && !userEmitters.isEmpty();
    }
}
