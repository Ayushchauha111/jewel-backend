package com.example.jewell.controller;

import com.example.jewell.exception.FeatureDisabledException;
import com.example.jewell.security.services.UserDetailsImpl;
import com.example.jewell.service.ChatService;
import com.example.jewell.service.FeatureFlagService;
import com.example.jewell.service.FriendshipService;
import com.example.jewell.service.SocialSseService;
import com.example.jewell.service.UserSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SocialController {

    private static final Logger log = LoggerFactory.getLogger(SocialController.class);

    private final FriendshipService friendshipService;
    private final UserSearchService userSearchService;
    private final ChatService chatService;
    private final FeatureFlagService featureFlagService;
    private final SocialSseService socialSseService;
    private final com.example.jewell.security.jwt.JwtUtils jwtUtils;
    private final com.example.jewell.security.services.UserDetailsServiceImpl userDetailsService;

    public SocialController(
            FriendshipService friendshipService,
            UserSearchService userSearchService,
            ChatService chatService,
            FeatureFlagService featureFlagService,
            SocialSseService socialSseService,
            com.example.jewell.security.jwt.JwtUtils jwtUtils,
            com.example.jewell.security.services.UserDetailsServiceImpl userDetailsService
    ) {
        this.friendshipService = friendshipService;
        this.userSearchService = userSearchService;
        this.chatService = chatService;
        this.featureFlagService = featureFlagService;
        this.socialSseService = socialSseService;
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    private void checkSocialFeaturesEnabled() {
        if (!featureFlagService.isFeatureEnabled("social_features")) {
            throw new FeatureDisabledException("Social features are currently disabled");
        }
    }

    // ============ FRIEND ENDPOINTS ============

    /**
     * Send friend request
     */
    @PostMapping("/friends/request/{receiverId}")
    public ResponseEntity<Map<String, Object>> sendFriendRequest(
            @PathVariable Long receiverId,
            Authentication authentication) {
        try {
            checkSocialFeaturesEnabled();
            Long userId = getUserIdFromAuth(authentication);
            Map<String, Object> result = friendshipService.sendFriendRequest(userId, receiverId);
            
            // Notify receiver via SSE (instant friend request notification!)
            socialSseService.notifyFriendUpdate(receiverId, "friendRequest", result);
            
            return ResponseEntity.ok(result);
        } catch (FeatureDisabledException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An error occurred: " + e.getMessage());
            log.error("Unexpected error in sendFriendRequest", e);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Accept friend request
     */
    @PostMapping("/friends/accept/{requestId}")
    public ResponseEntity<Map<String, Object>> acceptFriendRequest(
            @PathVariable Long requestId,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        Map<String, Object> result = friendshipService.acceptFriendRequest(userId, requestId);
        
        // Notify the original requester that their request was accepted
        Object senderId = result.get("senderId");
        if (senderId != null) {
            socialSseService.notifyFriendUpdate(Long.valueOf(senderId.toString()), "friendAccepted", result);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Reject friend request
     */
    @PostMapping("/friends/reject/{requestId}")
    public ResponseEntity<Map<String, Object>> rejectFriendRequest(
            @PathVariable Long requestId,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        Map<String, Object> result = friendshipService.rejectFriendRequest(userId, requestId);
        
        // Notify the original requester that their request was rejected
        Object senderId = result.get("senderId");
        if (senderId != null) {
            socialSseService.notifyFriendUpdate(Long.valueOf(senderId.toString()), "friendRejected", result);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Remove friend
     */
    @DeleteMapping("/friends/{friendId}")
    public ResponseEntity<Map<String, Object>> removeFriend(
            @PathVariable Long friendId,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        Map<String, Object> result = friendshipService.removeFriend(userId, friendId);
        
        // Notify the removed friend
        socialSseService.notifyFriendUpdate(friendId, "friendRemoved", result);
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get friends list
     */
    @GetMapping("/friends")
    public ResponseEntity<List<Map<String, Object>>> getFriends(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(friendshipService.getFriends(userId));
    }

    /**
     * Get pending friend requests
     */
    @GetMapping("/friends/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingRequests(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(friendshipService.getPendingRequests(userId));
    }

    /**
     * Search users
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(
            @RequestParam String query,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(userSearchService.searchUsers(userId, query));
    }

    // ============ CHAT ENDPOINTS ============

    /**
     * SSE endpoint for real-time chat updates.
     * Eliminates 3-second polling - instant message notifications.
     * Accepts token via query param since EventSource doesn't support headers.
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatUpdates(
            Authentication authentication,
            @RequestParam(required = false) String token) {
        // Try to get userId from authentication first, then from token
        Long userId = null;
        
        if (authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof UserDetailsImpl) {
            userId = ((UserDetailsImpl) authentication.getPrincipal()).getId();
        }
        
        // If no auth from Spring Security, try to validate token manually
        if (userId == null && token != null && !token.isEmpty()) {
            try {
                userId = validateTokenAndGetUserId(token);
            } catch (Exception e) {
                log.warn("Invalid token for SSE: {}", e.getMessage());
            }
        }
        
        if (userId == null) {
            throw new RuntimeException("Authentication required");
        }
        
        return socialSseService.subscribeToChatUpdates(userId);
    }
    
    private Long validateTokenAndGetUserId(String token) {
        if (jwtUtils.validateJwtToken(token)) {
            String username = jwtUtils.getUserNameFromJwtToken(token);
            var userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);
            return userDetails.getId();
        }
        throw new RuntimeException("Invalid token");
    }

    /**
     * Send message
     */
    @PostMapping("/chat/{receiverId}")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long receiverId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        String content = body.get("content");
        Map<String, Object> message = chatService.sendMessage(userId, receiverId, content);
        
        // Notify receiver via SSE (instant message delivery!)
        socialSseService.notifyNewMessage(receiverId, message);
        
        return ResponseEntity.ok(message);
    }

    /**
     * Get conversation with a friend
     */
    @GetMapping("/chat/{friendId}")
    public ResponseEntity<List<Map<String, Object>>> getConversation(
            @PathVariable Long friendId,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(chatService.getConversation(userId, friendId));
    }

    /**
     * Get unread messages count
     */
    @GetMapping("/chat/unread")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        long count = chatService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ============ HELPER ============

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Not authenticated");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getId();
    }
}

