package com.example.jewell.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.jewell.model.User;
import com.example.jewell.model.Role;
import com.example.jewell.model.ERole;
import com.example.jewell.repository.UserRepository;
import com.example.jewell.repository.RoleRepository;
import com.example.jewell.storage.ProfileImageStorage;

import java.io.IOException;
import java.io.InputStream;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProfileImageStorage profileImageStorage;

    @Autowired
    private RoleRepository roleRepository;

    @CacheEvict(value = "userProfile", key = "#userId")
    public User uploadProfileImage(Long userId, MultipartFile file) throws IOException {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Generate a unique filename
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            // Best-effort delete old profile image
            profileImageStorage.deleteIfExists(user.getProfileImageUrl());

            // Upload the new file
            try (InputStream is = file.getInputStream()) {
                profileImageStorage.upload(filename, is);
            }

            // Update the user's profile image URL
            user.setProfileImageUrl(filename);
            User savedUser = userRepository.save(user);
            return savedUser;
        } else {
            throw new RuntimeException("User not found with id: " + userId);
        }
    }

    @CacheEvict(value = "userProfile", key = "#result.id", condition = "#result != null")
    public User updatePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Cacheable(value = "userProfile", key = "#userId")
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    // Admin methods
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public org.springframework.data.domain.Page<User> getUsersPaginated(
            org.springframework.data.domain.Pageable pageable, 
            String search) {
        if (search != null && !search.isEmpty()) {
            return userRepository.findByUsernameContainingIgnoreCase(search, pageable);
        }
        return userRepository.findAll(pageable);
    }

    public User updateUserRoles(Long userId, java.util.Set<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        java.util.Set<Role> roles = new java.util.HashSet<>();
        for (String roleName : roleNames) {
            ERole roleEnum = ERole.valueOf(roleName);
            Role role = roleRepository.findByName(roleEnum)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            roles.add(role);
        }
        user.setRoles(roles);
        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        userRepository.delete(user);
    }

    public long getUserCount() {
        return userRepository.count();
    }
}