package com.example.jewell.service;

import com.example.jewell.model.Friendship;
import com.example.jewell.model.User;
import com.example.jewell.model.UserStreak;
import com.example.jewell.model.Friendship.FriendshipStatus;
import com.example.jewell.repository.ChatMessageRepository;
import com.example.jewell.repository.FriendshipRepository;
import com.example.jewell.repository.UserRepository;
import com.example.jewell.repository.UserStreakRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FriendshipService {
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserStreakRepository streakRepository;

    public FriendshipService(
            UserRepository userRepository,
            FriendshipRepository friendshipRepository,
            ChatMessageRepository chatMessageRepository,
            UserStreakRepository streakRepository
    ) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.streakRepository = streakRepository;
    }

    @Transactional
    public Map<String, Object> sendFriendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new RuntimeException("Cannot send friend request to yourself");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Friendship> existing = friendshipRepository.findByUsers(sender, receiver);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new RuntimeException("Already friends");
            }
            if (f.getStatus() == FriendshipStatus.PENDING) {
                throw new RuntimeException("Friend request already pending");
            }
            if (f.getStatus() == FriendshipStatus.BLOCKED) {
                throw new RuntimeException("Cannot send request");
            }
        }

        Friendship friendship = Friendship.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendshipStatus.PENDING)
                .build();
        friendshipRepository.save(friendship);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Friend request sent");
        return result;
    }

    @Transactional
    public Map<String, Object> acceptFriendRequest(Long userId, Long requestId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!friendship.getReceiver().getId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new RuntimeException("Request is not pending");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship.setAcceptedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Friend request accepted");
        return result;
    }

    @Transactional
    public Map<String, Object> rejectFriendRequest(Long userId, Long requestId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!friendship.getReceiver().getId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }

        friendship.setStatus(FriendshipStatus.REJECTED);
        friendshipRepository.save(friendship);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Friend request rejected");
        return result;
    }

    @Transactional
    public Map<String, Object> removeFriend(Long userId, Long friendId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("Friend not found"));

        Friendship friendship = friendshipRepository.findByUsers(user, friend)
                .orElseThrow(() -> new RuntimeException("Friendship not found"));

        friendshipRepository.delete(friendship);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Friend removed");
        return result;
    }

    public List<Map<String, Object>> getFriends(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Friendship> friendships = friendshipRepository.findFriendsByUser(user);

        return friendships.stream().map(f -> {
            User friend = f.getSender().getId().equals(userId) ? f.getReceiver() : f.getSender();
            Map<String, Object> friendData = new HashMap<>();
            friendData.put("id", friend.getId());
            friendData.put("username", friend.getUsername());
            friendData.put("email", friend.getEmail());
            friendData.put("friendsSince", f.getAcceptedAt());

            Optional<UserStreak> streak = streakRepository.findByUser(friend);
            streak.ifPresent(userStreak -> {
                friendData.put("level", userStreak.getLevel());
                friendData.put("currentStreak", userStreak.getCurrentStreak());
            });

            long unread = chatMessageRepository.countByReceiverAndSenderAndIsReadFalse(user, friend);
            friendData.put("unreadCount", unread);

            return friendData;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getPendingRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Friendship> pending = friendshipRepository.findByReceiverAndStatus(user, FriendshipStatus.PENDING);

        return pending.stream().map(f -> {
            Map<String, Object> request = new HashMap<>();
            request.put("id", f.getId());
            request.put("senderId", f.getSender().getId());
            request.put("senderUsername", f.getSender().getUsername());
            request.put("sentAt", f.getCreatedAt());
            return request;
        }).collect(Collectors.toList());
    }
}

