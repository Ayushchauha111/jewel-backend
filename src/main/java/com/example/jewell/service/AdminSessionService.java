package com.example.jewell.service;

import com.example.jewell.model.AdminSession;
import com.example.jewell.repository.AdminSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tracks admin login sessions (devices). Admins can be logged in from up to MAX_DEVICES (4) at a time.
 * When a 5th device logs in, the oldest session is removed.
 */
@Service
public class AdminSessionService {

    @Autowired
    private AdminSessionRepository adminSessionRepository;

    /**
     * Create or reuse a session for this admin. If already at MAX_DEVICES, evicts the oldest session.
     * @return session id to embed in JWT, or null if session creation failed
     */
    @Transactional
    public Long createOrRefreshSession(Long userId, HttpServletRequest request) {
        String deviceKey = computeDeviceKey(request);
        List<AdminSession> existing = adminSessionRepository.findByUserIdOrderByLastUsedAtAsc(userId);
        // Reuse same device: find by deviceKey and refresh lastUsedAt
        for (AdminSession s : existing) {
            if (s.getDeviceKey().equals(deviceKey)) {
                s.setLastUsedAt(java.time.LocalDateTime.now());
                adminSessionRepository.save(s);
                return s.getId();
            }
        }
        // New device: evict oldest if at limit
        while (existing.size() >= AdminSession.MAX_DEVICES && !existing.isEmpty()) {
            AdminSession oldest = existing.remove(0);
            adminSessionRepository.delete(oldest);
        }
        AdminSession session = new AdminSession(userId, deviceKey);
        session = adminSessionRepository.save(session);
        return session.getId();
    }

    public boolean isSessionValid(Long sessionId, Long userId) {
        if (sessionId == null) return false;
        return adminSessionRepository.findByIdAndUserId(sessionId, userId).isPresent();
    }

    /**
     * Get count of active (logged-in) devices/sessions for this admin.
     */
    public long getActiveSessionCount(Long userId) {
        return adminSessionRepository.countByUserId(userId);
    }

    /**
     * Get list of sessions for this admin (id, createdAt, lastUsedAt) for display.
     */
    public List<AdminSessionDto> getSessionsForUser(Long userId) {
        return adminSessionRepository.findByUserIdOrderByLastUsedAtAsc(userId).stream()
                .map(s -> new AdminSessionDto(s.getId(), s.getCreatedAt(), s.getLastUsedAt()))
                .collect(Collectors.toList());
    }

    public static class AdminSessionDto {
        private final Long id;
        private final LocalDateTime createdAt;
        private final LocalDateTime lastUsedAt;

        public AdminSessionDto(Long id, LocalDateTime createdAt, LocalDateTime lastUsedAt) {
            this.id = id;
            this.createdAt = createdAt;
            this.lastUsedAt = lastUsedAt;
        }

        public Long getId() { return id; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    }

    @Transactional
    public void refreshLastUsedAt(Long sessionId, Long userId) {
        adminSessionRepository.findByIdAndUserId(sessionId, userId).ifPresent(s -> {
            s.setLastUsedAt(java.time.LocalDateTime.now());
            adminSessionRepository.save(s);
        });
    }

    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        adminSessionRepository.findByIdAndUserId(sessionId, userId).ifPresent(adminSessionRepository::delete);
    }

    private static String computeDeviceKey(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) userAgent = "";
        String remote = request.getHeader("X-Forwarded-For");
        if (remote == null || remote.isEmpty()) remote = request.getRemoteAddr();
        if (remote == null) remote = "";
        String deviceId = request.getHeader("X-Device-Id");
        if (deviceId == null || deviceId.isBlank()) deviceId = "";
        String combined = userAgent + "|" + remote + "|" + deviceId;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(combined.hashCode());
        }
    }
}
