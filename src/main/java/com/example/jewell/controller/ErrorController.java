package com.example.jewell.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller to handle requests that don't match the /api prefix
 */
@RestController
public class ErrorController {

    @RequestMapping("/customers")
    public ResponseEntity<Map<String, String>> handleCustomersWithoutApi() {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid endpoint");
        error.put("message", "Please use /api/customers instead of /customers");
        error.put("correctUrl", "/api/customers");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
