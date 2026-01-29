package com.example.jewell.config;

import com.example.jewell.model.FeatureFlag;
import com.example.jewell.repository.FeatureFlagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Initializes feature flags for major features that can be toggled in admin panel.
 */
@Component
public class MajorFeaturesFlagInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(MajorFeaturesFlagInitializer.class);

    private final FeatureFlagRepository featureFlagRepository;

    public MajorFeaturesFlagInitializer(FeatureFlagRepository featureFlagRepository) {
        this.featureFlagRepository = featureFlagRepository;
    }

    @Override
    public void run(String... args) {
        createFlagIfNotExists("live_exams", "Live Exams", true,
                "Enable/disable the live exam feature (timed typing tests with real-time leaderboards).", "exams", 2);
        
        createFlagIfNotExists("leaderboard", "Leaderboard System", true,
                "Enable/disable the global leaderboard feature.", "social", 3);
        
        createFlagIfNotExists("community_hub", "Community Hub", true,
                "Enable/disable community features (forums, discussions, study rooms).", "community", 4);
        
        createFlagIfNotExists("typing_races", "Typing Races", true,
                "Enable/disable typing race competitions.", "games", 5);
        
        createFlagIfNotExists("daily_challenges", "Daily Challenges", true,
                "Enable/disable daily typing challenges.", "games", 6);
        
        createFlagIfNotExists("tournaments", "Tournaments", true,
                "Enable/disable typing tournaments.", "games", 7);
        
        createFlagIfNotExists("achievements", "Achievements System", true,
                "Enable/disable achievements and badges.", "gamification", 8);
        
        createFlagIfNotExists("social_features", "Social Features", true,
                "Enable/disable social features (friends, chat, follow).", "social", 9);
        
        createFlagIfNotExists("blog_system", "Blog System", true,
                "Enable/disable blog posts and articles.", "content", 11);
        
        createFlagIfNotExists("email_notifications", "Email Notifications", true,
                "Enable/disable email notifications and newsletters.", "communication", 12);
        
        createFlagIfNotExists("referral_system", "Referral System", true,
                "Enable/disable referral and rewards program.", "monetization", 13);
        
        createFlagIfNotExists("ssc_rrb_exam_flow", "SSC/RRB Exam Flow", true,
                "Enable/disable the 4-step flow process for SSC (CGL/CHSL) and RRB exams.", "exams", 14);
        
        createFlagIfNotExists("rank_predictor", "Rank Predictor & Marks Calculator", true,
                "Enable/disable the rank predictor and marks calculator for government exams (SSC, Railway, Banking, etc.).", "exams", 15);
        
        log.info("Major features flags initialized");
    }

    private void createFlagIfNotExists(String key, String name, boolean defaultEnabled, 
                                       String description, String category, int displayOrder) {
        Optional<FeatureFlag> existing = featureFlagRepository.findByFeatureKey(key);
        if (existing.isPresent()) return;

        FeatureFlag flag = FeatureFlag.builder()
                .featureKey(key)
                .featureName(name)
                .isEnabled(defaultEnabled)
                .description(description)
                .category(category)
                .displayOrder(displayOrder)
                .build();

        featureFlagRepository.save(flag);
        log.debug("Created feature flag: {}", key);
    }
}
