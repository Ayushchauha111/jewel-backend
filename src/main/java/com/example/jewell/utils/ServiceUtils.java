package com.example.jewell.utils;

import java.time.LocalDateTime;
import java.time.Period;

import org.springframework.security.core.Authentication;

import com.example.jewell.security.services.UserDetailsImpl;

public class ServiceUtils {

    public static Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        // Assuming the principal is a custom UserDetails implementation
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getId(); // Replace with the actual method to get the user ID
    }

    /**
     * Check if the authenticated user has the ROLE_ADMIN role
     */
    public static boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Check if the authenticated user has a specific role
     */
    public static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }

    public static LocalDateTime calculateExpiresAt(LocalDateTime enrolledAt, String validity) {
        if (enrolledAt == null) {
            throw new IllegalArgumentException("enrolledAt cannot be null");
        }
        if (validity == null || validity.trim().isEmpty()) {
            throw new IllegalArgumentException("validity cannot be null or empty");
        }
        
        // Parse the validity string to extract years, months, and days
        Period period = parseValidity(validity);

        // Add the period to the enrolledAt timestamp
        LocalDateTime expiresAt = enrolledAt.plus(period);
        
        return expiresAt;
    }

    private static Period parseValidity(String validity) {
        if (validity == null || validity.trim().isEmpty()) {
            return Period.ZERO;
        }
        
        // Normalize the validity string - remove extra spaces and convert to lowercase
        String normalized = validity.trim().toLowerCase();
        
        // Handle formats like "2Days", "3Months" (no space)
        normalized = normalized.replaceAll("(\\d+)([a-z]+)", "$1 $2");
        
        // Split into parts
        String[] parts = normalized.split("\\s+");

        int years = 0;
        int months = 0;
        int days = 0;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;
            
            if (part.matches("\\d+")) {
                int value = Integer.parseInt(part);
                // Check current part and next part for unit
                if (i + 1 < parts.length) {
                    String unit = parts[i + 1].toLowerCase().trim();
                    if (unit.startsWith("year") || unit.equals("y") || unit.equals("yr") || unit.equals("yrs")) {
                        years = value;
                    } else if (unit.startsWith("month") || unit.equals("m") || unit.equals("mo") || unit.equals("mos")) {
                        months = value;
                    } else if (unit.startsWith("day") || unit.equals("d")) {
                        days = value;
                    }
                }
            } else if (part.matches("\\d+[a-z]+")) {
                // Handle formats like "2days", "3months" (combined)
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)([a-z]+)");
                java.util.regex.Matcher matcher = pattern.matcher(part);
                if (matcher.find()) {
                    int value = Integer.parseInt(matcher.group(1));
                    String unit = matcher.group(2).toLowerCase();
                    if (unit.startsWith("year") || unit.equals("y") || unit.equals("yr") || unit.equals("yrs")) {
                        years = value;
                    } else if (unit.startsWith("month") || unit.equals("m") || unit.equals("mo") || unit.equals("mos")) {
                        months = value;
                    } else if (unit.startsWith("day") || unit.equals("d")) {
                        days = value;
                    }
                }
            }
        }

        return Period.of(years, months, days);
    }
}
