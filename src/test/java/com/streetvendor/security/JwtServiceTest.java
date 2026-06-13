package com.streetvendor.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;
    private UUID testUserId;
    private String testEmail;
    private String testRole;

    @BeforeEach
    void setUp() {
        String secret = "dGhpc0lzQVZlcnlTZWN1dGVLZXlGb3JKV1RUb2tlbkdlbmVyYXRpb24=";
        long accessExpirationMs = 900000L;
        jwtService = new JwtService(secret, accessExpirationMs);
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        testRole = "CUSTOMER";
    }

    @Test
    void shouldGenerateValidToken() {
        String token = jwtService.generateAccessToken(testUserId, testEmail, testRole);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldExtractUserId() {
        String token = jwtService.generateAccessToken(testUserId, testEmail, testRole);

        String extractedUserId = jwtService.extractUserId(token);

        assertEquals(testUserId.toString(), extractedUserId);
    }

    @Test
    void shouldExtractEmail() {
        String token = jwtService.generateAccessToken(testUserId, testEmail, testRole);

        String extractedEmail = jwtService.extractEmail(token);

        assertEquals(testEmail, extractedEmail);
    }

    @Test
    void shouldExtractRole() {
        String token = jwtService.generateAccessToken(testUserId, testEmail, testRole);

        String extractedRole = jwtService.extractRole(token);

        assertEquals(testRole, extractedRole);
    }

    @Test
    void shouldValidateToken() {
        String token = jwtService.generateAccessToken(testUserId, testEmail, testRole);

        boolean isValid = jwtService.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void shouldRejectInvalidToken() {
        boolean isValid = jwtService.validateToken("invalid-token");

        assertFalse(isValid);
    }

    @Test
    void shouldRejectExpiredToken() {
        String secret = "dGhpc0lzQVZlcnlTZWN1dGVLZXlGb3JKV1RUb2tlbkdlbmVyYXRpb24=";
        JwtService expiredJwtService = new JwtService(secret, -1L);

        String token = expiredJwtService.generateAccessToken(testUserId, testEmail, testRole);

        boolean isValid = jwtService.validateToken(token);

        assertFalse(isValid);
    }
}
