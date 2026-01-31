package com.example.jewell.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Security headers filter to protect against common web vulnerabilities
 */
@Component
@Order(1)
public class SecurityHeadersConfig implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Prevent caching of sensitive data
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        httpResponse.setHeader("Pragma", "no-cache");
        httpResponse.setHeader("Expires", "0");
        
        // Prevent MIME type sniffing
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        
        // Prevent clickjacking
        httpResponse.setHeader("X-Frame-Options", "DENY");
        
        // Enable XSS protection
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Referrer policy - don't leak referrer info
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions policy - allow camera for exam photo capture, restrict other features
        httpResponse.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=(self)");
        
        // Content Security Policy - Less restrictive for API responses
        // Frontend CSP is handled by Netlify, this is mainly for API endpoints
        httpResponse.setHeader("Content-Security-Policy", 
            "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://accounts.google.com https://checkout.razorpay.com; script-src-elem 'self' 'unsafe-inline' 'unsafe-eval' https://accounts.google.com https://checkout.razorpay.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.linearicons.com https://accounts.google.com; style-src-elem 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.linearicons.com https://accounts.google.com; img-src 'self' data: https:; font-src 'self' https://fonts.gstatic.com https://cdn.linearicons.com; connect-src 'self' https://api.gangajewellers.in https://accounts.google.com; frame-src 'self' https://www.google.com https://accounts.google.com https://checkout.razorpay.com https://api.razorpay.com;");
        
        chain.doFilter(request, response);
    } 
}

