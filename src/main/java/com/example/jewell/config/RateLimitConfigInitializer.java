package com.example.jewell.config;

import com.example.jewell.service.RateLimitConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initialize default rate limit configurations on application startup
 */
@Component
public class RateLimitConfigInitializer implements CommandLineRunner {
    
    @Autowired
    private RateLimitConfigService rateLimitConfigService;
    
    @Override
    public void run(String... args) throws Exception {
        rateLimitConfigService.initializeDefaults();
        System.out.println("Rate limit configurations initialized with defaults");
    }
}


