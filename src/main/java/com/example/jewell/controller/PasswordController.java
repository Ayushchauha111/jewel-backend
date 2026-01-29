package com.example.jewell.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.jewell.dto.ApiResponseDTO;
import com.example.jewell.exception.ResourceNotFoundException;
import com.example.jewell.repository.UserRepository;
import com.example.jewell.service.EmailService;
import com.example.jewell.service.OtpService;
import com.example.jewell.service.UserService;

@RestController
@RequestMapping("/api/password")
public class PasswordController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    @Autowired
    UserRepository userRepository;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO<Object>> forgotPassword(@RequestParam String email) {
        try {
            return userRepository.findByEmail(email)
                    .map(user -> {
                        // Logic for sending the reset email
                        String otp = otpService.generateOtp(email);
                        emailService.sendOtpEmail(email, otp);
                        return ResponseEntity.ok(new ApiResponseDTO<>(true, "OTP sent to your email.", null));
                    })
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ApiResponseDTO<>(false, "User not found with email: " + email, null)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error occurred while processing your request.", null));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO<Object>> resetPassword(@RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword) {
        if (!otpService.verifyOtp(email, otp)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, "Invalid OTP.", null));
        }

        try {
            userService.updatePassword(email, newPassword);
            otpService.clearOtp(email);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Password reset successfully.", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, "User not found with email: " + email, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error occurred while resetting the password.", null));
        }
    }
}