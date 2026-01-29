package com.example.jewell.repository;

import com.example.jewell.model.CurriculumLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurriculumLessonRepository extends JpaRepository<CurriculumLesson, Long> {
    List<CurriculumLesson> findByUnitIdAndIsActiveTrueOrderByDisplayOrderAsc(Long unitId);
    
    Optional<CurriculumLesson> findByLessonIdAndUnitId(Long lessonId, Long unitId);
    
    @Query("SELECT l FROM CurriculumLesson l WHERE l.unit.id = :unitId AND l.isActive = true ORDER BY l.displayOrder ASC")
    List<CurriculumLesson> findActiveLessonsByUnitId(@Param("unitId") Long unitId);
    
    List<CurriculumLesson> findByUnitId(Long unitId);
}
