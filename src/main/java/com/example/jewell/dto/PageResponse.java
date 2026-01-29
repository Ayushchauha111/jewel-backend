package com.example.jewell.dto;

import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for building consistent paginated API responses.
 * All list APIs (billing, stock, orders, credits, customers, etc.) return this structure.
 */
public final class PageResponse {

    private PageResponse() {}

    /**
     * Build a paginated response map from a Spring Data Page.
     * Response shape: content, totalElements, totalPages, currentPage, pageSize, hasNext, hasPrevious
     */
    public static Map<String, Object> of(Page<?> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", page.getContent());
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("currentPage", page.getNumber());
        response.put("pageSize", page.getSize());
        response.put("hasNext", page.hasNext());
        response.put("hasPrevious", page.hasPrevious());
        return response;
    }
}
