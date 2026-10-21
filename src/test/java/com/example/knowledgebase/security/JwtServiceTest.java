package com.example.knowledgebase.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 60000; // 1 minute

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Should generate valid token with correct claims")
    void testGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        String email = "alice@example.com";

        String token = jwtService.generateToken(userId, email);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(email, jwtService.extractEmail(token));
        assertEquals(userId, jwtService.extractUserId(token));
    }

    @Test
    @DisplayName("Should reject expired token")
    void testExpiredToken() {
        // Create service with negative expiration
        JwtService expiredJwtService = new JwtService(TEST_SECRET, -1000);
        UUID userId = UUID.randomUUID();
        String token = expiredJwtService.generateToken(userId, "bob@example.com");

        assertFalse(jwtService.validateToken(token));
    }

    @Test
    @DisplayName("Should reject tampered token")
    void testTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "charlie@example.com");
        String tamperedToken = token + "xyz";

        assertFalse(jwtService.validateToken(tamperedToken));
    }
}
