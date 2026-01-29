package com.example.jewell.repository;

import com.example.jewell.model.RoomUsage;
import com.example.jewell.model.StudyRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoomUsageRepository extends JpaRepository<RoomUsage, Long> {
    
    List<RoomUsage> findByRoomOrderByRecordedAtDesc(StudyRoom room);
    
    @Query("SELECT ru FROM RoomUsage ru WHERE ru.room = :room AND ru.recordedAt >= :startDate AND ru.recordedAt <= :endDate ORDER BY ru.recordedAt ASC")
    List<RoomUsage> findByRoomAndDateRange(@Param("room") StudyRoom room, 
                                           @Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT AVG(ru.participantCount) FROM RoomUsage ru WHERE ru.room = :room AND ru.recordedAt >= :startDate")
    Double getAverageParticipants(@Param("room") StudyRoom room, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT MAX(ru.participantCount) FROM RoomUsage ru WHERE ru.room = :room AND ru.recordedAt >= :startDate")
    Integer getMaxParticipants(@Param("room") StudyRoom room, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT ru.hourOfDay, AVG(ru.participantCount) FROM RoomUsage ru WHERE ru.room = :room AND ru.recordedAt >= :startDate GROUP BY ru.hourOfDay ORDER BY ru.hourOfDay")
    List<Object[]> getHourlyAverages(@Param("room") StudyRoom room, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT ru.dayOfWeek, AVG(ru.participantCount) FROM RoomUsage ru WHERE ru.room = :room AND ru.recordedAt >= :startDate GROUP BY ru.dayOfWeek ORDER BY ru.dayOfWeek")
    List<Object[]> getDailyAverages(@Param("room") StudyRoom room, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT DATE(ru.recordedAt), AVG(ru.participantCount), MAX(ru.participantCount) FROM RoomUsage ru WHERE ru.room = :room AND ru.recordedAt >= :startDate GROUP BY DATE(ru.recordedAt) ORDER BY DATE(ru.recordedAt)")
    List<Object[]> getDailyStats(@Param("room") StudyRoom room, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(DISTINCT ru.room) FROM RoomUsage ru WHERE ru.recordedAt >= :startDate")
    Long countActiveRooms(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT SUM(ru.participantCount) FROM RoomUsage ru WHERE ru.recordedAt >= :startDate")
    Long getTotalParticipantMinutes(@Param("startDate") LocalDateTime startDate);
}

