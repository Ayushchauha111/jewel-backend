package com.example.jewell.controller;

import com.example.jewell.dto.ApiResponseDTO;
import com.example.jewell.model.*;
import com.example.jewell.service.CurriculumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/curriculum")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CurriculumController {
    
    @Autowired
    private CurriculumService curriculumService;
    
    // Product Endpoints
    @GetMapping("/products")
    public ResponseEntity<ApiResponseDTO<List<CurriculumProduct>>> getAllProducts() {
        try {
            List<CurriculumProduct> products = curriculumService.getAllActiveProducts();
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Products retrieved successfully", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving products: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponseDTO<CurriculumProduct>> getProductByProductId(@PathVariable String productId) {
        try {
            Optional<CurriculumProduct> product = curriculumService.getProductByProductId(productId);
            if (product.isPresent()) {
                return ResponseEntity.ok(new ApiResponseDTO<>(true, "Product retrieved successfully", product.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponseDTO<>(false, "Product not found", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving product: " + e.getMessage(), null));
        }
    }
    
    // Unit Endpoints
    @GetMapping("/products/{productId}/units")
    public ResponseEntity<ApiResponseDTO<List<CurriculumUnit>>> getUnitsByProductId(@PathVariable String productId) {
        try {
            List<CurriculumUnit> units = curriculumService.getUnitsByProductId(productId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Units retrieved successfully", units));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving units: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/products/{productId}/units/{unitId}")
    public ResponseEntity<ApiResponseDTO<CurriculumUnit>> getUnitByUnitId(
            @PathVariable String productId, @PathVariable Long unitId) {
        try {
            Optional<CurriculumUnit> unit = curriculumService.getUnitByUnitIdAndProductId(unitId, productId);
            if (unit.isPresent()) {
                return ResponseEntity.ok(new ApiResponseDTO<>(true, "Unit retrieved successfully", unit.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponseDTO<>(false, "Unit not found", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving unit: " + e.getMessage(), null));
        }
    }
    
    // Lesson Endpoints
    @GetMapping("/units/{unitId}/lessons")
    public ResponseEntity<ApiResponseDTO<List<CurriculumLesson>>> getLessonsByUnitId(@PathVariable Long unitId) {
        try {
            List<CurriculumLesson> lessons = curriculumService.getLessonsByUnitId(unitId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Lessons retrieved successfully", lessons));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving lessons: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/units/{unitId}/lessons/{lessonId}")
    public ResponseEntity<ApiResponseDTO<CurriculumLesson>> getLessonByLessonId(
            @PathVariable Long unitId, @PathVariable Long lessonId) {
        try {
            Optional<CurriculumLesson> lesson = curriculumService.getLessonByLessonIdAndUnitId(lessonId, unitId);
            if (lesson.isPresent()) {
                return ResponseEntity.ok(new ApiResponseDTO<>(true, "Lesson retrieved successfully", lesson.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponseDTO<>(false, "Lesson not found", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving lesson: " + e.getMessage(), null));
        }
    }
    
    // Screen Endpoints
    @GetMapping("/lessons/{lessonId}/screens")
    public ResponseEntity<ApiResponseDTO<List<CurriculumScreen>>> getScreensByLessonId(@PathVariable Long lessonId) {
        try {
            List<CurriculumScreen> screens = curriculumService.getScreensByLessonId(lessonId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Screens retrieved successfully", screens));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving screens: " + e.getMessage(), null));
        }
    }
}
