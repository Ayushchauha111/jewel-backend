package com.example.jewell.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.multipart.MultipartFile;

import com.example.jewell.dto.ApiResponseDTO;
import com.example.jewell.dto.UserDTO;
import com.example.jewell.model.User;
import com.example.jewell.service.UserService;
import com.example.jewell.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

   @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @SuppressWarnings("rawtypes")
    @PostMapping("/{userId}/profile-image")
    public ResponseEntity<ApiResponseDTO> uploadProfileImage(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        try {
            userService.uploadProfileImage(userId, file);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "User profile uploaded successfully.", null));
        } catch (Exception e){
            log.error("Error uploading profile image for userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @SuppressWarnings("rawtypes")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponseDTO> getUser(@PathVariable Long userId) {
    
        try {
            User user = userService.getUserById(userId);
            UserDTO userDTO=new UserDTO(userId, user.getProfileImageUrl());
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "User data returned successfully.", userDTO));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    // Admin endpoints
    /**
     * Get all users (admin only) - DEPRECATED: Use paginated endpoint instead
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<List<Map<String, Object>>>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            // Use sequential stream for lazy-loaded entities to avoid Hibernate session issues
            List<Map<String, Object>> userDTOs = users.stream().map(user -> {
                Map<String, Object> userMap = new java.util.HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("email", user.getEmail());
                userMap.put("profileImageUrl", user.getProfileImageUrl());
                userMap.put("referralCode", user.getReferralCode());
                userMap.put("earned", user.getEarned());
                if (user.getCreatedAt() != null) {
                    userMap.put("createdAt", user.getCreatedAt().toString());
                }
                userMap.put("roles", user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList()));
                return userMap;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true, 
                "Users fetched successfully.", 
                userDTOs
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }

    /**
     * Get paginated users (admin only)
     */
    @GetMapping("/admin/paginated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        try {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
            org.springframework.data.domain.Page<User> usersPage = userService.getUsersPaginated(pageable, search);
            
            // Use sequential stream for lazy-loaded entities to avoid Hibernate session issues
            // For paginated results (typically 10-50 items), parallel streams provide minimal benefit
            List<Map<String, Object>> userDTOs = usersPage.getContent().stream().map(user -> {
                Map<String, Object> userMap = new java.util.HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("email", user.getEmail());
                userMap.put("profileImageUrl", user.getProfileImageUrl());
                userMap.put("referralCode", user.getReferralCode());
                userMap.put("earned", user.getEarned());
                if (user.getCreatedAt() != null) {
                    userMap.put("createdAt", user.getCreatedAt().toString());
                }
                userMap.put("roles", user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList()));
                return userMap;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("content", userDTOs);
            response.put("totalElements", usersPage.getTotalElements());
            response.put("totalPages", usersPage.getTotalPages());
            response.put("currentPage", usersPage.getNumber());
            response.put("pageSize", usersPage.getSize());
            response.put("hasNext", usersPage.hasNext());
            response.put("hasPrevious", usersPage.hasPrevious());
            
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true, 
                "Users fetched successfully.", 
                response
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }

    /**
     * Get user count (admin only)
     */
    @GetMapping("/admin/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Long>>> getUserCount() {
        try {
            long count = userService.getUserCount();
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true, 
                "User count fetched successfully.", 
                Map.of("count", count)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }

    /**
     * Update user roles (admin only)
     */
    @PutMapping("/admin/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> updateUserRoles(
            @PathVariable Long userId,
            @RequestBody Map<String, Set<String>> request) {
        try {
            Set<String> roleNames = request.get("roles");
            if (roleNames == null || roleNames.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponseDTO<>(false, "Roles are required", null));
            }
            
            User updatedUser = userService.updateUserRoles(userId, roleNames);
            Map<String, Object> userMap = new java.util.HashMap<>();
            userMap.put("id", updatedUser.getId());
            userMap.put("username", updatedUser.getUsername());
            userMap.put("email", updatedUser.getEmail());
            userMap.put("roles", updatedUser.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()));
            
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true, 
                "User roles updated successfully.", 
                userMap
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }

    /**
     * Delete user (admin only)
     */
    @DeleteMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true, 
                "User deleted successfully.", 
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }

    /**
     * Send bulk email to selected users (admin only)
     */
    @PostMapping("/admin/bulk-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> sendBulkEmail(
            @RequestBody Map<String, Object> request) {
        try {
            String subject = (String) request.get("subject");
            String body = (String) request.get("body");
            String templateId = (String) request.get("templateId"); // Optional template ID
            @SuppressWarnings("unchecked")
            List<Object> selectedUserIdsRaw = (List<Object>) request.get("selectedUserIds");
            
            // Convert to List<Long> handling both Integer and Long types
            final List<Long> selectedUserIds;
            if (selectedUserIdsRaw != null && !selectedUserIdsRaw.isEmpty()) {
                selectedUserIds = selectedUserIdsRaw.stream()
                        .map(id -> {
                            if (id instanceof Long) return (Long) id;
                            if (id instanceof Integer) return ((Integer) id).longValue();
                            if (id instanceof String) return Long.parseLong((String) id);
                            return null;
                        })
                        .filter(id -> id != null)
                        .collect(java.util.stream.Collectors.toList());
            } else {
                selectedUserIds = null;
            }
            
            if (subject == null || subject.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponseDTO<>(false, "Subject is required", null));
            }
            
            if (body == null || body.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponseDTO<>(false, "Body is required", null));
            }
            
            // Get users - if selectedUserIds is provided and not empty, filter by those IDs; otherwise send to all
            List<User> allUsers = userService.getAllUsers();
            List<User> targetUsers;
            
            // Check if selectedUserIds is provided and not empty
            // If it's null or empty, send to all users
            if (selectedUserIds != null && !selectedUserIds.isEmpty()) {
                // Filter to only selected users using parallel stream
                targetUsers = allUsers.parallelStream()
                        .filter(user -> selectedUserIds.contains(user.getId()))
                        .collect(java.util.stream.Collectors.toList());
            } else {
                // If no selection provided or empty array, send to all users (backward compatibility)
                targetUsers = allUsers;
            }
            
            if (targetUsers.isEmpty()) {
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("totalUsers", allUsers.size());
                result.put("selectedUsers", 0);
                result.put("emailsSent", 0);
                result.put("emailsFailed", 0);
                return ResponseEntity.ok(new ApiResponseDTO<>(
                    true, 
                    "No users found to send email to.", 
                    result
                ));
            }
            
            // Use parallel stream for email extraction
            List<String> emailAddresses = targetUsers.parallelStream()
                    .map(User::getEmail)
                    .filter(email -> email != null && !email.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            
            // Create email to user map for tracking using parallel stream
            java.util.Map<String, User> emailToUserMap = targetUsers.parallelStream()
                    .filter(user -> user.getEmail() != null && !user.getEmail().trim().isEmpty())
                    .collect(java.util.stream.Collectors.toConcurrentMap(
                            User::getEmail,
                            user -> user,
                            (existing, replacement) -> existing // Keep first if duplicate emails
                    ));
            
            // Send bulk email with tracking if templateId is provided
            int successCount;
            if (templateId != null && !templateId.trim().isEmpty()) {
                successCount = emailService.sendBulkEmail(emailToUserMap, templateId, emailAddresses, subject, body);
            } else {
                // Fallback to old method if no templateId
                successCount = emailService.sendBulkEmail(emailAddresses, subject, body);
            }
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("totalUsers", allUsers.size());
            result.put("selectedUsers", targetUsers.size());
            result.put("emailsSent", successCount);
            result.put("emailsFailed", emailAddresses.size() - successCount);
            
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true, 
                "Bulk email sent successfully.", 
                result
            ));
        } catch (Exception e) {
            log.error("Error sending bulk email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
}