package com.example.jewell.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleAuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthService.class);
    
    @Value("${google.client.id}")
    private String googleClientId;
    
    private GoogleIdTokenVerifier verifier;
    
    /**
     * Get or create the Google ID token verifier
     */
    private GoogleIdTokenVerifier getVerifier() {
        if (verifier == null) {
            verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), 
                GsonFactory.getDefaultInstance()
            )
            .setAudience(Collections.singletonList(googleClientId))
            .build();
        }
        return verifier;
    }
    
    /**
     * Verify Google ID token and extract user info
     */
    public GoogleUserInfo verifyToken(String idTokenString) {
        try {
            GoogleIdToken idToken = getVerifier().verify(idTokenString);
            
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                
                // Extract user information
                String email = payload.getEmail();
                boolean emailVerified = payload.getEmailVerified();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");
                String givenName = (String) payload.get("given_name");
                String familyName = (String) payload.get("family_name");
                
                if (!emailVerified) {
                    logger.warn("Google email not verified for: {}", email);
                    return null;
                }
                
                return new GoogleUserInfo(email, name, pictureUrl, givenName, familyName);
            } else {
                logger.warn("Invalid Google ID token");
                return null;
            }
        } catch (Exception e) {
            logger.error("Error verifying Google token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * User info extracted from Google token
     */
    public static class GoogleUserInfo {
        private final String email;
        private final String name;
        private final String pictureUrl;
        private final String givenName;
        private final String familyName;
        
        public GoogleUserInfo(String email, String name, String pictureUrl, 
                             String givenName, String familyName) {
            this.email = email;
            this.name = name;
            this.pictureUrl = pictureUrl;
            this.givenName = givenName;
            this.familyName = familyName;
        }
        
        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getPictureUrl() { return pictureUrl; }
        public String getGivenName() { return givenName; }
        public String getFamilyName() { return familyName; }
        
        /**
         * Generate a username from Google name
         */
        public String generateUsername() {
            if (givenName != null && !givenName.isEmpty()) {
                // Use first name + random numbers if needed
                String base = givenName.toLowerCase().replaceAll("[^a-z0-9]", "");
                return base + "_" + System.currentTimeMillis() % 10000;
            }
            // Fallback to email prefix
            return email.split("@")[0].replaceAll("[^a-z0-9]", "");
        }
    }
}

