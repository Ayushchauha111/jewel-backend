package com.example.jewell.exception;

public class DuplicateReferralException extends RuntimeException {
    public DuplicateReferralException(String message) {
        super(message);
    }
}