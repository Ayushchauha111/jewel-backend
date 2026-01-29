package com.example.jewell.service;

import com.example.jewell.model.ChatMessage;
import com.example.jewell.model.User;
import com.example.jewell.repository.ChatMessageRepository;
import com.example.jewell.repository.FriendshipRepository;
import com.example.jewell.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatService(
            UserRepository userRepository,
            FriendshipRepository friendshipRepository,
            ChatMessageRepository chatMessageRepository
    ) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public Map<String, Object> sendMessage(Long senderId, Long receiverId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!friendshipRepository.areFriends(sender, receiver)) {
            throw new RuntimeException("Can only message friends");
        }

        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .build();
        chatMessageRepository.save(message);

        Map<String, Object> result = new HashMap<>();
        result.put("id", message.getId());
        result.put("content", message.getContent());
        result.put("createdAt", message.getCreatedAt().toString()); // ISO-8601 format for JS
        result.put("senderId", senderId);
        result.put("senderUsername", sender.getUsername());
        result.put("receiverId", receiverId);
        return result;
    }

    @Transactional
    public List<Map<String, Object>> getConversation(Long userId, Long friendId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        chatMessageRepository.markAsRead(user, friend);

        List<ChatMessage> messages = chatMessageRepository.findConversation(user, friend);

        return messages.stream().map(m -> {
            Map<String, Object> msg = new HashMap<>();
            msg.put("id", m.getId());
            msg.put("content", m.getContent());
            msg.put("senderId", m.getSender().getId());
            msg.put("senderUsername", m.getSender().getUsername());
            msg.put("createdAt", m.getCreatedAt().toString()); // ISO-8601 format for JS
            msg.put("isOwn", m.getSender().getId().equals(userId));
            return msg;
        }).collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return chatMessageRepository.countByReceiverAndIsReadFalse(user);
    }
}

