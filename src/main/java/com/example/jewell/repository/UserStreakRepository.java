package com.example.jewell.repository;

import com.example.jewell.model.User;
import com.example.jewell.model.UserStreak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {
    
    Optional<UserStreak> findByUser(User user);
    
    Optional<UserStreak> findByUserId(Long userId);
    
    // Top streakers leaderboard - with pagination
    Page<UserStreak> findByOrderByCurrentStreakDesc(Pageable pageable);
    
    // Top XP leaderboard - with pagination
    Page<UserStreak> findByOrderByTotalXpDesc(Pageable pageable);
    
    // Top level leaderboard - with pagination
    Page<UserStreak> findByOrderByLevelDesc(Pageable pageable);
    
    // Legacy methods for backward compatibility (used in overall stats)
    List<UserStreak> findTop10ByOrderByCurrentStreakDesc();
    List<UserStreak> findTop10ByOrderByTotalXpDesc();
    List<UserStreak> findTop10ByOrderByLevelDesc();
    
    @Query("SELECT us FROM UserStreak us WHERE us.currentStreak > 0 ORDER BY us.currentStreak DESC")
    List<UserStreak> findActiveStreaks();
}

