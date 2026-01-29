package com.example.jewell.service;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Service
public class OtpService {
    private final Map<String, String> otpStorage = new HashMap<>();
    private static final SecureRandom random = new SecureRandom();

    public String generateOtp(String email) {
        String otp = String.format("%06d", random.nextInt(1000000));
        otpStorage.put(email, otp);
        return otp;
    }

    public boolean verifyOtp(String email, String enteredOtp) {
        String storedOtp = otpStorage.get(email);
        return storedOtp != null && storedOtp.equals(enteredOtp);
    }

    public void clearOtp(String email) {
        otpStorage.remove(email);
    }
}