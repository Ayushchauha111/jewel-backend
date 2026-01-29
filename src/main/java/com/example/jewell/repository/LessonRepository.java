package com.example.jewell.repository;

import com.example.jewell.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByWorldIdAndIsActiveTrueOrderByDisplayOrderAsc(Long worldId);
    
    @Query("SELECT l FROM Lesson l WHERE l.world.id = :worldId AND l.isActive = true ORDER BY l.lessonNumber ASC")
    List<Lesson> findActiveLessonsByWorldId(@Param("worldId") Long worldId);
}
