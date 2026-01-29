package com.example.jewell.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for managing cache operations
 * Provides methods for caching frequently accessed data
 */
@Service
public class CacheService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Cache user profile data
     */
    @Cacheable(value = "userProfile", key = "#userId")
    public Object getUserProfile(Long userId) {
        // This will be called only if cache miss
        // Actual data fetching should be done in the service that calls this
        return null;
    }

    /**
     * Invalidate user profile cache
     */
    @CacheEvict(value = "userProfile", key = "#userId")
    public void evictUserProfile(Long userId) {
        // Cache will be evicted automatically
    }

    /**
     * Cache course catalog
     */
    @Cacheable(value = "courseCatalog")
    public Object getCourseCatalog() {
        return null;
    }

    /**
     * Invalidate course catalog cache
     */
    @CacheEvict(value = "courseCatalog", allEntries = true)
    public void evictCourseCatalog() {
        // All course catalog entries will be evicted
    }

    /**
     * Cache test content
     */
    @Cacheable(value = "testContent", key = "#testId")
    public Object getTestContent(Long testId) {
        return null;
    }

    /**
     * Cache user subscriptions
     */
    @Cacheable(value = "userSubscriptions", key = "#userId")
    public Object getUserSubscriptions(Long userId) {
        return null;
    }

    /**
     * Invalidate user subscriptions cache
     */
    @CacheEvict(value = "userSubscriptions", key = "#userId")
    public void evictUserSubscriptions(Long userId) {
        // Cache will be evicted automatically
    }

    /**
     * Manual cache operations (for advanced use cases)
     * These methods only work when Redis is enabled
     */
    public void setCache(String key, Object value, long timeout, TimeUnit unit) {
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        }
    }

    public Object getCache(String key) {
        if (redisTemplate != null) {
            return redisTemplate.opsForValue().get(key);
        }
        return null;
    }

    public void deleteCache(String key) {
        if (redisTemplate != null) {
            redisTemplate.delete(key);
        }
    }

    public void deleteCachePattern(String pattern) {
        if (redisTemplate != null) {
            redisTemplate.delete(redisTemplate.keys(pattern));
        }
    }
}

