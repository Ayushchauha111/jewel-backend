package com.example.jewell.service;

import com.example.jewell.model.RoomUsage;
import com.example.jewell.model.StudyRoom;
import com.example.jewell.model.StudyRoomSession;
import com.example.jewell.model.User;
import com.example.jewell.repository.RoomUsageRepository;
import com.example.jewell.repository.StudyRoomRepository;
import com.example.jewell.repository.StudyRoomSessionRepository;
import com.example.jewell.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing study rooms and assigning users to available rooms
 */
@Service
public class StudyRoomService {
    
    @Autowired
    private StudyRoomRepository studyRoomRepository;
    
    @Autowired
    private RoomUsageRepository roomUsageRepository;
    
    @Autowired
    private StudyRoomSessionRepository studyRoomSessionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get an available room link for a user
     * Uses optimistic tracking: increments count immediately, but marks as "pending"
     * Auto-confirms after 2 minutes if user doesn't cancel
     */
    @Transactional
    public StudyRoom getRoomLink(String roomType, Long userId) {
        List<StudyRoom> availableRooms = studyRoomRepository.findAvailableRoomsByType(roomType);
        
        if (availableRooms.isEmpty()) {
            return null;
        }
        
        StudyRoom assignedRoom = availableRooms.get(0);
        
        // Check if user already has a session for this room
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Optional<StudyRoomSession> existingSession = studyRoomSessionRepository
                .findByUserAndRoomAndIsConfirmedFalse(user, assignedRoom);
        
        if (existingSession.isPresent()) {
            // Return existing session's room
            return existingSession.get().getRoom();
        }
        
        // Check if room still has space
        if (!assignedRoom.hasSpace()) {
            return null; // Room is full
        }
        
        // Optimistic approach: increment count immediately, but mark as pending
        // This prevents the "100 users clicked but didn't confirm" problem
        assignedRoom.incrementParticipants();
        studyRoomRepository.save(assignedRoom);
        
        // Create a pending session (will auto-confirm after 2 minutes)
        StudyRoomSession session = StudyRoomSession.builder()
                .user(user)
                .room(assignedRoom)
                .isConfirmed(false) // Will auto-confirm after 2 min
                .linkOpenedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5)) // 5 min to cancel
                .build();
        studyRoomSessionRepository.save(session);
        
        // Record usage for analytics (async)
        recordRoomUsage(assignedRoom);
        
        return assignedRoom;
    }
    
    /**
     * Confirm that user has joined the meeting
     * This increments the participant count
     */
    @Transactional
    public StudyRoom confirmJoin(Long roomId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        StudyRoom room = studyRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        
        // Find or create session
        Optional<StudyRoomSession> sessionOpt = studyRoomSessionRepository
                .findByUserAndRoomAndIsConfirmedFalse(user, room);
        
        StudyRoomSession session;
        if (sessionOpt.isPresent()) {
            session = sessionOpt.get();
            // Check if expired
            if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Session expired. Please join again.");
            }
        } else {
            // Create new session if doesn't exist
            session = StudyRoomSession.builder()
                    .user(user)
                    .room(room)
                    .isConfirmed(false)
                    .linkOpenedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
        }
        
        // Only increment if not already confirmed
        if (!session.getIsConfirmed()) {
            // Check if room still has space
            if (!room.hasSpace()) {
                throw new RuntimeException("Room is now full. Please try another room.");
            }
            
            // Mark session as confirmed
            session.setIsConfirmed(true);
            session.setConfirmedAt(LocalDateTime.now());
            studyRoomSessionRepository.save(session);
            
            // Increment participant count
            room.incrementParticipants();
            studyRoomRepository.save(room);
            
            // Record usage for analytics (async)
            recordRoomUsage(room);
        }
        
        return room;
    }
    
    /**
     * Get an available room for a user (legacy method - kept for backward compatibility)
     * Now redirects to getRoomLink
     */
    @Transactional
    public StudyRoom assignRoom(String roomType) {
        // For backward compatibility, but this should use getRoomLink with userId
        List<StudyRoom> availableRooms = studyRoomRepository.findAvailableRoomsByType(roomType);
        return availableRooms.isEmpty() ? null : availableRooms.get(0);
    }
    
    /**
     * Get room assignment without incrementing count (for preview)
     */
    public StudyRoom getAvailableRoom(String roomType) {
        List<StudyRoom> availableRooms = studyRoomRepository.findAvailableRoomsByType(roomType);
        return availableRooms.isEmpty() ? null : availableRooms.get(0);
    }
    
    /**
     * Release a room slot (when user leaves)
     */
    @Transactional
    public void releaseRoom(Long roomId) {
        Optional<StudyRoom> roomOpt = studyRoomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            StudyRoom room = roomOpt.get();
            room.decrementParticipants();
            studyRoomRepository.save(room);
        }
    }
    
    /**
     * Get room statistics
     */
    public Map<String, Object> getRoomStats(String roomType) {
        List<StudyRoom> rooms = studyRoomRepository.findByRoomTypeAndIsActiveTrueOrderByPriorityDesc(roomType);
        
        int totalCapacity = 0;
        int totalParticipants = 0;
        int availableRooms = 0;
        int fullRooms = 0;
        
        for (StudyRoom room : rooms) {
            totalCapacity += room.getMaxCapacity();
            totalParticipants += room.getCurrentParticipants();
            if (room.hasSpace()) {
                availableRooms++;
            } else {
                fullRooms++;
            }
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRooms", rooms.size());
        stats.put("totalCapacity", totalCapacity);
        stats.put("totalParticipants", totalParticipants);
        stats.put("availableSpots", totalCapacity - totalParticipants);
        stats.put("availableRooms", availableRooms);
        stats.put("fullRooms", fullRooms);
        stats.put("utilizationPercent", totalCapacity > 0 ? 
            (totalParticipants * 100.0 / totalCapacity) : 0);
        
        return stats;
    }
    
    /**
     * Create a new study room
     */
    @Transactional
    public StudyRoom createRoom(String roomName, String meetLink, Integer maxCapacity, 
                                 String roomType, String region, Integer priority, String platform) {
        // Auto-detect platform from link if not provided
        if (platform == null || platform.isEmpty()) {
            platform = detectPlatform(meetLink);
        }
        
        StudyRoom room = StudyRoom.builder()
                .roomName(roomName)
                .meetLink(meetLink)
                .maxCapacity(maxCapacity != null ? maxCapacity : 100)
                .roomType(roomType != null ? roomType : "study_room")
                .region(region)
                .priority(priority != null ? priority : 0)
                .platform(platform)
                .isActive(true)
                .currentParticipants(0)
                .build();
        
        return studyRoomRepository.save(room);
    }
    
    /**
     * Update an existing study room
     */
    @Transactional
    public StudyRoom updateRoom(Long roomId, String roomName, String meetLink, Integer maxCapacity, 
                                 String roomType, String region, Integer priority, String platform) {
        Optional<StudyRoom> roomOpt = studyRoomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            throw new RuntimeException("Study room not found with id: " + roomId);
        }
        
        StudyRoom room = roomOpt.get();
        
        // Auto-detect platform from link if not provided
        if (platform == null || platform.isEmpty()) {
            platform = detectPlatform(meetLink);
        }
        
        // Update fields
        room.setRoomName(roomName);
        room.setMeetLink(meetLink);
        if (maxCapacity != null) {
            room.setMaxCapacity(maxCapacity);
        }
        if (roomType != null) {
            room.setRoomType(roomType);
        }
        if (region != null) {
            room.setRegion(region);
        }
        if (priority != null) {
            room.setPriority(priority);
        }
        if (platform != null) {
            room.setPlatform(platform);
        }
        
        return studyRoomRepository.save(room);
    }
    
    /**
     * Detect platform from link URL
     */
    private String detectPlatform(String link) {
        if (link == null || link.isEmpty()) {
            return "other";
        }
        String lowerLink = link.toLowerCase();
        if (lowerLink.contains("meet.google.com") || lowerLink.contains("google.com/meet")) {
            return "google_meet";
        } else if (lowerLink.contains("youtube.com") || lowerLink.contains("youtu.be")) {
            return "youtube_live";
        } else if (lowerLink.contains("discord.com") || lowerLink.contains("discord.gg")) {
            return "discord";
        } else if (lowerLink.contains("zoom.us")) {
            return "zoom";
        } else {
            return "other";
        }
    }
    
    /**
     * Record room usage for analytics (async to not block main flow)
     */
    @Async
    @Transactional
    public void recordRoomUsage(StudyRoom room) {
        try {
            RoomUsage usage = RoomUsage.builder()
                    .room(room)
                    .participantCount(room.getCurrentParticipants())
                    .recordedAt(LocalDateTime.now())
                    .build();
            roomUsageRepository.save(usage);
        } catch (Exception e) {
            // Log error but don't fail the main operation
            System.err.println("Error recording room usage: " + e.getMessage());
        }
    }
    
    /**
     * Get analytics for a specific room
     */
    public Map<String, Object> getRoomAnalytics(Long roomId, int days) {
        Optional<StudyRoom> roomOpt = studyRoomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return new HashMap<>();
        }
        
        StudyRoom room = roomOpt.get();
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        Map<String, Object> analytics = new HashMap<>();
        
        // Basic stats
        Double avgParticipants = roomUsageRepository.getAverageParticipants(room, startDate);
        Integer maxParticipants = roomUsageRepository.getMaxParticipants(room, startDate);
        
        analytics.put("roomId", room.getId());
        analytics.put("roomName", room.getRoomName());
        analytics.put("averageParticipants", avgParticipants != null ? avgParticipants : 0);
        analytics.put("maxParticipants", maxParticipants != null ? maxParticipants : 0);
        analytics.put("currentParticipants", room.getCurrentParticipants());
        analytics.put("maxCapacity", room.getMaxCapacity());
        
        // Hourly averages
        List<Object[]> hourlyData = roomUsageRepository.getHourlyAverages(room, startDate);
        Map<Integer, Double> hourlyAverages = new HashMap<>();
        for (Object[] data : hourlyData) {
            hourlyAverages.put((Integer) data[0], (Double) data[1]);
        }
        analytics.put("hourlyAverages", hourlyAverages);
        
        // Daily averages
        List<Object[]> dailyData = roomUsageRepository.getDailyAverages(room, startDate);
        Map<Integer, Double> dailyAverages = new HashMap<>();
        for (Object[] data : dailyData) {
            dailyAverages.put((Integer) data[0], (Double) data[1]);
        }
        analytics.put("dailyAverages", dailyAverages);
        
        // Daily stats (for charts)
        List<Object[]> dailyStats = roomUsageRepository.getDailyStats(room, startDate);
        analytics.put("dailyStats", dailyStats);
        
        return analytics;
    }
    
    /**
     * Get overall analytics across all rooms
     */
    public Map<String, Object> getOverallAnalytics(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        Map<String, Object> analytics = new HashMap<>();
        
        Long activeRooms = roomUsageRepository.countActiveRooms(startDate);
        Long totalParticipantMinutes = roomUsageRepository.getTotalParticipantMinutes(startDate);
        
        List<StudyRoom> allRooms = studyRoomRepository.findByIsActiveTrueOrderByPriorityDesc();
        int totalCapacity = allRooms.stream().mapToInt(StudyRoom::getMaxCapacity).sum();
        int currentParticipants = allRooms.stream().mapToInt(StudyRoom::getCurrentParticipants).sum();
        
        analytics.put("activeRooms", activeRooms);
        analytics.put("totalRooms", allRooms.size());
        analytics.put("totalCapacity", totalCapacity);
        analytics.put("currentParticipants", currentParticipants);
        analytics.put("totalParticipantMinutes", totalParticipantMinutes != null ? totalParticipantMinutes : 0);
        analytics.put("utilizationPercent", totalCapacity > 0 ? (currentParticipants * 100.0 / totalCapacity) : 0);
        
        // Platform breakdown
        Map<String, Integer> platformCounts = new HashMap<>();
        Map<String, Integer> platformParticipants = new HashMap<>();
        for (StudyRoom room : allRooms) {
            String platform = room.getPlatform() != null ? room.getPlatform() : "other";
            platformCounts.put(platform, platformCounts.getOrDefault(platform, 0) + 1);
            platformParticipants.put(platform, 
                platformParticipants.getOrDefault(platform, 0) + room.getCurrentParticipants());
        }
        analytics.put("platformCounts", platformCounts);
        analytics.put("platformParticipants", platformParticipants);
        
        return analytics;
    }
    
    /**
     * Get all active rooms
     */
    public List<StudyRoom> getAllActiveRooms(String roomType) {
        if (roomType != null && !roomType.isEmpty()) {
            return studyRoomRepository.findByRoomTypeAndIsActiveTrueOrderByPriorityDesc(roomType);
        }
        return studyRoomRepository.findByIsActiveTrueOrderByPriorityDesc();
    }
    
    /**
     * Delete a study room (admin only)
     */
    @Transactional
    public void deleteRoom(Long roomId) {
        Optional<StudyRoom> roomOpt = studyRoomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            throw new RuntimeException("Study room not found with id: " + roomId);
        }
        
        StudyRoom room = roomOpt.get();
        
        // Delete all sessions for this room
        List<StudyRoomSession> sessions = studyRoomSessionRepository.findByRoomAndIsConfirmedTrue(room);
        studyRoomSessionRepository.deleteAll(sessions);
        
        // Delete the room
        studyRoomRepository.delete(room);
    }
    
    /**
     * Release a room slot when user leaves
     */
    @Transactional
    public void releaseRoomSlot(Long roomId, Long userId) {
        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        StudyRoom room = studyRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        
        // Find confirmed session
        List<StudyRoomSession> sessions = studyRoomSessionRepository.findByRoomAndIsConfirmedTrue(room);
        StudyRoomSession userSession = sessions.stream()
                .filter(s -> s.getUser().getId().equals(userId))
                .findFirst()
                .orElse(null);
        
        if (userSession != null) {
            // Delete the session
            studyRoomSessionRepository.delete(userSession);
            
            // Decrement participant count
            room.decrementParticipants();
            studyRoomRepository.save(room);
        }
    }
    
    /**
     * Clean up expired unconfirmed sessions and auto-confirm active sessions
     * Runs every 3 minutes (optimized for scale: 500K-1M users):
     * - Auto-confirms sessions older than 2 minutes (user likely joined but forgot to confirm)
     * - Deletes sessions older than 5 minutes that are still unconfirmed (user likely didn't join)
     * 
     * OPTIMIZED: Uses database queries instead of loading all sessions into memory
     */
    @Scheduled(fixedRate = 180000) // Every 3 minutes (optimized for scale: 500K-1M users)
    @Transactional
    public void cleanupExpiredSessions() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime twoMinutesAgo = now.minusMinutes(2);
            LocalDateTime fiveMinutesAgo = now.minusMinutes(5);
            
            // OPTIMIZED: Query only unconfirmed sessions (database-level filtering)
            List<StudyRoomSession> unconfirmedSessions = studyRoomSessionRepository.findAllUnconfirmedSessions();
            
            // Batch process: collect rooms to update
            Map<StudyRoom, Integer> roomDecrementMap = new HashMap<>();
            List<StudyRoomSession> toDelete = new java.util.ArrayList<>();
            List<StudyRoomSession> toConfirm = new java.util.ArrayList<>();
            
            for (StudyRoomSession session : unconfirmedSessions) {
                StudyRoom room = session.getRoom();
                LocalDateTime linkOpenedAt = session.getLinkOpenedAt();
                
                // If session is between 2-5 minutes old, auto-confirm
                if (linkOpenedAt.isBefore(twoMinutesAgo) && linkOpenedAt.isAfter(fiveMinutesAgo)) {
                    if (room.getCurrentParticipants() <= room.getMaxCapacity()) {
                        toConfirm.add(session);
                    }
                } 
                // If session is older than 5 minutes and still unconfirmed, delete it
                else if (linkOpenedAt.isBefore(fiveMinutesAgo)) {
                    if (room.getCurrentParticipants() > 0) {
                        roomDecrementMap.put(room, roomDecrementMap.getOrDefault(room, 0) + 1);
                    }
                    toDelete.add(session);
                }
            }
            
            // Batch update: confirm sessions
            for (StudyRoomSession session : toConfirm) {
                session.setIsConfirmed(true);
                session.setConfirmedAt(now);
                studyRoomSessionRepository.save(session);
            }
            
            // Batch update: decrement room counts
            for (Map.Entry<StudyRoom, Integer> entry : roomDecrementMap.entrySet()) {
                StudyRoom room = entry.getKey();
                int decrementCount = entry.getValue();
                for (int i = 0; i < decrementCount; i++) {
                    room.decrementParticipants();
                }
                studyRoomRepository.save(room);
            }
            
            // Batch delete: remove expired sessions
            if (!toDelete.isEmpty()) {
                studyRoomSessionRepository.deleteAll(toDelete);
            }
        } catch (Exception e) {
            // Log error but don't fail the scheduled task
            System.err.println("Error in cleanupExpiredSessions: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cancel join - user didn't actually join, remove the session and decrement count
     */
    @Transactional
    public void cancelJoin(Long roomId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        StudyRoom room = studyRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        
        // Find unconfirmed session
        Optional<StudyRoomSession> sessionOpt = studyRoomSessionRepository
                .findByUserAndRoomAndIsConfirmedFalse(user, room);
        
        if (sessionOpt.isPresent()) {
            StudyRoomSession session = sessionOpt.get();
            
            // Decrement participant count
            if (room.getCurrentParticipants() > 0) {
                room.decrementParticipants();
                studyRoomRepository.save(room);
            }
            
            // Delete the session
            studyRoomSessionRepository.delete(session);
        }
    }
    
    /**
     * Sync participant counts with confirmed sessions
     * This ensures accuracy if there are any discrepancies
     */
    @Transactional
    public void syncParticipantCounts() {
        List<StudyRoom> allRooms = studyRoomRepository.findAll();
        for (StudyRoom room : allRooms) {
            Long confirmedCount = studyRoomSessionRepository.countConfirmedSessionsByRoom(room);
            room.setCurrentParticipants(confirmedCount.intValue());
            studyRoomRepository.save(room);
        }
    }
}

