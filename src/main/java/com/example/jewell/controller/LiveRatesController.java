package com.example.jewell.controller;

import com.example.jewell.service.LiveRatesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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

    /**
     * Proxy for external streaming URL. Frontend calls this instead of the external URL to avoid CORS on prod (e.g. gangajewellers.in).
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getStream() {
        String body = liveRatesService.fetchExternalStream();
        if (body != null) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.noContent().build();
    }
}
