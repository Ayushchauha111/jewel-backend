package com.example.jewell.service;

import com.example.jewell.dto.InstitutionCreationResponseDTO;
import com.example.jewell.model.*;
import com.example.jewell.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SuperAdminService {

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BatchRepository batchRepository;


    // Get all institutions with pagination
    @Transactional(readOnly = true)
    public Map<String, Object> getAllInstitutions(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Institution> institutionPage = institutionRepository.findAll(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("institutions", institutionPage.getContent());
        response.put("currentPage", institutionPage.getNumber());
        response.put("totalItems", institutionPage.getTotalElements());
        response.put("totalPages", institutionPage.getTotalPages());
        return response;
    }

    // Create new institution (Super Admin only)
    // If adminUserId is null, automatically creates a new admin user
    @Transactional
    public InstitutionCreationResponseDTO createInstitution(String institutionName, Institution.InstitutionType institutionType,
                                         String contactEmail, String contactPhone, String address,
                                         String subdomain, Integer maxStudents, Long adminUserId) {
        // Check if subdomain is unique
        if (subdomain != null && !subdomain.isEmpty()) {
            Optional<Institution> existing = institutionRepository.findBySubdomain(subdomain);
            if (existing.isPresent()) {
                throw new RuntimeException("Subdomain already exists: " + subdomain);
            }
        }

        User adminUser;
        String generatedPassword = null;
        String generatedUsername = null;

        if (adminUserId != null) {
            // Use existing user
            adminUser = userRepository.findById(adminUserId)
                    .orElseThrow(() -> new RuntimeException("Admin user not found"));
        } else {
            // Automatically create new admin user
            generatedUsername = generateInstitutionAdminUsername(institutionName);
            generatedPassword = generateSecurePassword();
            
            // Ensure username is unique
            int counter = 1;
            String baseUsername = generatedUsername;
            while (userRepository.existsByUsername(generatedUsername)) {
                generatedUsername = baseUsername + counter;
                counter++;
            }
            
            // Check if contact email is already used
            if (userRepository.existsByEmail(contactEmail)) {
                throw new RuntimeException("Email " + contactEmail + " is already registered. Please use a different email or specify an existing adminUserId.");
            }
            
            // Create new user
            adminUser = new User();
            adminUser.setUsername(generatedUsername);
            adminUser.setEmail(contactEmail);
            adminUser.setPassword(passwordEncoder.encode(generatedPassword));
            adminUser.setRoles(new HashSet<>());
            adminUser = userRepository.save(adminUser);
        }

        // Assign institution admin role
        // Handle case where multiple roles with same name might exist (duplicates in DB)
        Role institutionAdminRole;
        try {
            Optional<Role> roleOpt = roleRepository.findByName(ERole.ROLE_INSTITUTION_ADMIN);
            if (roleOpt.isPresent()) {
                institutionAdminRole = roleOpt.get();
            } else {
                // Create new role if it doesn't exist
                institutionAdminRole = new Role();
                institutionAdminRole.setName(ERole.ROLE_INSTITUTION_ADMIN);
                institutionAdminRole = roleRepository.save(institutionAdminRole);
            }
        } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
            // Handle duplicate roles - get the first one
            List<Role> roles = roleRepository.findAllByName(ERole.ROLE_INSTITUTION_ADMIN);
            if (!roles.isEmpty()) {
                institutionAdminRole = roles.get(0);
            } else {
                // Create new role if none found
                institutionAdminRole = new Role();
                institutionAdminRole.setName(ERole.ROLE_INSTITUTION_ADMIN);
                institutionAdminRole = roleRepository.save(institutionAdminRole);
            }
        }

        if (!adminUser.getRoles().contains(institutionAdminRole)) {
            adminUser.getRoles().add(institutionAdminRole);
            userRepository.save(adminUser);
        }

        Institution institution = new Institution();
        institution.setInstitutionName(institutionName);
        institution.setInstitutionType(institutionType);
        institution.setContactEmail(contactEmail);
        institution.setContactPhone(contactPhone);
        institution.setAddress(address);
        institution.setSubdomain(subdomain);
        institution.setAdminUser(adminUser);
        institution.setLicenseKey(generateLicenseKey());
        institution.setMaxStudents(maxStudents != null ? maxStudents : 1000000); // Default 1M students
        institution.setSubscriptionStatus(Institution.SubscriptionStatus.TRIAL);
        institution.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(30));
        institution.setIsActive(true);

        institution = institutionRepository.save(institution);

        // Create response with credentials if user was auto-created
        InstitutionCreationResponseDTO response = new InstitutionCreationResponseDTO();
        response.setInstitution(institution);
        
        if (generatedPassword != null) {
            InstitutionCreationResponseDTO.AdminCredentials credentials = new InstitutionCreationResponseDTO.AdminCredentials();
            credentials.setUsername(generatedUsername);
            credentials.setEmail(contactEmail);
            credentials.setPassword(generatedPassword);
            credentials.setMessage("⚠️ IMPORTANT: Save these credentials now! The password will not be shown again. Share these with the institution admin.");
            response.setAdminCredentials(credentials);
        }

        return response;
    }

    private String generateInstitutionAdminUsername(String institutionName) {
        // Convert institution name to username format
        // e.g., "Parmaar Coaching" -> "parmaar_coaching_admin"
        String username = institutionName.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")  // Replace non-alphanumeric with underscore
                .replaceAll("_+", "_")         // Replace multiple underscores with single
                .replaceAll("^_|_$", "");      // Remove leading/trailing underscores
        
        if (username.isEmpty()) {
            username = "institution_admin";
        }
        
        return username + "_admin";
    }

    private String generateSecurePassword() {
        // Generate a secure random password: 12 characters
        // Mix of uppercase, lowercase, numbers, and special characters
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String special = "!@#$%^&*";
        String allChars = upperCase + lowerCase + numbers + special;
        
        java.util.Random random = new java.util.Random();
        StringBuilder password = new StringBuilder();
        
        // Ensure at least one of each type
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(special.charAt(random.nextInt(special.length())));
        
        // Fill the rest randomly
        for (int i = 4; i < 12; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // Shuffle the password
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }

    // Update institution (Super Admin only)
    @Transactional
    public Institution updateInstitution(Long institutionId, String institutionName,
                                        Institution.InstitutionType institutionType,
                                        String contactEmail, String contactPhone, String address,
                                        String subdomain, Integer maxStudents,
                                        Institution.SubscriptionStatus subscriptionStatus,
                                        LocalDateTime subscriptionExpiresAt) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("Institution not found"));

        if (subdomain != null && !subdomain.isEmpty() && !subdomain.equals(institution.getSubdomain())) {
            Optional<Institution> existing = institutionRepository.findBySubdomain(subdomain);
            if (existing.isPresent() && !existing.get().getId().equals(institutionId)) {
                throw new RuntimeException("Subdomain already exists: " + subdomain);
            }
        }

        if (institutionName != null) institution.setInstitutionName(institutionName);
        if (institutionType != null) institution.setInstitutionType(institutionType);
        if (contactEmail != null) institution.setContactEmail(contactEmail);
        if (contactPhone != null) institution.setContactPhone(contactPhone);
        if (address != null) institution.setAddress(address);
        if (subdomain != null) institution.setSubdomain(subdomain);
        if (maxStudents != null) institution.setMaxStudents(maxStudents);
        if (subscriptionStatus != null) institution.setSubscriptionStatus(subscriptionStatus);
        if (subscriptionExpiresAt != null) institution.setSubscriptionExpiresAt(subscriptionExpiresAt);

        return institutionRepository.save(institution);
    }

    // Delete institution (Super Admin only) - Hard delete
    @Transactional
    public void deleteInstitution(Long institutionId) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("Institution not found"));
        
        // JPA cascade should handle related entities (batches, lab sessions)
        // But we'll delete explicitly to ensure clean deletion
        
        // Delete batches and their relationships
        List<Batch> batches = batchRepository.findByInstitutionId(institutionId);
        if (!batches.isEmpty()) {
            batchRepository.deleteAll(batches);
        }
        
        // Delete the institution itself
        // This will cascade delete any remaining relationships
        institutionRepository.delete(institution);
    }
    
    // Deactivate institution (Super Admin only) - Soft delete
    @Transactional
    public Institution deactivateInstitution(Long institutionId) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("Institution not found"));
        institution.setIsActive(false);
        return institutionRepository.save(institution);
    }

    // Get institution statistics
    @Transactional(readOnly = true)
    public Map<String, Object> getInstitutionStats(Long institutionId) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("Institution not found"));

        List<Batch> batches = batchRepository.findByInstitutionId(institutionId);
        int totalBatches = batches.size();
        int totalStudents = batches.stream()
                .mapToInt(batch -> batch.getStudents() != null ? batch.getStudents().size() : 0)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("institutionId", institutionId);
        stats.put("institutionName", institution.getInstitutionName());
        stats.put("totalBatches", totalBatches);
        stats.put("totalStudents", totalStudents);
        stats.put("maxStudents", institution.getMaxStudents());
        stats.put("subscriptionStatus", institution.getSubscriptionStatus());
        stats.put("subscriptionExpiresAt", institution.getSubscriptionExpiresAt());
        stats.put("isActive", institution.getIsActive());
        return stats;
    }

    // Get all institutions statistics (Super Admin dashboard)
    @Transactional(readOnly = true)
    public Map<String, Object> getAllInstitutionsStats() {
        List<Institution> institutions = institutionRepository.findAll();
        
        int totalInstitutions = institutions.size();
        int activeInstitutions = (int) institutions.stream()
                .filter(i -> i.getIsActive() != null && i.getIsActive())
                .count();
        int totalStudents = 0;
        int totalBatches = 0;

        for (Institution inst : institutions) {
            List<Batch> batches = batchRepository.findByInstitutionId(inst.getId());
            totalBatches += batches.size();
            totalStudents += batches.stream()
                    .mapToInt(batch -> batch.getStudents() != null ? batch.getStudents().size() : 0)
                    .sum();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInstitutions", totalInstitutions);
        stats.put("activeInstitutions", activeInstitutions);
        stats.put("totalBatches", totalBatches);
        stats.put("totalStudents", totalStudents);
        return stats;
    }

    // Activate/Deactivate institution
    @Transactional
    public Institution toggleInstitutionStatus(Long institutionId, Boolean isActive) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("Institution not found"));
        institution.setIsActive(isActive);
        return institutionRepository.save(institution);
    }

    // Extend subscription
    @Transactional
    public Institution extendSubscription(Long institutionId, int days) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("Institution not found"));
        LocalDateTime currentExpiry = institution.getSubscriptionExpiresAt();
        if (currentExpiry == null) {
            currentExpiry = LocalDateTime.now();
        }
        institution.setSubscriptionExpiresAt(currentExpiry.plusDays(days));
        institution.setSubscriptionStatus(Institution.SubscriptionStatus.ACTIVE);
        return institutionRepository.save(institution);
    }

    private String generateLicenseKey() {
        return "INST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
