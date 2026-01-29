package com.example.jewell.repository;

import com.example.jewell.model.Friendship;
import com.example.jewell.model.Friendship.FriendshipStatus;
import com.example.jewell.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    
    // Find friendship between two users (in either direction)
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.sender = :user1 AND f.receiver = :user2) OR " +
           "(f.sender = :user2 AND f.receiver = :user1)")
    Optional<Friendship> findByUsers(User user1, User user2);
    
    // Get all friends of a user (accepted status)
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.sender = :user OR f.receiver = :user) AND f.status = 'ACCEPTED'")
    List<Friendship> findFriendsByUser(User user);
    
    // Get pending friend requests for a user (where they are the receiver)
    List<Friendship> findByReceiverAndStatus(User receiver, FriendshipStatus status);
    
    // Get friend requests sent by a user
    List<Friendship> findBySenderAndStatus(User sender, FriendshipStatus status);
    
    // Check if two users are friends
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
           "((f.sender = :user1 AND f.receiver = :user2) OR (f.sender = :user2 AND f.receiver = :user1)) " +
           "AND f.status = 'ACCEPTED'")
    boolean areFriends(User user1, User user2);
}

