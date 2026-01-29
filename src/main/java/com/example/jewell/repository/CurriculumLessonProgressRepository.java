package com.example.jewell.repository;

import com.example.jewell.model.CurriculumLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurriculumLessonProgressRepository extends JpaRepository<CurriculumLessonProgress, Long> {
    Optional<CurriculumLessonProgress> findByUserIdAndCurriculumLessonId(Long userId, Long lessonId);
    
    List<CurriculumLessonProgress> findByUserId(Long userId);
    
    List<CurriculumLessonProgress> findByCurriculumLessonId(Long lessonId);
    
    @Query("SELECT clp FROM CurriculumLessonProgress clp WHERE clp.user.id = :userId AND clp.curriculumLesson.unit.id = :unitId")
    List<CurriculumLessonProgress> findByUserIdAndUnitId(@Param("userId") Long userId, @Param("unitId") Long unitId);
    
    @Query("SELECT COUNT(clp) FROM CurriculumLessonProgress clp WHERE clp.user.id = :userId AND clp.isCompleted = true")
    Long countCompletedLessonsByUserId(@Param("userId") Long userId);
}
