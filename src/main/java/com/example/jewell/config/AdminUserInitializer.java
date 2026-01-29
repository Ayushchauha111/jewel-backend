package com.example.jewell.config;

import com.example.jewell.model.ERole;
import com.example.jewell.model.Role;
import com.example.jewell.model.User;
import com.example.jewell.repository.RoleRepository;
import com.example.jewell.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@Order(1) // Run first to ensure admin user exists
public class AdminUserInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Initialize roles first
        initializeRoles();

        // Create default admin user if it doesn't exist
        Optional<User> adminUser = userRepository.findByUsername("admin");
        if (adminUser.isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@jewelryshop.com");
            admin.setPassword(passwordEncoder.encode("admin123")); // Default password
            
            Set<Role> roles = new HashSet<>();
            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            roles.add(adminRole);
            admin.setRoles(roles);
            
            userRepository.save(admin);
            System.out.println("==========================================");
            System.out.println("Default Admin User Created!");
            System.out.println("Username: admin");
            System.out.println("Password: admin123");
            System.out.println("Email: admin@jewelryshop.com");
            System.out.println("==========================================");
        } else {
            System.out.println("Admin user already exists: " + adminUser.get().getUsername());
        }
    }

    private void initializeRoles() {
        for (ERole roleEnum : ERole.values()) {
            Optional<Role> existingRole = roleRepository.findByName(roleEnum);
            if (existingRole.isEmpty()) {
                Role newRole = new Role(roleEnum);
                roleRepository.save(newRole);
                System.out.println("Created role: " + roleEnum.name());
            }
        }
    }
}
