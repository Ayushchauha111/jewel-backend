package com.example.jewell.controller;

import com.example.jewell.exception.FeatureDisabledException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.jewell.model.User;
import com.example.jewell.security.services.UserDetailsImpl;
import com.example.jewell.service.FeatureFlagService;
import com.example.jewell.service.ReferralService;

import java.util.Map;

@RestController
@RequestMapping("/api/referral")
public class ReferralController {
  @Autowired
  private ReferralService referralService;

  @Autowired
  private FeatureFlagService featureFlagService;

  private void checkReferralSystemEnabled() {
    if (!featureFlagService.isFeatureEnabled("referral_system")) {
      throw new FeatureDisabledException("Referral system feature is currently disabled");
    }
  }

  @GetMapping("/generate")
  public ResponseEntity<?> generateReferralLink(Authentication authentication) {
    try {
      checkReferralSystemEnabled();
      Long userId = getUserIdFromAuthentication(authentication);
      return ResponseEntity.ok(Map.of("referralLink", referralService.generateReferralLink(userId)));
    } catch (FeatureDisabledException e) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
              .body(Map.of("success", false, "message", e.getMessage()));
    }
  }

  @PostMapping("/track")
  public String trackReferral(@RequestParam String referralCode, @RequestParam String newUserId) {
    return referralService.trackReferral(referralCode, newUserId);
  }

  @GetMapping("/stats/{userId}")
  public User getReferralStats(@PathVariable Long userId) {
    return referralService.getReferralStats(userId);
  }

  private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        // Assuming the principal is a custom UserDetails implementation
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getId(); // Replace with the actual method to get the user ID
    }
}