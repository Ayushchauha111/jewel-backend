package com.example.jewell.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.jewell.dto.ApiResponseDTO;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponseDTO<Object>> healthCheck() {
        try {
            String status = "System is up";
            return ResponseEntity.ok(new ApiResponseDTO<>(true, status, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO<>(false, "An error occurred during health check.", null));
        }
    }
}