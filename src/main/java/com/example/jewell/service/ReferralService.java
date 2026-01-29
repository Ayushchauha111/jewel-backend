package com.example.jewell.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.jewell.exception.DuplicateReferralException;
import com.example.jewell.exception.InvalidReferralCodeException;
import com.example.jewell.exception.UserNotFoundException;
import com.example.jewell.model.User;
import com.example.jewell.repository.UserRepository;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class ReferralService {

    @Autowired
    private UserRepository userRepository;

    public String generateReferralLink(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getReferralCode() != null && !user.getReferralCode().isEmpty()) {
            return user.getReferralLink();
        }

        String referralCode = generateRandomCode();
        user.setReferralCode(referralCode);
        user.setReferralLink("https://typogram.in/?ref=" + referralCode);
        user.setEarned(0.0);
        userRepository.save(user);

        return user.getReferralLink();
    }

    public String trackReferral(String referralCode, String newUserId) {
        User referrer = userRepository.findByReferralCode(referralCode);
        if (referrer == null) {
            throw new InvalidReferralCodeException("Invalid referral code");
        }

        if (referrer.getReferredUsers().contains(newUserId)) {
            throw new DuplicateReferralException("User already referred");
        }

        referrer.getReferredUsers().add(newUserId);
        referrer.setEarned(referrer.getEarned() + 10);
        userRepository.save(referrer);

        return "Referral tracked successfully";
    }

    public User getReferralStats(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private String generateRandomCode() {
        return ThreadLocalRandom.current()
                .ints(48, 122)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(8)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}