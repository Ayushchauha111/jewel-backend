package com.example.jewell.repository;

import com.example.jewell.model.DailyGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyGoalRepository extends JpaRepository<DailyGoal, Long> {
    Optional<DailyGoal> findByUserIdAndGoalDate(Long userId, LocalDate goalDate);

    @Query("SELECT dg FROM DailyGoal dg WHERE dg.user.id = :userId AND dg.goalDate = :goalDate")
    Optional<DailyGoal> findTodayGoal(@Param("userId") Long userId, @Param("goalDate") LocalDate goalDate);

    @Query("SELECT dg FROM DailyGoal dg WHERE dg.user.id = :userId ORDER BY dg.goalDate DESC")
    Optional<DailyGoal> findLatestGoalByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(dg.todayTimeSpentSeconds) FROM DailyGoal dg WHERE dg.user.id = :userId AND dg.goalDate = :goalDate")
    Optional<Integer> findTotalTimeSpentToday(@Param("userId") Long userId, @Param("goalDate") LocalDate goalDate);
}
