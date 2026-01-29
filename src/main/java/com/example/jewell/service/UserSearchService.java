package com.example.jewell.service;

import com.example.jewell.model.Friendship;
import com.example.jewell.model.User;
import com.example.jewell.model.UserStreak;
import com.example.jewell.repository.FriendshipRepository;
import com.example.jewell.repository.UserRepository;
import com.example.jewell.repository.UserStreakRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserSearchService {
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserStreakRepository streakRepository;

    public UserSearchService(
            UserRepository userRepository,
            FriendshipRepository friendshipRepository,
            UserStreakRepository streakRepository
    ) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.streakRepository = streakRepository;
    }

    public List<Map<String, Object>> searchUsers(Long userId, String query) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<User> users = userRepository.findByUsernameContainingIgnoreCase(query);

        return users.stream()
                .filter(u -> !u.getId().equals(userId))
                .limit(20)
                .map(u -> {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("id", u.getId());
                    userData.put("username", u.getUsername());

                    Optional<Friendship> friendship = friendshipRepository.findByUsers(currentUser, u);
                    userData.put("friendshipStatus", friendship.map(f -> f.getStatus().name()).orElse("NONE"));

                    Optional<UserStreak> streak = streakRepository.findByUser(u);
                    streak.ifPresent(userStreak -> userData.put("level", userStreak.getLevel()));

                    return userData;
                })
                .collect(Collectors.toList());
    }
}

