package com.example.jewell.controller;

import com.example.jewell.service.LiveRatesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/live-rates")
public class LiveRatesController {
    
    @Autowired
    private LiveRatesService liveRatesService;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getLiveRates() {
        return ResponseEntity.ok(liveRatesService.getLiveRates());
    }
}
