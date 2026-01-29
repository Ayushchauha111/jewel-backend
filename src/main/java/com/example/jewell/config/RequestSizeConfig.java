package com.example.jewell.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Request size limiting filter to prevent DoS attacks
 */
@Configuration
public class RequestSizeConfig {

    private static final long MAX_REQUEST_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_MULTIPART_SIZE = 5 * 1024 * 1024; // 5MB for file uploads

    @Bean
    public FilterRegistrationBean<RequestSizeFilter> requestSizeFilter() {
        FilterRegistrationBean<RequestSizeFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestSizeFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    public static class RequestSizeFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            
            long contentLength = request.getContentLengthLong();
            
            // Check if request is too large
            if (contentLength > 0) {
                String contentType = request.getContentType();
                long maxSize = (contentType != null && contentType.contains("multipart")) 
                    ? MAX_MULTIPART_SIZE 
                    : MAX_REQUEST_SIZE;
                
                if (contentLength > maxSize) {
                    response.setStatus(413); // Payload Too Large
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Request entity too large. Maximum size: " + 
                        (maxSize / 1024 / 1024) + "MB\"}");
                    return;
                }
            }
            
            filterChain.doFilter(request, response);
        }
    }
}

