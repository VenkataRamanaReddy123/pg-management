package com.pgapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SecurityConfig {

    /**
     * Configure HTTP security rules.
     * 
     * CSRF protection is disabled for simplicity.
     * Public access is allowed to static resources, login, register,
     * password change, OTP sending, and password reset endpoints.
     * All other requests are also permitted (can be restricted later if needed).
     * Frame options are disabled to allow H2 console or iframe embedding if required.
     *
     * @param http the HttpSecurity to modify
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()  // Disable CSRF protection
            .authorizeRequests()
                .antMatchers(
                    "/css/**", "/webjars/**",   // Static resources
                    "/", "/login", "/register", // Public pages
                    "/change-password", "/send-otp", "/reset-password" // Password-related endpoints
                ).permitAll()  // Allow public access
                .anyRequest().permitAll() // All other requests are allowed (can restrict later)
            .and()
            .headers().frameOptions().disable(); // Disable frame options (useful for H2 console)

        return http.build();
    }

    /**
     * Provides a BCryptPasswordEncoder bean for password hashing.
     * BCrypt is a strong hashing algorithm recommended for storing passwords securely.
     *
     * @return a BCryptPasswordEncoder instance
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
