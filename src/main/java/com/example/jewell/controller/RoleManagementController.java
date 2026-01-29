package com.example.jewell.controller;

import com.example.jewell.dto.ApiResponseDTO;
import com.example.jewell.model.ERole;
import com.example.jewell.model.Role;
import com.example.jewell.model.User;
import com.example.jewell.repository.RoleRepository;
import com.example.jewell.repository.UserRepository;
import com.example.jewell.utils.ServiceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/roles")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class RoleManagementController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    // Assign SUPER_ADMIN role to a user (only existing ADMIN can do this)
    @PostMapping("/assign-super-admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> assignSuperAdminRole(
            @RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String username = request.get("username");
            if (username == null || username.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponseDTO<>(false, "Username is required", null));
            }

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            Role superAdminRole = roleRepository.findByName(ERole.ROLE_SUPER_ADMIN)
                    .orElseGet(() -> {
                        Role newRole = new Role(ERole.ROLE_SUPER_ADMIN);
                        return roleRepository.save(newRole);
                    });

            Set<Role> userRoles = user.getRoles();
            if (!userRoles.contains(superAdminRole)) {
                userRoles.add(superAdminRole);
                user.setRoles(userRoles);
                userRepository.save(user);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("roles", user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .toList());

            return ResponseEntity.ok(new ApiResponseDTO<>(true, 
                    "SUPER_ADMIN role assigned successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error assigning role: " + e.getMessage(), null));
        }
    }

    // Get current user's roles
    @GetMapping("/my-roles")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getMyRoles(Authentication authentication) {
        try {
            Long userId = ServiceUtils.getUserIdFromAuthentication(authentication);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("username", user.getUsername());
            response.put("roles", user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .toList());

            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Roles retrieved successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving roles: " + e.getMessage(), null));
        }
    }

    // Initialize roles (creates missing roles)
    @PostMapping("/initialize")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> initializeRoles() {
        try {
            Map<String, Object> created = new HashMap<>();

            for (ERole roleEnum : ERole.values()) {
                Role existingRole = roleRepository.findByName(roleEnum).orElse(null);
                if (existingRole == null) {
                    Role newRole = new Role(roleEnum);
                    roleRepository.save(newRole);
                    created.put(roleEnum.name(), "created");
                } else {
                    created.put(roleEnum.name(), "already exists");
                }
            }

            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Roles initialized successfully", created));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error initializing roles: " + e.getMessage(), null));
        }
    }
}
