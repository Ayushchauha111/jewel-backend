package com.example.jewell.repository;

import com.example.jewell.model.EmailHistory;
import com.example.jewell.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailHistoryRepository extends JpaRepository<EmailHistory, Long> {
    
    // Find all emails sent to a specific user
    List<EmailHistory> findByUserOrderBySentAtDesc(User user);
    
    // Find all emails sent with a specific template
    List<EmailHistory> findByTemplateIdOrderBySentAtDesc(String templateId);
    
    // Find emails sent to a user with a specific template
    List<EmailHistory> findByUserAndTemplateIdOrderBySentAtDesc(User user, String templateId);
    
    // Check if a user has received a specific template
    boolean existsByUserAndTemplateId(User user, String templateId);
    
    // Find emails by status
    List<EmailHistory> findByStatusOrderBySentAtDesc(String status);
    
    // Find emails within a date range
    List<EmailHistory> findBySentAtBetweenOrderBySentAtDesc(LocalDateTime start, LocalDateTime end);
    
    // Find emails by user and date range
    List<EmailHistory> findByUserAndSentAtBetweenOrderBySentAtDesc(User user, LocalDateTime start, LocalDateTime end);
    
    // Paginated queries
    Page<EmailHistory> findAllByOrderBySentAtDesc(Pageable pageable);
    Page<EmailHistory> findByUserOrderBySentAtDesc(User user, Pageable pageable);
    Page<EmailHistory> findByTemplateIdOrderBySentAtDesc(String templateId, Pageable pageable);
    
    // Count emails by template
    long countByTemplateId(String templateId);
    
    // Count emails sent to a user
    long countByUser(User user);
    
    // Get latest email sent to a user with a specific template
    Optional<EmailHistory> findFirstByUserAndTemplateIdOrderBySentAtDesc(User user, String templateId);
    
    // Get statistics query
    @Query("SELECT e.templateId, COUNT(e) as count FROM EmailHistory e GROUP BY e.templateId")
    List<Object[]> getEmailCountByTemplate();
    
    // Get user email history with pagination
    @Query("SELECT e FROM EmailHistory e WHERE e.user.id = :userId ORDER BY e.sentAt DESC")
    Page<EmailHistory> findByUserIdOrderBySentAtDesc(@Param("userId") Long userId, Pageable pageable);
}

