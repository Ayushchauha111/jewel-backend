package com.example.jewell.repository;

import com.example.jewell.model.StudyRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyRoomRepository extends JpaRepository<StudyRoom, Long> {
    
    List<StudyRoom> findByIsActiveTrueOrderByPriorityDesc();
    
    List<StudyRoom> findByRoomTypeAndIsActiveTrueOrderByPriorityDesc(String roomType);
    
    @Query("SELECT sr FROM StudyRoom sr WHERE sr.isActive = true AND sr.roomType = ?1 AND sr.currentParticipants < sr.maxCapacity ORDER BY sr.currentParticipants ASC, sr.priority DESC")
    List<StudyRoom> findAvailableRoomsByType(String roomType);
    
    @Query("SELECT sr FROM StudyRoom sr WHERE sr.isActive = true AND sr.currentParticipants < sr.maxCapacity ORDER BY sr.currentParticipants ASC, sr.priority DESC")
    List<StudyRoom> findAvailableRooms();
    
    Optional<StudyRoom> findByRoomName(String roomName);
}

