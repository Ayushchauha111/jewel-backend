package com.example.jewell.security.jwt;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.jewell.security.services.UserDetailsServiceImpl;
import com.example.jewell.service.AdminSessionService;

public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AdminSessionService adminSessionService;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null) {
                try {
                    // Only try to authenticate if token is valid
                    if (jwtUtils.validateJwtToken(jwt)) {
                        String username = jwtUtils.getUserNameFromJwtToken(jwt);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        Long userId = ((com.example.jewell.security.services.UserDetailsImpl) userDetails).getId();
                        boolean isAdmin = userDetails.getAuthorities().stream()
                                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));

                        Long sessionId = jwtUtils.getSessionIdFromJwtToken(jwt);
                        if (isAdmin && sessionId != null) {
                            // Admin with session: validate session still exists (device limit / eviction)
                            if (!adminSessionService.isSessionValid(sessionId, userId)) {
                                logger.debug("Admin session {} invalid or evicted for user {}", sessionId, username);
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Session expired or maximum devices reached. Please login again.\",\"status\":401}");
                                return;
                            }
                            adminSessionService.refreshLastUsedAt(sessionId, userId);
                        } else if (!isAdmin) {
                            // Non-admin: check token version (single device)
                            com.example.jewell.model.User user = userDetailsService.getUserByUsername(username);
                            if (user != null) {
                                Long tokenVersion = jwtUtils.getTokenVersionFromJwtToken(jwt);
                                if (tokenVersion != null) {
                                    Long currentTokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
                                    if (!tokenVersion.equals(currentTokenVersion)) {
                                        logger.debug("Token version mismatch for user {}: token version {} != current version {}",
                                            username, tokenVersion, currentTokenVersion);
                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                        response.setContentType("application/json");
                                        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Session expired. Please login again.\",\"status\":401}");
                                        return;
                                    }
                                }
                            }
                        }
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (Exception e) {
                    // Token validation failed - just continue without auth
                    // This allows public endpoints to still work with expired tokens
                    logger.debug("JWT validation failed, continuing without auth: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
            // Don't throw - just continue without auth for public endpoints
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
