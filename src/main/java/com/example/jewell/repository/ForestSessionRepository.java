package com.example.jewell.repository;

import com.example.jewell.model.ForestSession;
import com.example.jewell.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ForestSessionRepository extends JpaRepository<ForestSession, Long> {
    Optional<ForestSession> findByUserAndSessionDate(User user, LocalDate date);
    
    Optional<ForestSession> findByUserIdAndSessionDate(Long userId, LocalDate date);
    
    List<ForestSession> findByUserOrderBySessionDateDesc(User user);
    
    List<ForestSession> findBySessionDateOrderByTreesPlantedDesc(LocalDate date);
    
    @Query("SELECT fs FROM ForestSession fs WHERE fs.sessionDate = ?1 ORDER BY fs.treesPlanted DESC, fs.totalFocusMinutes DESC")
    List<ForestSession> findDailyLeaderboard(LocalDate date);
    
    @Query("SELECT SUM(fs.treesPlanted) FROM ForestSession fs WHERE fs.user = ?1")
    Long getTotalTreesByUser(User user);
    
    @Query("SELECT SUM(fs.totalFocusMinutes) FROM ForestSession fs WHERE fs.user = ?1")
    Long getTotalFocusMinutesByUser(User user);
    
    @Query("SELECT DISTINCT fs.sessionDate FROM ForestSession fs ORDER BY fs.sessionDate DESC")
    List<LocalDate> findDistinctSessionDates();
    
    @Query("SELECT COUNT(DISTINCT fs.user) FROM ForestSession fs WHERE fs.sessionDate = ?1")
    Long countParticipantsByDate(LocalDate date);
}

