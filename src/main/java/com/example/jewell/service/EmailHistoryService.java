package com.example.jewell.service;

import com.example.jewell.model.EmailHistory;
import com.example.jewell.model.User;
import com.example.jewell.repository.EmailHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmailHistoryService {
    
    @Autowired
    private EmailHistoryRepository emailHistoryRepository;
    
    /**
     * Save email history record
     */
    @Transactional
    public EmailHistory saveEmailHistory(EmailHistory emailHistory) {
        return emailHistoryRepository.save(emailHistory);
    }
    
    /**
     * Record successful email send
     */
    @Transactional
    public EmailHistory recordEmailSent(User user, String templateId, String subject, String emailAddress) {
        EmailHistory history = new EmailHistory(user, templateId, subject, emailAddress);
        return emailHistoryRepository.save(history);
    }
    
    /**
     * Record failed email send
     */
    @Transactional
    public EmailHistory recordEmailFailed(User user, String templateId, String subject, String emailAddress, String errorMessage) {
        EmailHistory history = new EmailHistory(user, templateId, subject, emailAddress, errorMessage);
        return emailHistoryRepository.save(history);
    }
    
    /**
     * Get all email history for a user
     */
    public List<EmailHistory> getUserEmailHistory(User user) {
        return emailHistoryRepository.findByUserOrderBySentAtDesc(user);
    }
    
    /**
     * Get email history for a user with pagination
     */
    public Page<EmailHistory> getUserEmailHistory(User user, Pageable pageable) {
        return emailHistoryRepository.findByUserOrderBySentAtDesc(user, pageable);
    }
    
    /**
     * Get all emails sent with a specific template
     */
    public List<EmailHistory> getTemplateEmailHistory(String templateId) {
        return emailHistoryRepository.findByTemplateIdOrderBySentAtDesc(templateId);
    }
    
    /**
     * Check if user has received a specific template
     */
    public boolean hasUserReceivedTemplate(User user, String templateId) {
        return emailHistoryRepository.existsByUserAndTemplateId(user, templateId);
    }
    
    /**
     * Get all email history with pagination
     */
    public Page<EmailHistory> getAllEmailHistory(Pageable pageable) {
        return emailHistoryRepository.findAllByOrderBySentAtDesc(pageable);
    }
    
    /**
     * Get email statistics by template
     */
    public Map<String, Long> getEmailCountByTemplate() {
        List<Object[]> results = emailHistoryRepository.getEmailCountByTemplate();
        return results.stream()
                .collect(Collectors.toMap(
                    result -> (String) result[0],
                    result -> (Long) result[1]
                ));
    }
    
    /**
     * Get email history within date range
     */
    public List<EmailHistory> getEmailHistoryByDateRange(LocalDateTime start, LocalDateTime end) {
        return emailHistoryRepository.findBySentAtBetweenOrderBySentAtDesc(start, end);
    }
    
    /**
     * Get latest email sent to user with template
     */
    public Optional<EmailHistory> getLatestEmailByUserAndTemplate(User user, String templateId) {
        return emailHistoryRepository.findFirstByUserAndTemplateIdOrderBySentAtDesc(user, templateId);
    }
    
    /**
     * Count emails by template
     */
    public long countEmailsByTemplate(String templateId) {
        return emailHistoryRepository.countByTemplateId(templateId);
    }
    
    /**
     * Count emails sent to user
     */
    public long countEmailsByUser(User user) {
        return emailHistoryRepository.countByUser(user);
    }
}

