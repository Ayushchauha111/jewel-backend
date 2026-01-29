package com.example.jewell.storage;

import java.io.InputStream;

/**
 * Abstraction for profile image storage.
 * 
 * SOLID:
 * - DIP: services depend on this interface, not on OCI SDK directly.
 * - SRP: storage concerns live here, not in user business logic.
 */
public interface StockImageStorage {
    void upload(String objectName, InputStream inputStream);
    void deleteIfExists(String objectName);
    String getPublicUrl(String objectName);
}

