package com.example.jewell.repository;

import com.example.jewell.model.ChatMessage;
import com.example.jewell.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // Get conversation between two users
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.sender = :user1 AND m.receiver = :user2) OR " +
           "(m.sender = :user2 AND m.receiver = :user1) " +
           "ORDER BY m.createdAt ASC")
    List<ChatMessage> findConversation(User user1, User user2);
    
    // Get unread messages count for a user
    long countByReceiverAndIsReadFalse(User receiver);
    
    // Get unread messages from a specific user
    long countByReceiverAndSenderAndIsReadFalse(User receiver, User sender);
    
    // Mark messages as read
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.receiver = :receiver AND m.sender = :sender AND m.isRead = false")
    void markAsRead(User receiver, User sender);
    
    // Get recent conversations (latest message with each friend)
    @Query("SELECT m FROM ChatMessage m WHERE m.id IN " +
           "(SELECT MAX(cm.id) FROM ChatMessage cm WHERE cm.sender = :user OR cm.receiver = :user " +
           "GROUP BY CASE WHEN cm.sender = :user THEN cm.receiver.id ELSE cm.sender.id END)")
    List<ChatMessage> findRecentConversations(User user);
    
    // Find all messages where user is sender
    List<ChatMessage> findBySender(User sender);
    
    // Find all messages where user is receiver
    List<ChatMessage> findByReceiver(User receiver);
}

