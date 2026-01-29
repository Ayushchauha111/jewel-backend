package com.example.jewell.config;

import com.example.jewell.model.FeatureFlag;
import com.example.jewell.repository.FeatureFlagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Feature flag to enable/disable Redis caching. Can be toggled in admin panel.
 * When disabled, falls back to NoOpCacheManager (no caching).
 */
@Component
public class RedisFeatureFlagInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(RedisFeatureFlagInitializer.class);

    private final FeatureFlagRepository featureFlagRepository;

    @Value("${spring.redis.enabled:false}")
    private boolean redisEnabledDefault;

    public RedisFeatureFlagInitializer(FeatureFlagRepository featureFlagRepository) {
        this.featureFlagRepository = featureFlagRepository;
    }

    @Override
    public void run(String... args) {
        Optional<FeatureFlag> existing = featureFlagRepository.findByFeatureKey("redis_caching");
        if (existing.isPresent()) {
            log.info("Redis feature flag already exists (enabled: {})", existing.get().getIsEnabled());
            return;
        }

        FeatureFlag flag = FeatureFlag.builder()
                .featureKey("redis_caching")
                .featureName("Redis Caching")
                .isEnabled(redisEnabledDefault)
                .description("Enable/disable Redis caching for improved performance. When disabled, the app uses NoOpCacheManager (no caching). Toggle this off if Redis breaks in production.")
                .category("infrastructure")
                .displayOrder(1)
                .build();

        featureFlagRepository.save(flag);
        log.info("Created Redis feature flag (default: {})", redisEnabledDefault);
    }
}
