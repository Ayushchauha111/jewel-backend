package com.example.jewell.service;

import com.example.jewell.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserActivityService {
    private static final long MIN_UPDATE_INTERVAL_MS = 60_000; // throttle per user (1 min)
    private final Map<Long, Long> lastUpdateMsByUser = new ConcurrentHashMap<>();

    private final UserRepository userRepository;

    public UserActivityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void touch(Long userId) {
        if (userId == null) return;
        long now = System.currentTimeMillis();
        Long last = lastUpdateMsByUser.get(userId);
        if (last != null && (now - last) < MIN_UPDATE_INTERVAL_MS) return;
        lastUpdateMsByUser.put(userId, now);

        userRepository.findById(userId).ifPresent(u -> {
            u.setLastActiveAt(LocalDateTime.now());
            userRepository.save(u);
        });
    }
}

