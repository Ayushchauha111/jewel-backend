package com.example.jewell.controller;

import com.example.jewell.dto.ApiResponseDTO;
import com.example.jewell.dto.InstitutionCreationResponseDTO;
import com.example.jewell.model.Institution;
import com.example.jewell.service.SuperAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/super-admin")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SuperAdminController {

    @Autowired
    private SuperAdminService superAdminService;

    // Get all institutions with pagination
    @GetMapping("/institutions")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getAllInstitutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Map<String, Object> institutions = superAdminService.getAllInstitutions(page, size, sortBy, sortDir);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Institutions retrieved successfully", institutions));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving institutions: " + e.getMessage(), null));
        }
    }

    // Create new institution
    @PostMapping("/institutions")
    public ResponseEntity<ApiResponseDTO<InstitutionCreationResponseDTO>> createInstitution(
            @RequestBody Map<String, Object> institutionData, Authentication authentication) {
        try {
            String institutionName = institutionData.get("institutionName").toString();
            Institution.InstitutionType institutionType = Institution.InstitutionType.valueOf(
                    institutionData.get("institutionType").toString().toUpperCase());
            String contactEmail = institutionData.get("contactEmail").toString();
            String contactPhone = institutionData.get("contactPhone") != null ?
                    institutionData.get("contactPhone").toString() : null;
            String address = institutionData.get("address") != null ?
                    institutionData.get("address").toString() : null;
            String subdomain = institutionData.get("subdomain") != null ?
                    institutionData.get("subdomain").toString() : null;
            Integer maxStudents = institutionData.get("maxStudents") != null ?
                    Integer.valueOf(institutionData.get("maxStudents").toString()) : null;
            // If adminUserId is provided, use it. Otherwise, auto-create admin user (pass null)
            Long assignedAdminUserId = institutionData.get("adminUserId") != null ?
                    Long.valueOf(institutionData.get("adminUserId").toString()) : null;

            InstitutionCreationResponseDTO response = superAdminService.createInstitution(
                    institutionName, institutionType, contactEmail, contactPhone, address,
                    subdomain, maxStudents, assignedAdminUserId);
            
            String message = "Institution created successfully";
            if (response.getAdminCredentials() != null) {
                message += ". Admin credentials generated - save them now!";
            }
            
            return ResponseEntity.ok(new ApiResponseDTO<>(true, message, response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error creating institution: " + e.getMessage(), null));
        }
    }

    // Update institution
    @PutMapping("/institutions/{id}")
    public ResponseEntity<ApiResponseDTO<Institution>> updateInstitution(
            @PathVariable Long id, @RequestBody Map<String, Object> institutionData) {
        try {
            String institutionName = institutionData.get("institutionName") != null ?
                    institutionData.get("institutionName").toString() : null;
            Institution.InstitutionType institutionType = institutionData.get("institutionType") != null ?
                    Institution.InstitutionType.valueOf(institutionData.get("institutionType").toString().toUpperCase()) : null;
            String contactEmail = institutionData.get("contactEmail") != null ?
                    institutionData.get("contactEmail").toString() : null;
            String contactPhone = institutionData.get("contactPhone") != null ?
                    institutionData.get("contactPhone").toString() : null;
            String address = institutionData.get("address") != null ?
                    institutionData.get("address").toString() : null;
            String subdomain = institutionData.get("subdomain") != null ?
                    institutionData.get("subdomain").toString() : null;
            Integer maxStudents = institutionData.get("maxStudents") != null ?
                    Integer.valueOf(institutionData.get("maxStudents").toString()) : null;
            Institution.SubscriptionStatus subscriptionStatus = institutionData.get("subscriptionStatus") != null ?
                    Institution.SubscriptionStatus.valueOf(institutionData.get("subscriptionStatus").toString().toUpperCase()) : null;
            LocalDateTime subscriptionExpiresAt = institutionData.get("subscriptionExpiresAt") != null ?
                    LocalDateTime.parse(institutionData.get("subscriptionExpiresAt").toString()) : null;

            Institution institution = superAdminService.updateInstitution(
                    id, institutionName, institutionType, contactEmail, contactPhone, address,
                    subdomain, maxStudents, subscriptionStatus, subscriptionExpiresAt);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Institution updated successfully", institution));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error updating institution: " + e.getMessage(), null));
        }
    }

    // Delete institution (permanent deletion)
    @DeleteMapping("/institutions/{id}")
    public ResponseEntity<ApiResponseDTO<String>> deleteInstitution(@PathVariable Long id) {
        try {
            superAdminService.deleteInstitution(id);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Institution deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error deleting institution: " + e.getMessage(), null));
        }
    }
    
    // Deactivate institution (soft delete - keeps data)
    @PutMapping("/institutions/{id}/deactivate")
    public ResponseEntity<ApiResponseDTO<Institution>> deactivateInstitution(@PathVariable Long id) {
        try {
            Institution institution = superAdminService.deactivateInstitution(id);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Institution deactivated successfully", institution));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error deactivating institution: " + e.getMessage(), null));
        }
    }

    // Get institution statistics
    @GetMapping("/institutions/{id}/stats")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getInstitutionStats(@PathVariable Long id) {
        try {
            Map<String, Object> stats = superAdminService.getInstitutionStats(id);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Institution stats retrieved successfully", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving stats: " + e.getMessage(), null));
        }
    }

    // Get all institutions statistics (dashboard)
    @GetMapping("/stats")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getAllInstitutionsStats() {
        try {
            Map<String, Object> stats = superAdminService.getAllInstitutionsStats();
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Statistics retrieved successfully", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving statistics: " + e.getMessage(), null));
        }
    }

    // Toggle institution status
    @PutMapping("/institutions/{id}/status")
    public ResponseEntity<ApiResponseDTO<Institution>> toggleInstitutionStatus(
            @PathVariable Long id, @RequestBody Map<String, Boolean> statusData) {
        try {
            Boolean isActive = statusData.get("isActive");
            Institution institution = superAdminService.toggleInstitutionStatus(id, isActive);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Institution status updated successfully", institution));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error updating status: " + e.getMessage(), null));
        }
    }

    // Extend subscription
    @PostMapping("/institutions/{id}/extend-subscription")
    public ResponseEntity<ApiResponseDTO<Institution>> extendSubscription(
            @PathVariable Long id, @RequestBody Map<String, Integer> subscriptionData) {
        try {
            Integer days = subscriptionData.get("days");
            Institution institution = superAdminService.extendSubscription(id, days);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Subscription extended successfully", institution));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error extending subscription: " + e.getMessage(), null));
        }
    }
}
