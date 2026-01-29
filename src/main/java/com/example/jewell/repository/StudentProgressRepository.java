package com.example.jewell.repository;

import com.example.jewell.model.StudentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentProgressRepository extends JpaRepository<StudentProgress, Long> {
    Optional<StudentProgress> findByUserIdAndLessonId(Long userId, Long lessonId);
    
    List<StudentProgress> findByUserId(Long userId);
    
    List<StudentProgress> findByLessonId(Long lessonId);
    
    @Query("SELECT sp FROM StudentProgress sp WHERE sp.user.id = :userId AND sp.lesson.world.id = :worldId")
    List<StudentProgress> findByUserIdAndWorldId(@Param("userId") Long userId, @Param("worldId") Long worldId);
    
    @Query("SELECT COUNT(sp) FROM StudentProgress sp WHERE sp.user.id = :userId AND sp.isCompleted = true")
    Long countCompletedLessonsByUserId(@Param("userId") Long userId);
}
