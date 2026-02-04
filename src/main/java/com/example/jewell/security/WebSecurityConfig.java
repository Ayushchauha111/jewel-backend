package com.example.jewell.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.example.jewell.security.jwt.AuthEntryPointJwt;
import com.example.jewell.security.jwt.AuthTokenFilter;
import com.example.jewell.security.services.UserDetailsServiceImpl;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {
    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                                // Static resources (uploaded images)
                                .requestMatchers("/uploads/**").permitAll()
                                // Specific auth endpoints that require authentication (roles checked by @PreAuthorize)
                                .requestMatchers("/api/auth/user", "/api/auth/mod", "/api/auth/admin", "/api/auth/validate", "/api/auth/sessions", "/api/auth/sessions/**", "/api/auth/logout").authenticated()
                                // Public auth endpoints
                                .requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/api/health/**").permitAll()
                                .requestMatchers("/api/payment/**").permitAll()
                                // Jewelry shop public endpoints
                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/stock").permitAll()
                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/stock/**").permitAll()
                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/gold-price/**").permitAll()
                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/silver-price/**").permitAll()
                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/rates/**").permitAll()
                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/config/category-making").permitAll()
                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/live-rates/**").permitAll()
                                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/orders").permitAll()
                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/orders/**").permitAll()
                                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/orders/*/payment").permitAll()
                                // Gold Mine 10+1: admin-only pay/redeem/cancel; public GET and enrollment
                                .requestMatchers("/api/gold-mine/plans/*/installments/*/pay").hasRole("ADMIN")
                                .requestMatchers("/api/gold-mine/plans/*/redeem").hasRole("ADMIN")
                                .requestMatchers("/api/gold-mine/plans/*/cancel").hasRole("ADMIN")
                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/gold-mine/**").permitAll()
                                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/gold-mine/plans").permitAll()
                                // Customer endpoints - public for checkout flow (must come before general rule)
                                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/customers").permitAll()
                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/customers/phone/**").permitAll()
                                // Jewelry shop admin endpoints
                                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/stock/**").hasRole("ADMIN")
                                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/stock/**").hasRole("ADMIN")
                                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/stock/**").hasRole("ADMIN")
                                .requestMatchers("/api/billing/**").hasRole("ADMIN")
                                // Customer endpoints - authenticated access (general rule, comes after specific public rules)
                                .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "USER")
                                .requestMatchers("/api/credits/**").hasRole("ADMIN")
                                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/gold-price/**").hasRole("ADMIN")
                                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/silver-price/**").hasRole("ADMIN")
                                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/rates/**").hasRole("ADMIN")
                                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/orders/**").hasRole("ADMIN")
                                .requestMatchers("/api/analytics/**").hasRole("ADMIN")
                                .requestMatchers("/api/income-expense/**").hasRole("ADMIN")
                                .requestMatchers("/api/referral/**").permitAll()
                                .requestMatchers("/api/user/**").permitAll()
                                .requestMatchers("/api/email/**").permitAll()
                                .requestMatchers("/api/otp/**").permitAll()
                                // Reject requests to /customers (without /api) - should use /api/customers
                                .requestMatchers("/customers", "/customers/**").denyAll()
                                .requestMatchers("/api/password/**").permitAll()
                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/coupons").permitAll()
                                .requestMatchers("/api/coupons/validate").permitAll()
                                .requestMatchers("/api/coupons/**").hasRole("ADMIN")
                                .requestMatchers("/api/feedback/**").permitAll()
                                .requestMatchers("/api/api-config/**").hasRole("ADMIN")
                                .requestMatchers("/api/sitemap/**").permitAll() // Sitemap endpoint for SEO
                                .requestMatchers("/api/feature-flags/check/**").permitAll()
                                .requestMatchers("/api/feature-flags/all").permitAll()
                                .requestMatchers("/api/feature-flags/admin/**").hasRole("ADMIN")
                                .requestMatchers("/api/admin/dashboard").hasRole("ADMIN") // Dashboard overview (single API)
                                .requestMatchers("/api/admin/roles/my-roles").authenticated() // Anyone can check their own roles
                                .requestMatchers("/api/admin/roles/**").hasAnyRole("ADMIN", "SUPER_ADMIN") // Role management
                                .requestMatchers("/api/super-admin/**").hasRole("SUPER_ADMIN") // Super Admin only
                                .requestMatchers("/api/institution-admin/**").hasAnyRole("INSTITUTION_ADMIN", "SUPER_ADMIN") // Institution Admin
                                .requestMatchers("/api/institutional/**").hasAnyRole("ADMIN", "MODERATOR", "INSTITUTION_ADMIN", "SUPER_ADMIN") // Institutional features
                                .requestMatchers("/error").permitAll() // Allow access to error page
                                .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Only allow specific origins (not wildcard)
        config.addAllowedOrigin("http://localhost:8000");
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("https://gangajewellers.netlify.app");
        config.addAllowedOrigin("http://gangajewellers.in");
        config.addAllowedOrigin("https://gangajewellers.in");
        config.addAllowedOrigin("https://www.gangajewellers.in");
        config.addAllowedOrigin("https://api.gangajewellers.in");
        
        // Only allow specific HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Only allow specific headers (not wildcard)
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "x-access-token",
            "X-Device-Id"
        ));
        
        // Expose only necessary headers
        config.setExposedHeaders(List.of(
            "Authorization",
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "Retry-After"
        ));
        
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    // Resource handlers are now in WebMvcSecurityConfig
}