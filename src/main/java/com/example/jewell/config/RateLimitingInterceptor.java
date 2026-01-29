package com.example.jewell.config;

import com.example.jewell.service.RateLimitConfigService;
import com.example.jewell.service.UserActivityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.jewell.security.services.UserDetailsImpl;

/**
 * Simple in-memory rate limiting interceptor to prevent DDoS and brute force attacks
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(RateLimitingInterceptor.class);

    @Autowired
    private RateLimitConfigService rateLimitConfigService;

    @Autowired(required = false)
    private UserActivityService userActivityService;
    
    // Default values (used as fallback)
    private static final int DEFAULT_AUTH_REQUESTS_PER_MINUTE = 5;
    private static final int DEFAULT_GENERAL_REQUESTS_PER_MINUTE = 100;
    private static final int DEFAULT_PUBLIC_REQUESTS_PER_MINUTE = 200;
    private static final long DEFAULT_WINDOW_SIZE_MS = 60_000; // 1 minute
    
    // Cache config values to avoid DB lookups on every request
    private volatile int cachedAuthRequestsPerMinute = DEFAULT_AUTH_REQUESTS_PER_MINUTE;
    private volatile int cachedGeneralRequestsPerMinute = DEFAULT_GENERAL_REQUESTS_PER_MINUTE;
    private volatile int cachedPublicRequestsPerMinute = DEFAULT_PUBLIC_REQUESTS_PER_MINUTE;
    private volatile long cachedWindowSizeMs = DEFAULT_WINDOW_SIZE_MS;
    private volatile long lastConfigRefresh = 0;
    private static final long CONFIG_REFRESH_INTERVAL_MS = 30_000; // Refresh every 30 seconds
    
    // Store request counts per IP address with timestamps
    static class RequestRecord {
        int count;
        long windowStart;
    }
    
    private final Map<String, RequestRecord> requestCounts = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Refresh config cache periodically
        refreshConfigCache();

        // Track "active users" for admin metric (authenticated only; throttled in service)
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && userActivityService != null) {
                Object principal = auth.getPrincipal();
                if (principal instanceof UserDetailsImpl) {
                    userActivityService.touch(((UserDetailsImpl) principal).getId());
                }
            }
        } catch (Exception ignored) {
            // best-effort only
        }
        
        String clientIp = getClientIpAddress(request);
        String path = request.getRequestURI();
        
        // Determine rate limit based on endpoint type
        int requestsPerMinute = getRateLimitForPath(path);
        
        RequestRecord record = requestCounts.computeIfAbsent(clientIp, k -> new RequestRecord());
        long currentTime = System.currentTimeMillis();
        
        // Reset window if expired
        if (currentTime - record.windowStart > cachedWindowSizeMs) {
            record.count = 0;
            record.windowStart = currentTime;
        }
        
        if (record.count < requestsPerMinute) {
            record.count++;
            // Add rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(requestsPerMinute - record.count));
            return true;
        } else {
            // Rate limit exceeded
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("Retry-After", "60"); // Retry after 60 seconds
            try {
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
            } catch (Exception e) {
                // Ignore
            }
            return false;
        }
    }

    private void refreshConfigCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastConfigRefresh > CONFIG_REFRESH_INTERVAL_MS) {
            try {
                if (rateLimitConfigService != null) {
                    cachedAuthRequestsPerMinute = rateLimitConfigService.getConfigValue(
                        RateLimitConfigService.KEY_AUTH_REQUESTS_PER_MINUTE, 
                        DEFAULT_AUTH_REQUESTS_PER_MINUTE
                    );
                    cachedGeneralRequestsPerMinute = rateLimitConfigService.getConfigValue(
                        RateLimitConfigService.KEY_GENERAL_REQUESTS_PER_MINUTE, 
                        DEFAULT_GENERAL_REQUESTS_PER_MINUTE
                    );
                    cachedPublicRequestsPerMinute = rateLimitConfigService.getConfigValue(
                        RateLimitConfigService.KEY_PUBLIC_REQUESTS_PER_MINUTE, 
                        DEFAULT_PUBLIC_REQUESTS_PER_MINUTE
                    );
                    int windowSizeSeconds = rateLimitConfigService.getConfigValue(
                        RateLimitConfigService.KEY_WINDOW_SIZE_MS, 
                        (int) (DEFAULT_WINDOW_SIZE_MS / 1000)
                    );
                    cachedWindowSizeMs = windowSizeSeconds * 1000L;
                }
            } catch (Exception e) {
                // If service is not available, use defaults
                log.warn("Error refreshing rate limit config", e);
            }
            lastConfigRefresh = currentTime;
        }
    }
    
    private int getRateLimitForPath(String path) {
        // Stricter limits for authentication endpoints
        if (path.contains("/api/auth/")) {
            return cachedAuthRequestsPerMinute;
        }
        
        // Stricter limits for write operations
        if (path.contains("/api/payment/") || 
            path.contains("/api/user/") && (path.contains("POST") || path.contains("PUT") || path.contains("DELETE"))) {
            return cachedAuthRequestsPerMinute * 2; // Double the auth limit for write operations
        }
        
        // Public read-only endpoints get higher limits
        if (path.contains("/api/blog/") || 
            path.contains("/api/leaderboard/") ||
            path.contains("/api/tournaments") && !path.contains("/admin") ||
            path.equals("/api/stock") || path.startsWith("/api/stock?") || // Public stock listing
            path.contains("/api/live-rates")) {
            return cachedPublicRequestsPerMinute;
        }
        
        // Default for other endpoints
        return cachedGeneralRequestsPerMinute;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // Take the first IP in case of multiple proxies
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

