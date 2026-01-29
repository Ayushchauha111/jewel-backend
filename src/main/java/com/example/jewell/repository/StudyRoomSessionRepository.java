package com.example.jewell.repository;

import com.example.jewell.model.StudyRoom;
import com.example.jewell.model.StudyRoomSession;
import com.example.jewell.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudyRoomSessionRepository extends JpaRepository<StudyRoomSession, Long> {
    
    Optional<StudyRoomSession> findByUserAndRoomAndIsConfirmedFalse(User user, StudyRoom room);
    
    List<StudyRoomSession> findByRoomAndIsConfirmedTrue(StudyRoom room);
    
    List<StudyRoomSession> findByUserAndIsConfirmedTrue(User user);
    
    @Query("SELECT s FROM StudyRoomSession s WHERE s.expiresAt < :now AND s.isConfirmed = false")
    List<StudyRoomSession> findExpiredUnconfirmedSessions(@Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM StudyRoomSession s WHERE s.isConfirmed = false")
    List<StudyRoomSession> findAllUnconfirmedSessions();
    
    @Modifying
    @Query("DELETE FROM StudyRoomSession s WHERE s.expiresAt < :now AND s.isConfirmed = false")
    void deleteExpiredUnconfirmedSessions(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(s) FROM StudyRoomSession s WHERE s.room = :room AND s.isConfirmed = true")
    Long countConfirmedSessionsByRoom(@Param("room") StudyRoom room);
    
    @Query("SELECT s FROM StudyRoomSession s WHERE s.room = :room AND s.user = :user")
    Optional<StudyRoomSession> findByRoomAndUser(@Param("room") StudyRoom room, @Param("user") User user);
}

