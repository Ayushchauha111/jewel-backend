package com.example.jewell.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.cache.support.NoOpCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;
    
    /**
     * NOTE: Redis beans are created based on spring.redis.enabled property.
     * To disable Redis via feature flag (redis_caching), also set spring.redis.enabled=false
     * in application.properties and restart the application.
     * The feature flag is primarily for admin visibility and future runtime control.
     */

    // Redis beans - only created when spring.redis.enabled=true
    @Bean
    @ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true")
    public CacheManager redisCacheManager(@Autowired(required = false) RedisConnectionFactory connectionFactory) {
        if (connectionFactory == null) {
            // Fallback to NoOpCacheManager if Redis connection factory is not available
            log.warn("Redis connection factory is null. Falling back to NoOpCacheManager.");
            return new NoOpCacheManager();
        }
        
        try {
            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofHours(1)) // Default TTL: 1 hour
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                    .disableCachingNullValues();

            // Cache-specific configurations
            RedisCacheConfiguration userProfileConfig = defaultConfig.entryTtl(Duration.ofHours(1));
            RedisCacheConfiguration courseCatalogConfig = defaultConfig.entryTtl(Duration.ofHours(1));
            RedisCacheConfiguration testContentConfig = defaultConfig.entryTtl(Duration.ofHours(24));
            RedisCacheConfiguration userSubscriptionsConfig = defaultConfig.entryTtl(Duration.ofMinutes(15));
            RedisCacheConfiguration recentResultsConfig = defaultConfig.entryTtl(Duration.ofHours(1));

            CacheManager redisManager = RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultConfig)
                    .withCacheConfiguration("userProfile", userProfileConfig)
                    .withCacheConfiguration("courseCatalog", courseCatalogConfig)
                    .withCacheConfiguration("testContent", testContentConfig)
                    .withCacheConfiguration("userSubscriptions", userSubscriptionsConfig)
                    .withCacheConfiguration("recentResults", recentResultsConfig)
                    .build();
            
            // Test the connection by trying to get a cache
            try {
                redisManager.getCache("testConnection");
                log.info("Redis cache manager initialized successfully.");
            } catch (Exception e) {
                log.error("Redis cache manager test failed: {}. Falling back to NoOpCacheManager.", e.getMessage());
                return new NoOpCacheManager();
            }
            
            // Wrap with resilient cache manager to handle runtime failures
            return new ResilientCacheManagerWrapper(redisManager);
        } catch (Exception e) {
            log.error("Failed to create Redis cache manager: {}. Falling back to NoOpCacheManager.", e.getMessage(), e);
            return new NoOpCacheManager();
        }
    }
    
    /**
     * Wrapper that catches Redis exceptions at runtime and falls back to NoOpCacheManager
     */
    private static class ResilientCacheManagerWrapper implements CacheManager {
        private final CacheManager delegate;
        private final NoOpCacheManager fallback;
        private volatile boolean useFallback = false;
        
        public ResilientCacheManagerWrapper(CacheManager delegate) {
            this.delegate = delegate;
            this.fallback = new NoOpCacheManager();
        }
        
        @Override
        public Cache getCache(String name) {
            if (useFallback) {
                return fallback.getCache(name);
            }
            try {
                Cache cache = delegate.getCache(name);
                if (cache == null) {
                    return fallback.getCache(name);
                }
                return new ResilientCacheWrapper(cache, fallback.getCache(name));
            } catch (Exception e) {
                log.warn("Failed to get cache '{}' from Redis: {}. Using fallback.", name, e.getMessage());
                useFallback = true;
                return fallback.getCache(name);
            }
        }
        
        @Override
        public java.util.Collection<String> getCacheNames() {
            if (useFallback) {
                return fallback.getCacheNames();
            }
            try {
                return delegate.getCacheNames();
            } catch (Exception e) {
                log.warn("Failed to get cache names from Redis: {}. Using fallback.", e.getMessage());
                useFallback = true;
                return fallback.getCacheNames();
            }
        }
    }
    
    /**
     * Wrapper that catches Redis exceptions during cache operations
     */
    private static class ResilientCacheWrapper implements Cache {
        private final Cache delegate;
        private final Cache fallback;
        private volatile boolean useFallback = false;
        
        public ResilientCacheWrapper(Cache delegate, Cache fallback) {
            this.delegate = delegate;
            this.fallback = fallback;
        }
        
        @Override
        public String getName() {
            return delegate.getName();
        }
        
        @Override
        public Object getNativeCache() {
            if (useFallback) {
                return fallback.getNativeCache();
            }
            try {
                return delegate.getNativeCache();
            } catch (Exception e) {
                log.debug("Cache operation failed: {}. Using fallback.", e.getMessage());
                useFallback = true;
                return fallback.getNativeCache();
            }
        }
        
        @Override
        public ValueWrapper get(Object key) {
            if (useFallback) {
                return fallback.get(key);
            }
            try {
                return delegate.get(key);
            } catch (Exception e) {
                log.debug("Cache get failed for key '{}': {}. Using fallback.", key, e.getMessage());
                useFallback = true;
                return fallback.get(key);
            }
        }
        
        @Override
        public <T> T get(Object key, Class<T> type) {
            if (useFallback) {
                return fallback.get(key, type);
            }
            try {
                return delegate.get(key, type);
            } catch (Exception e) {
                log.debug("Cache get failed for key '{}': {}. Using fallback.", key, e.getMessage());
                useFallback = true;
                return fallback.get(key, type);
            }
        }
        
        @Override
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            if (useFallback) {
                return fallback.get(key, valueLoader);
            }
            try {
                return delegate.get(key, valueLoader);
            } catch (Exception e) {
                log.debug("Cache get failed for key '{}': {}. Using fallback.", key, e.getMessage());
                useFallback = true;
                return fallback.get(key, valueLoader);
            }
        }
        
        @Override
        public void put(Object key, Object value) {
            if (useFallback) {
                fallback.put(key, value);
                return;
            }
            try {
                delegate.put(key, value);
            } catch (Exception e) {
                log.debug("Cache put failed for key '{}': {}. Using fallback.", key, e.getMessage());
                useFallback = true;
                fallback.put(key, value);
            }
        }
        
        @Override
        public void evict(Object key) {
            if (useFallback) {
                fallback.evict(key);
                return;
            }
            try {
                delegate.evict(key);
            } catch (Exception e) {
                log.debug("Cache evict failed for key '{}': {}. Using fallback.", key, e.getMessage());
                useFallback = true;
                fallback.evict(key);
            }
        }
        
        @Override
        public void clear() {
            if (useFallback) {
                fallback.clear();
                return;
            }
            try {
                delegate.clear();
            } catch (Exception e) {
                log.debug("Cache clear failed: {}. Using fallback.", e.getMessage());
                useFallback = true;
                fallback.clear();
            }
        }
    }
    
    // Fallback: No-op cache manager when Redis is disabled
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager noOpCacheManager() {
        // NoOpCacheManager handles all cache names dynamically, so no need to pre-register them
        // It will create NoOpCache instances on demand for any cache name
        return new NoOpCacheManager();
    }
}

