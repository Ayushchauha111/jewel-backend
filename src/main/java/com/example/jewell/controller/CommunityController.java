package com.example.jewell.controller;

import com.example.jewell.dto.ApiResponseDTO;
import com.example.jewell.exception.FeatureDisabledException;
import com.example.jewell.model.CommunityConfig;
import com.example.jewell.model.ForestSession;
import com.example.jewell.model.Friendship;
import com.example.jewell.model.StudyBuddy;
import com.example.jewell.model.User;
import com.example.jewell.repository.FriendshipRepository;
import com.example.jewell.repository.UserRepository;
import com.example.jewell.security.services.UserDetailsImpl;
import com.example.jewell.model.StudyRoom;
import com.example.jewell.service.CommunityConfigService;
import com.example.jewell.service.FeatureFlagService;
import com.example.jewell.service.ForestService;
import com.example.jewell.service.StudyBuddyService;
import com.example.jewell.service.StudyRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CommunityController {
    private static final Logger log = LoggerFactory.getLogger(CommunityController.class);
    
    @Autowired
    private StudyBuddyService studyBuddyService;
    
    @Autowired
    private ForestService forestService;
    
    @Autowired
    private CommunityConfigService communityConfigService;
    
    @Autowired
    private FriendshipRepository friendshipRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeatureFlagService featureFlagService;

    private void checkCommunityHubEnabled() {
        if (!featureFlagService.isFeatureEnabled("community_hub")) {
            throw new FeatureDisabledException("Community hub feature is currently disabled");
        }
    }
    
    @Autowired
    private StudyRoomService studyRoomService;
    
    // ============ Community Config (Public) ============
    @GetMapping("/configs")
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> getAllConfigs() {
        try {
            Map<String, String> configs = communityConfigService.getAllActiveConfigs();
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Configs retrieved successfully.", configs));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/configs/{key}")
    public ResponseEntity<ApiResponseDTO<String>> getConfig(@PathVariable String key) {
        try {
            String value = communityConfigService.getConfigValue(key);
            if (value != null) {
                return ResponseEntity.ok(new ApiResponseDTO<>(true, "Config retrieved successfully.", value));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponseDTO<>(false, "Config not found.", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    // ============ Study Buddy Endpoints ============
    @PostMapping("/study-buddy")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<StudyBuddy>> createOrUpdateStudyBuddy(@RequestBody StudyBuddy studyBuddy) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            Long userId = userDetails.getId();
            
            // Clear user from request body to prevent conflicts
            studyBuddy.setUser(null);
            
            StudyBuddy saved = studyBuddyService.createOrUpdateStudyBuddy(userId, studyBuddy);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Study buddy profile updated successfully.", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, "Validation error: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creating/updating study buddy", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/study-buddy/me")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<StudyBuddy>> getMyStudyBuddy() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            Long userId = userDetails.getId();
            
            return studyBuddyService.getStudyBuddyByUserId(userId)
                    .map(buddy -> ResponseEntity.ok(new ApiResponseDTO<>(true, "Study buddy profile retrieved.", buddy)))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ApiResponseDTO<>(false, "Study buddy profile not found.", null)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/study-buddy/search")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<List<Map<String, Object>>>> searchStudyBuddies(
            @RequestParam(required = false) String exam,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) String search) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            Long currentUserId = userDetails.getId();
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            
            List<StudyBuddy> buddies = studyBuddyService.findAvailableBuddies(exam, availability, search);
            // Filter out current user's own profile and enrich with friendship status
            List<Map<String, Object>> enrichedBuddies = buddies.stream()
                    .filter(buddy -> buddy.getUser() != null && !buddy.getUser().getId().equals(currentUserId))
                    .map(buddy -> {
                        Map<String, Object> buddyMap = new java.util.HashMap<>();
                        buddyMap.put("id", buddy.getId());
                        buddyMap.put("exam", buddy.getExam());
                        buddyMap.put("availability", buddy.getAvailability());
                        buddyMap.put("status", buddy.getStatus());
                        buddyMap.put("bio", buddy.getBio());
                        buddyMap.put("preferredStudyMethod", buddy.getPreferredStudyMethod());
                        buddyMap.put("timezone", buddy.getTimezone());
                        buddyMap.put("isActive", buddy.getIsActive());
                        buddyMap.put("createdAt", buddy.getCreatedAt());
                        buddyMap.put("updatedAt", buddy.getUpdatedAt());
                        
                        // Add user info
                        Map<String, Object> userMap = new java.util.HashMap<>();
                        userMap.put("id", buddy.getUser().getId());
                        userMap.put("username", buddy.getUser().getUsername());
                        buddyMap.put("user", userMap);
                        
                        // Check friendship status
                        java.util.Optional<Friendship> friendship = friendshipRepository.findByUsers(currentUser, buddy.getUser());
                        if (friendship.isPresent()) {
                            buddyMap.put("friendshipStatus", friendship.get().getStatus().name());
                        } else {
                            buddyMap.put("friendshipStatus", "NONE");
                        }
                        
                        return buddyMap;
                    })
                    .toList();
            
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Study buddies retrieved successfully.", enrichedBuddies));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @DeleteMapping("/study-buddy")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> deactivateStudyBuddy() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            Long userId = userDetails.getId();
            
            studyBuddyService.deactivateStudyBuddy(userId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Study buddy profile deactivated.", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    // ============ Forest Group Endpoints ============
    @PostMapping("/forest/session")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<ForestSession>> addFocusSession(@RequestParam Integer focusMinutes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            Long userId = userDetails.getId();
            
            ForestSession session = forestService.addFocusSession(userId, focusMinutes);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Focus session recorded successfully.", session));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/forest/today")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<ForestSession>> getTodaySession() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            Long userId = userDetails.getId();
            
            return forestService.getTodaySession(userId)
                    .map(session -> ResponseEntity.ok(new ApiResponseDTO<>(true, "Today's session retrieved.", session)))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ApiResponseDTO<>(false, "No session found for today.", null)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/forest/history")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<List<ForestSession>>> getUserHistory() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            Long userId = userDetails.getId();
            
            List<ForestSession> history = forestService.getUserHistory(userId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "History retrieved successfully.", history));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/forest/leaderboard")
    public ResponseEntity<ApiResponseDTO<List<ForestSession>>> getDailyLeaderboard(
            @RequestParam(required = false) String date) {
        try {
            List<ForestSession> leaderboard;
            if (date != null && !date.trim().isEmpty()) {
                try {
                    java.time.LocalDate parsedDate = java.time.LocalDate.parse(date);
                    leaderboard = forestService.getDailyLeaderboardByDate(parsedDate);
                } catch (Exception e) {
                    leaderboard = forestService.getDailyLeaderboard();
                }
            } else {
                leaderboard = forestService.getDailyLeaderboard();
            }
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Leaderboard retrieved successfully.", leaderboard));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/forest/ranking/dates")
    public ResponseEntity<ApiResponseDTO<List<Map<String, Object>>>> getAvailableDates() {
        try {
            List<Map<String, Object>> dates = forestService.getAvailableDates();
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Available dates retrieved successfully.", dates));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/forest/stats")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Long>>> getUserStats() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            Long userId = userDetails.getId();
            
            Long totalTrees = forestService.getTotalTrees(userId);
            Long totalMinutes = forestService.getTotalFocusMinutes(userId);
            
            Map<String, Long> stats = Map.of(
                    "totalTrees", totalTrees,
                    "totalFocusMinutes", totalMinutes
            );
            
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Stats retrieved successfully.", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    // ============ Study Room Assignment Endpoints ============
    /**
     * Get room link without incrementing count (user must confirm they joined)
     */
    @GetMapping("/study-room/assign")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> assignStudyRoom(
            @RequestParam(required = false, defaultValue = "study_room") String roomType,
            Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            StudyRoom room = studyRoomService.getRoomLink(roomType, userId);
            if (room == null) {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("available", false);
                response.put("message", "No available rooms at the moment. Please try again later.");
                return ResponseEntity.ok(new ApiResponseDTO<>(true, "No rooms available.", response));
            }
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("available", true);
            response.put("roomId", room.getId());
            response.put("roomName", room.getRoomName());
            response.put("meetLink", room.getMeetLink());
            response.put("currentParticipants", room.getCurrentParticipants());
            response.put("maxCapacity", room.getMaxCapacity());
            response.put("availableSpots", room.getAvailableSpots());
            response.put("requiresConfirmation", true); // Frontend should show confirmation dialog
            
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Room link retrieved. Please confirm after joining.", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    /**
     * Confirm that user has joined the meeting (increments participant count)
     */
    @PostMapping("/study-room/confirm")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> confirmJoinStudyRoom(
            @RequestParam Long roomId,
            Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            StudyRoom room = studyRoomService.confirmJoin(roomId, userId);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("roomId", room.getId());
            response.put("roomName", room.getRoomName());
            response.put("currentParticipants", room.getCurrentParticipants());
            response.put("maxCapacity", room.getMaxCapacity());
            response.put("availableSpots", room.getAvailableSpots());
            
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Join confirmed. Participant count updated.", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    /**
     * Cancel join - user didn't actually join, remove the session
     */
    @PostMapping("/study-room/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> cancelJoinStudyRoom(
            @RequestParam Long roomId,
            Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            studyRoomService.cancelJoin(roomId, userId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Join cancelled. Session removed.", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/study-room/preview")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> previewStudyRoom(
            @RequestParam(required = false, defaultValue = "study_room") String roomType) {
        try {
            StudyRoom room = studyRoomService.getAvailableRoom(roomType);
            if (room == null) {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("available", false);
                response.put("message", "No available rooms at the moment.");
                return ResponseEntity.ok(new ApiResponseDTO<>(true, "No rooms available.", response));
            }
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("available", true);
            response.put("roomId", room.getId());
            response.put("roomName", room.getRoomName());
            response.put("meetLink", room.getMeetLink());
            response.put("currentParticipants", room.getCurrentParticipants());
            response.put("maxCapacity", room.getMaxCapacity());
            response.put("availableSpots", room.getAvailableSpots());
            
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Room preview retrieved.", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/study-room/release")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> releaseStudyRoom(
            @RequestParam Long roomId,
            Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            studyRoomService.releaseRoomSlot(roomId, userId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Room slot released.", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/study-room/stats")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getStudyRoomStats(
            @RequestParam(required = false, defaultValue = "study_room") String roomType) {
        try {
            Map<String, Object> stats = studyRoomService.getRoomStats(roomType);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Room stats retrieved.", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    // ============ Admin Endpoints ============
    @GetMapping("/admin/configs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<List<CommunityConfig>>> getAllConfigsAdmin() {
        try {
            List<CommunityConfig> configs = communityConfigService.getAllConfigs();
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "All configs retrieved.", configs));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/admin/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<CommunityConfig>> createOrUpdateConfig(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) Long id) {
        try {
            // Convert empty strings to null for optional parameters
            String processedType = (type != null && type.trim().isEmpty()) ? null : type;
            String processedDescription = (description != null && description.trim().isEmpty()) ? null : description;
            
            CommunityConfig config = communityConfigService.createOrUpdateConfig(
                key, value, processedType, processedDescription, displayOrder, id);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Config updated successfully.", config));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, "Validation error: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error joining study room", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    // ============ Admin Study Room Management ============
    @GetMapping("/admin/study-rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<List<StudyRoom>>> getAllStudyRooms(
            @RequestParam(required = false) String roomType) {
        try {
            List<StudyRoom> rooms = studyRoomService.getAllActiveRooms(roomType);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Study rooms retrieved.", rooms));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/admin/study-room")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<StudyRoom>> createStudyRoom(
            @RequestParam String roomName,
            @RequestParam String meetLink,
            @RequestParam(required = false) Integer maxCapacity,
            @RequestParam(required = false, defaultValue = "study_room") String roomType,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) String platform) {
        try {
            StudyRoom room = studyRoomService.createRoom(roomName, meetLink, maxCapacity, roomType, region, priority, platform);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Study room created successfully.", room));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @PutMapping("/admin/study-room/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<StudyRoom>> updateStudyRoom(
            @PathVariable Long roomId,
            @RequestParam String roomName,
            @RequestParam String meetLink,
            @RequestParam(required = false) Integer maxCapacity,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) String platform) {
        try {
            StudyRoom room = studyRoomService.updateRoom(roomId, roomName, meetLink, maxCapacity, roomType, region, priority, platform);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Study room updated successfully.", room));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @DeleteMapping("/admin/study-room/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> deleteStudyRoom(@PathVariable Long roomId) {
        try {
            studyRoomService.deleteRoom(roomId);
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("deleted", true);
            response.put("roomId", roomId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Study room deleted successfully.", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    // ============ Analytics Endpoints ============
    @GetMapping("/admin/study-rooms/analytics/room/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getRoomAnalytics(
            @PathVariable Long roomId,
            @RequestParam(required = false, defaultValue = "7") int days) {
        try {
            Map<String, Object> analytics = studyRoomService.getRoomAnalytics(roomId, days);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Room analytics retrieved.", analytics));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/admin/study-rooms/analytics/overall")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getOverallAnalytics(
            @RequestParam(required = false, defaultValue = "7") int days) {
        try {
            Map<String, Object> analytics = studyRoomService.getOverallAnalytics(days);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Overall analytics retrieved.", analytics));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
}

