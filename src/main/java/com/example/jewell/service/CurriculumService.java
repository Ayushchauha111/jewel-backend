package com.example.jewell.service;

import com.example.jewell.model.*;
import com.example.jewell.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CurriculumService {
    
    private static final Logger logger = LoggerFactory.getLogger(CurriculumService.class);
    
    @Autowired
    private CurriculumProductRepository productRepository;
    
    @Autowired
    private CurriculumUnitRepository unitRepository;
    
    @Autowired
    private CurriculumLessonRepository lessonRepository;
    
    @Autowired
    private CurriculumScreenRepository screenRepository;
    
    // Product Methods
    public List<CurriculumProduct> getAllActiveProducts() {
        return productRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }
    
    public Optional<CurriculumProduct> getProductByProductId(String productId) {
        return productRepository.findByProductId(productId);
    }
    
    // Unit Methods
    @Transactional(readOnly = true)
    public List<CurriculumUnit> getUnitsByProductId(String productId) {
        List<CurriculumUnit> units = unitRepository.findActiveUnitsWithLessonsByProductId(productId);
        for (CurriculumUnit unit : units) {
            if (unit.getLessons() != null) {
                // Filter and sort lessons
                unit.getLessons().removeIf(lesson -> lesson.getIsActive() == null || !lesson.getIsActive());
                unit.getLessons().sort((l1, l2) -> {
                    int order1 = l1.getDisplayOrder() != null ? l1.getDisplayOrder() : 0;
                    int order2 = l2.getDisplayOrder() != null ? l2.getDisplayOrder() : 0;
                    return Integer.compare(order1, order2);
                });
            } else {
                // Explicitly load lessons if not already loaded
                List<CurriculumLesson> lessons = lessonRepository.findActiveLessonsByUnitId(unit.getId());
                unit.setLessons(lessons);
                logger.debug("Explicitly loaded {} lessons for unit {}", lessons.size(), unit.getId());
            }
        }
        return units;
    }
    
    public Optional<CurriculumUnit> getUnitByUnitIdAndProductId(Long unitId, String productId) {
        return unitRepository.findByUnitIdAndProductProductId(unitId, productId);
    }
    
    // Lesson Methods
    @Transactional(readOnly = true)
    public List<CurriculumLesson> getLessonsByUnitId(Long unitId) {
        return lessonRepository.findActiveLessonsByUnitId(unitId);
    }
    
    public Optional<CurriculumLesson> getLessonByLessonIdAndUnitId(Long lessonId, Long unitId) {
        return lessonRepository.findByLessonIdAndUnitId(lessonId, unitId);
    }
    
    // Screen Methods
    @Transactional(readOnly = true)
    public List<CurriculumScreen> getScreensByLessonId(Long lessonId) {
        return screenRepository.findActiveScreensByLessonId(lessonId);
    }
}
