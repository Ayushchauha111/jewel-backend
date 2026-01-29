package com.example.jewell.service;

import com.example.jewell.model.ForestSession;
import com.example.jewell.model.User;
import com.example.jewell.repository.ForestSessionRepository;
import com.example.jewell.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ForestService {
    
    @Autowired
    private ForestSessionRepository forestSessionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public ForestSession addFocusSession(Long userId, Integer focusMinutes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        LocalDate today = LocalDate.now();
        Optional<ForestSession> existing = forestSessionRepository.findByUserIdAndSessionDate(userId, today);
        
        ForestSession session;
        if (existing.isPresent()) {
            session = existing.get();
            session.setTreesPlanted(session.getTreesPlanted() + 1);
            session.setTotalFocusMinutes(session.getTotalFocusMinutes() + focusMinutes);
            session.setSessionsCount(session.getSessionsCount() + 1);
            if (focusMinutes > session.getLongestSessionMinutes()) {
                session.setLongestSessionMinutes(focusMinutes);
            }
        } else {
            session = ForestSession.builder()
                    .user(user)
                    .sessionDate(today)
                    .treesPlanted(1)
                    .totalFocusMinutes(focusMinutes)
                    .longestSessionMinutes(focusMinutes)
                    .sessionsCount(1)
                    .build();
        }
        
        return forestSessionRepository.save(session);
    }
    
    public Optional<ForestSession> getTodaySession(Long userId) {
        return forestSessionRepository.findByUserIdAndSessionDate(userId, LocalDate.now());
    }
    
    public List<ForestSession> getUserHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return forestSessionRepository.findByUserOrderBySessionDateDesc(user);
    }
    
    public List<ForestSession> getDailyLeaderboard() {
        return forestSessionRepository.findDailyLeaderboard(LocalDate.now());
    }
    
    public Long getTotalTrees(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        Long total = forestSessionRepository.getTotalTreesByUser(user);
        return total != null ? total : 0L;
    }
    
    public Long getTotalFocusMinutes(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        Long total = forestSessionRepository.getTotalFocusMinutesByUser(user);
        return total != null ? total : 0L;
    }
    
    public List<ForestSession> getDailyLeaderboardByDate(LocalDate date) {
        return forestSessionRepository.findDailyLeaderboard(date);
    }
    
    public List<Map<String, Object>> getAvailableDates() {
        List<LocalDate> dates = forestSessionRepository.findDistinctSessionDates();
        return dates.stream().map(date -> {
            Map<String, Object> dateInfo = new HashMap<>();
            dateInfo.put("date", date.toString());
            Long participants = forestSessionRepository.countParticipantsByDate(date);
            dateInfo.put("totalParticipants", participants != null ? participants : 0L);
            
            // Get top ranking for this date
            List<ForestSession> topSessions = forestSessionRepository.findDailyLeaderboard(date);
            if (!topSessions.isEmpty()) {
                ForestSession top = topSessions.get(0);
                Map<String, Object> topRanking = new HashMap<>();
                topRanking.put("rank", 1);
                topRanking.put("fullName", top.getUser().getUsername());
                topRanking.put("totalDurationFormatted", formatDuration(top.getTotalFocusMinutes()));
                dateInfo.put("topRanking", topRanking);
            }
            
            dateInfo.put("computedAt", java.time.LocalDateTime.now().toString());
            return dateInfo;
        }).collect(Collectors.toList());
    }
    
    private String formatDuration(Integer minutes) {
        if (minutes == null || minutes == 0) {
            return "0 min";
        }
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours > 0) {
            return hours + " hr " + mins + " min";
        }
        return mins + " min";
    }
}

