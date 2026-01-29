package com.example.jewell.repository;

import com.example.jewell.model.CurriculumScreen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurriculumScreenRepository extends JpaRepository<CurriculumScreen, Long> {
    List<CurriculumScreen> findByLessonIdAndIsActiveTrueOrderByScreenIndexAsc(Long lessonId);
    
    @Query("SELECT s FROM CurriculumScreen s WHERE s.lesson.id = :lessonId AND s.isActive = true ORDER BY s.screenIndex ASC")
    List<CurriculumScreen> findActiveScreensByLessonId(@Param("lessonId") Long lessonId);
    
    List<CurriculumScreen> findByLessonId(Long lessonId);
}
