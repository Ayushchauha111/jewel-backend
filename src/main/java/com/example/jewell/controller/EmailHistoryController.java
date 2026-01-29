package com.example.jewell.controller;

import com.example.jewell.dto.ApiResponseDTO;
import com.example.jewell.model.EmailHistory;
import com.example.jewell.model.User;
import com.example.jewell.repository.UserRepository;
import com.example.jewell.service.EmailHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/email-history")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EmailHistoryController {
    
    @Autowired
    private EmailHistoryService emailHistoryService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get all email history (admin only)
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getAllEmailHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
            Page<EmailHistory> emailHistoryPage = emailHistoryService.getAllEmailHistory(pageable);
            
            Map<String, Object> result = new HashMap<>();
            result.put("emailHistory", emailHistoryPage.getContent());
            result.put("totalElements", emailHistoryPage.getTotalElements());
            result.put("totalPages", emailHistoryPage.getTotalPages());
            result.put("currentPage", emailHistoryPage.getNumber());
            result.put("pageSize", emailHistoryPage.getSize());
            
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true,
                "Email history retrieved successfully",
                result
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    /**
     * Get email history for a specific user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getUserEmailHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponseDTO<>(false, "User not found", null));
            }
            
            User user = userOpt.get();
            Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
            Page<EmailHistory> emailHistoryPage = emailHistoryService.getUserEmailHistory(user, pageable);
            
            Map<String, Object> result = new HashMap<>();
            result.put("emailHistory", emailHistoryPage.getContent());
            result.put("totalElements", emailHistoryPage.getTotalElements());
            result.put("totalPages", emailHistoryPage.getTotalPages());
            result.put("currentPage", emailHistoryPage.getNumber());
            result.put("pageSize", emailHistoryPage.getSize());
            result.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail()
            ));
            
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true,
                "User email history retrieved successfully",
                result
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    /**
     * Get email history for a specific template
     */
    @GetMapping("/template/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<List<EmailHistory>>> getTemplateEmailHistory(
            @PathVariable String templateId) {
        try {
            List<EmailHistory> emailHistory = emailHistoryService.getTemplateEmailHistory(templateId);
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true,
                "Template email history retrieved successfully",
                emailHistory
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    /**
     * Get email statistics by template
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Long>>> getEmailStatistics() {
        try {
            Map<String, Long> statistics = emailHistoryService.getEmailCountByTemplate();
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true,
                "Email statistics retrieved successfully",
                statistics
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    /**
     * Check if user has received a specific template
     */
    @GetMapping("/user/{userId}/template/{templateId}/check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> checkUserTemplate(
            @PathVariable Long userId,
            @PathVariable String templateId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponseDTO<>(false, "User not found", null));
            }
            
            User user = userOpt.get();
            boolean hasReceived = emailHistoryService.hasUserReceivedTemplate(user, templateId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("hasReceived", hasReceived);
            result.put("userId", userId);
            result.put("templateId", templateId);
            
            if (hasReceived) {
                Optional<EmailHistory> latestEmail = emailHistoryService.getLatestEmailByUserAndTemplate(user, templateId);
                latestEmail.ifPresent(email -> {
                    result.put("lastSentAt", email.getSentAt());
                    result.put("subject", email.getSubject());
                });
            }
            
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true,
                "Template check completed",
                result
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    /**
     * Get bulk user-template status for multiple users and templates
     * This endpoint reduces the number of API calls significantly
     */
    @PostMapping("/bulk-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Map<String, Object>>>> getBulkUserTemplateStatus(
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<?> userIdsRaw = (List<?>) request.get("userIds");
            @SuppressWarnings("unchecked")
            List<String> templateIds = (List<String>) request.get("templateIds");
            
            if (userIdsRaw == null || userIdsRaw.isEmpty() || templateIds == null || templateIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponseDTO<>(false, "userIds and templateIds are required", null));
            }
            
            // Convert userIds to Long, handling both Integer and Long types
            List<Long> userIds = userIdsRaw.stream()
                    .map(id -> {
                        if (id instanceof Long) {
                            return (Long) id;
                        } else if (id instanceof Integer) {
                            return ((Integer) id).longValue();
                        } else if (id instanceof Number) {
                            return ((Number) id).longValue();
                        } else {
                            throw new IllegalArgumentException("Invalid user ID type: " + id.getClass().getName());
                        }
                    })
                    .collect(Collectors.toList());
            
            // Use parallel stream for concurrent processing
            Map<String, Map<String, Object>> result = userIds.parallelStream()
                    .collect(Collectors.toConcurrentMap(
                            userId -> userId.toString(),
                            userId -> {
                                Optional<User> userOpt = userRepository.findById(userId);
                                if (userOpt.isEmpty()) {
                                    return new HashMap<>();
                                }
                                
                                User user = userOpt.get();
                                Map<String, Object> userStatus = new HashMap<>();
                                
                                // Process templates in parallel for each user
                                Map<String, Object> templateStatusMap = templateIds.parallelStream()
                                        .collect(Collectors.toConcurrentMap(
                                                templateId -> templateId,
                                                templateId -> {
                                                    Map<String, Object> templateStatus = new HashMap<>();
                                                    boolean hasReceived = emailHistoryService.hasUserReceivedTemplate(user, templateId);
                                                    templateStatus.put("sent", hasReceived);
                                                    
                                                    if (hasReceived) {
                                                        Optional<EmailHistory> latestEmail = emailHistoryService.getLatestEmailByUserAndTemplate(user, templateId);
                                                        latestEmail.ifPresent(email -> {
                                                            templateStatus.put("sentAt", email.getSentAt());
                                                            templateStatus.put("subject", email.getSubject());
                                                        });
                                                    }
                                                    
                                                    return templateStatus;
                                                }
                                        ));
                                
                                userStatus.putAll(templateStatusMap);
                                return userStatus;
                            }
                    ));
            
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true,
                "Bulk status retrieved successfully",
                result
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
}

