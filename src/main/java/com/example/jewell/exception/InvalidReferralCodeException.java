package com.example.jewell.exception;

public class InvalidReferralCodeException extends RuntimeException {
    public InvalidReferralCodeException(String message) {
        super(message);
    }
}