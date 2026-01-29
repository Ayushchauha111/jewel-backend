package com.example.jewell.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.jewell.model.User;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);
  
    Boolean existsByEmail(String email);

    User findByReferralCode(String referralCode);

    Optional<User> findByEmail(String email);
    
    Optional<User> findByResetToken(String resetToken);

    // Search users by username
    List<User> findByUsernameContainingIgnoreCase(String query);
    
    // Paginated search users by username
    org.springframework.data.domain.Page<User> findByUsernameContainingIgnoreCase(String query, org.springframework.data.domain.Pageable pageable);

    long countByLastActiveAtAfter(LocalDateTime cutoff);
    
    long countByLastActiveAtBetween(LocalDateTime start, LocalDateTime end);
    
    long countByCreatedAtAfter(LocalDateTime cutoff);
    
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<User> findByLastActiveAtAfter(LocalDateTime cutoff);
    
    List<User> findByLastActiveAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<User> findByCreatedAtAfter(LocalDateTime cutoff);
    
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}