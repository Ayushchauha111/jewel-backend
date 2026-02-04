package com.example.jewell.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.HttpServletRequest;

import com.example.jewell.model.ERole;
import com.example.jewell.model.Role;
import com.example.jewell.model.User;
import com.example.jewell.payload.request.GoogleAuthRequest;
import com.example.jewell.payload.request.LoginRequest;
import com.example.jewell.payload.request.SignupRequest;
import com.example.jewell.payload.response.JwtResponse;
import com.example.jewell.payload.response.MessageResponse;
import com.example.jewell.repository.RoleRepository;
import com.example.jewell.repository.UserRepository;
import com.example.jewell.security.jwt.JwtUtils;
import com.example.jewell.security.services.UserDetailsImpl;
import com.example.jewell.service.AdminSessionService;
import com.example.jewell.service.GoogleAuthService;
import com.example.jewell.service.GoogleAuthService.GoogleUserInfo;
import com.example.jewell.service.DisposableEmailService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  PasswordEncoder encoder;
  
  @Autowired
  JwtUtils jwtUtils;
  
  @Autowired
  GoogleAuthService googleAuthService;
  
  @Autowired
  DisposableEmailService disposableEmailService;

  @Autowired
  AdminSessionService adminSessionService;

  @PostMapping("/signin")
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
      HttpServletRequest request) {

    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);
    
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User user = userRepository.findById(userDetails.getId())
        .orElseThrow(() -> new RuntimeException("User not found"));

    List<String> roles = userDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());

    boolean isAdmin = roles.contains("ROLE_ADMIN");
    String jwt;

    if (isAdmin) {
      // Admin: allow up to 4 devices; track sessions, do not invalidate other devices
      Long currentTokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
      Long sessionId = adminSessionService.createOrRefreshSession(user.getId(), request);
      jwt = jwtUtils.generateJwtToken(authentication, currentTokenVersion, sessionId);
    } else {
      // Non-admin: single device - increment token version on each login
      Long newTokenVersion = (user.getTokenVersion() != null ? user.getTokenVersion() : 0L) + 1;
      user.setTokenVersion(newTokenVersion);
      userRepository.save(user);
      jwt = jwtUtils.generateJwtToken(authentication, newTokenVersion);
    }

    return ResponseEntity.ok(new JwtResponse(jwt,
        userDetails.getId(),
        userDetails.getUsername(),
        userDetails.getEmail(),
        userDetails.getProfileImageUrl(),
        roles));
  }

  @PostMapping("/signup")
  public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    // Check for disposable/fake email
    if (disposableEmailService.shouldBlockRegistration(signUpRequest.getEmail())) {
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("Error: Disposable or temporary email addresses are not allowed. Please use a valid email address."));
    }
    
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("Error: Username is already taken!"));
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("Error: Email is already in use!"));
    }

    // Create new user's account
    User user = new User(signUpRequest.getUsername(),
        signUpRequest.getEmail(),
        encoder.encode(signUpRequest.getPassword()));

    Set<String> strRoles = signUpRequest.getRole();
    Set<Role> roles = new HashSet<>();

    if (strRoles == null) {
      Role userRole = roleRepository.findByName(ERole.ROLE_USER)
          .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
      roles.add(userRole);
    } else {
      strRoles.forEach(role -> {
        switch (role) {
          case "admin":
            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(adminRole);

            break;
          case "mod":
            Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(modRole);

            break;
          default:
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        }
      });
    }

    user.setRoles(roles);
    userRepository.save(user);

    return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
  }

  /**
   * Google OAuth Sign-In/Sign-Up
   * Creates new user if doesn't exist, or logs in existing user
   */
  @PostMapping("/google")
  public ResponseEntity<?> googleAuth(@Valid @RequestBody GoogleAuthRequest googleRequest) {
    // Verify Google token
    GoogleUserInfo googleUser = googleAuthService.verifyToken(googleRequest.getCredential());
    
    if (googleUser == null) {
      return ResponseEntity.badRequest()
          .body(new MessageResponse("Invalid Google credentials"));
    }
    
    // Check for disposable email (even via Google)
    if (disposableEmailService.shouldBlockRegistration(googleUser.getEmail())) {
      return ResponseEntity.badRequest()
          .body(new MessageResponse("Error: Disposable or temporary email addresses are not allowed."));
    }
    
    // Check if user exists by email
    Optional<User> existingUser = userRepository.findByEmail(googleUser.getEmail());
    
    User user;
    if (existingUser.isPresent()) {
      // Existing user - log them in
      user = existingUser.get();
      
      // Update profile image if changed
      if (googleUser.getPictureUrl() != null && 
          (user.getProfileImageUrl() == null || user.getProfileImageUrl().isEmpty())) {
        user.setProfileImageUrl(googleUser.getPictureUrl());
        userRepository.save(user);
      }
    } else {
      // New user - create account
      String username = googleUser.generateUsername();
      
      // Ensure username is unique
      while (userRepository.existsByUsername(username)) {
        username = googleUser.generateUsername();
      }
      
      // Generate a random secure password (user won't need it for Google login)
      String randomPassword = UUID.randomUUID().toString();
      
      user = new User(username, googleUser.getEmail(), encoder.encode(randomPassword));
      user.setProfileImageUrl(googleUser.getPictureUrl());
      
      // Set default user role
      Set<Role> roles = new HashSet<>();
      Role userRole = roleRepository.findByName(ERole.ROLE_USER)
          .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
      roles.add(userRole);
      user.setRoles(roles);
      
      userRepository.save(user);
    }
    
    // Build UserDetails and generate JWT token directly (no password auth needed for Google)
    // Increment token version to invalidate previous sessions
    Long newTokenVersion = (user.getTokenVersion() != null ? user.getTokenVersion() : 0L) + 1;
    user.setTokenVersion(newTokenVersion);
    userRepository.save(user);
    
    UserDetailsImpl userDetails = UserDetailsImpl.build(user);
    String jwt = jwtUtils.generateJwtTokenFromUsername(userDetails.getUsername(), newTokenVersion);
    
    List<String> roles = userDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(new JwtResponse(jwt,
        userDetails.getId(),
        userDetails.getUsername(),
        userDetails.getEmail(),
        userDetails.getProfileImageUrl(),
        roles));
  }

  /**
   * Logout: remove current admin session (frees device slot for multi-device limit).
   * Call with Bearer token; the session for this token is deleted.
   */
  @PostMapping("/logout")
  public ResponseEntity<?> logout(Authentication authentication, HttpServletRequest request) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.ok(new MessageResponse("Logged out"));
    }
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String jwt = authHeader.substring(7);
      Long sessionId = jwtUtils.getSessionIdFromJwtToken(jwt);
      if (sessionId != null) {
        adminSessionService.deleteSession(sessionId, userDetails.getId());
      }
    }
    return ResponseEntity.ok(new MessageResponse("Logged out"));
  }

  /**
   * Get active sessions for current admin (multi-device limit).
   * Returns { "activeSessions": number, "sessions": [ { id, createdAt, lastUsedAt } ], "currentSessionId": id or null }.
   * Non-admin users get activeSessions: 0, sessions: [].
   */
  @GetMapping("/sessions")
  public ResponseEntity<?> getActiveSessions(Authentication authentication, HttpServletRequest request) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
    }
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    List<String> roles = userDetails.getAuthorities().stream()
        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
        .collect(Collectors.toList());
    boolean isAdmin = roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SUPER_ADMIN");
    Long currentSessionId = null;
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      currentSessionId = jwtUtils.getSessionIdFromJwtToken(authHeader.substring(7));
    }
    long count = 0L;
    java.util.List<com.example.jewell.service.AdminSessionService.AdminSessionDto> sessions = java.util.Collections.emptyList();
    if (isAdmin) {
      count = adminSessionService.getActiveSessionCount(userDetails.getId());
      sessions = adminSessionService.getSessionsForUser(userDetails.getId());
    }
    java.util.Map<String, Object> body = new java.util.HashMap<>();
    body.put("activeSessions", count);
    body.put("sessions", sessions);
    body.put("currentSessionId", currentSessionId);
    return ResponseEntity.ok(body);
  }

  /**
   * Remove a session (log out that device). Only the session owner can remove it.
   * If you remove the current session, your token will be invalid; frontend should then redirect to login.
   */
  @DeleteMapping("/sessions/{sessionId}")
  public ResponseEntity<Void> removeSession(@PathVariable Long sessionId, Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
    }
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    adminSessionService.deleteSession(sessionId, userDetails.getId());
    return ResponseEntity.noContent().build();
  }

  /**
   * Validate current session token
   * Returns 200 if token is valid, 401 if invalid
   */
  @GetMapping("/validate")
  public ResponseEntity<?> validateToken(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
          .body(new MessageResponse("Invalid or expired token"));
    }
    return ResponseEntity.ok(new MessageResponse("Token is valid"));
  }

  /**
   * Change password for authenticated user
   * Requires current password verification for security
   */
  @PostMapping("/change-password")
  public ResponseEntity<?> changePassword(
      @RequestParam String currentPassword,
      @RequestParam String newPassword,
      Authentication authentication) {
    
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
          .body(new MessageResponse("Authentication required"));
    }
    
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User user = userRepository.findById(userDetails.getId())
        .orElseThrow(() -> new RuntimeException("User not found"));
    
    // Verify current password
    if (!encoder.matches(currentPassword, user.getPassword())) {
      return ResponseEntity.badRequest()
          .body(new MessageResponse("Current password is incorrect"));
    }
    
    // Validate new password
    if (newPassword == null || newPassword.length() < 6) {
      return ResponseEntity.badRequest()
          .body(new MessageResponse("New password must be at least 6 characters"));
    }
    
    if (currentPassword.equals(newPassword)) {
      return ResponseEntity.badRequest()
          .body(new MessageResponse("New password must be different from current password"));
    }
    
    // Update password
    user.setPassword(encoder.encode(newPassword));
    
    // Increment token version to invalidate all other sessions
    Long newTokenVersion = (user.getTokenVersion() != null ? user.getTokenVersion() : 0L) + 1;
    user.setTokenVersion(newTokenVersion);
    userRepository.save(user);
    
    // Generate new JWT token
    UserDetailsImpl updatedUserDetails = UserDetailsImpl.build(user);
    String jwt = jwtUtils.generateJwtTokenFromUsername(updatedUserDetails.getUsername(), newTokenVersion);
    
    List<String> roles = updatedUserDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(new JwtResponse(jwt,
        updatedUserDetails.getId(),
        updatedUserDetails.getUsername(),
        updatedUserDetails.getEmail(),
        updatedUserDetails.getProfileImageUrl(),
        roles));
  }

}