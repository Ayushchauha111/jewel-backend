package com.example.jewell.repository;

import com.example.jewell.model.CurriculumUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurriculumUnitRepository extends JpaRepository<CurriculumUnit, Long> {
    List<CurriculumUnit> findByProductProductIdAndIsActiveTrueOrderByDisplayOrderAsc(String productId);
    
    @Query("SELECT DISTINCT u FROM CurriculumUnit u LEFT JOIN FETCH u.lessons l WHERE u.product.productId = :productId AND u.isActive = true AND (l.isActive = true OR l IS NULL) ORDER BY u.displayOrder ASC, l.displayOrder ASC")
    List<CurriculumUnit> findActiveUnitsWithLessonsByProductId(@Param("productId") String productId);
    
    Optional<CurriculumUnit> findByUnitIdAndProductProductId(Long unitId, String productId);
    
    List<CurriculumUnit> findByProductProductId(String productId);
}
