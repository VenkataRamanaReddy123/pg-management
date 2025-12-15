package com.pgapp.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SecurityService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Hash plain text password
    public String hash(String plain) {
        return encoder.encode(plain);
    }

    // Verify plain text password with hash
    public boolean matches(String plain, String hash) {
        return encoder.matches(plain, hash);
    }

    // Generate a random token for password reset
    public String generateRandomToken() {
        return UUID.randomUUID().toString();
    }
}
