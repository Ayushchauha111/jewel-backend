package com.example.jewell.dto;

import com.example.jewell.model.Institution;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionCreationResponseDTO {
    private Institution institution;
    private AdminCredentials adminCredentials;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminCredentials {
        private String username;
        private String email;
        private String password; // Only returned once during creation
        private String message; // Instructions for sharing
    }
}
