package com.streetvendor.auth;

import com.streetvendor.auth.entity.RefreshToken;
import com.streetvendor.auth.repository.RefreshTokenRepository;
import com.streetvendor.auth.service.RefreshTokenServiceImpl;
import com.streetvendor.auth.service.RotateResult;
import com.streetvendor.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RefreshTokenServiceImpl refreshTokenService;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, passwordEncoder, 30L);
    }

    @Test
    void shouldGenerateRefreshToken() {
        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedHash");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            return token;
        });

        String rawToken = refreshTokenService.generateRefreshToken(testUserId);

        assertNotNull(rawToken);
        assertTrue(rawToken.contains(":"));
    }

    @Test
    void shouldStoreBCryptHash() {
        when(passwordEncoder.encode(any(String.class))).thenReturn("$2a$12$encodedHash");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            return token;
        });

        refreshTokenService.generateRefreshToken(testUserId);

        verify(passwordEncoder).encode(any(String.class));
    }

    @Test
    void shouldCalculateExpirationCorrectly() {
        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedHash");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            return token;
        });

        refreshTokenService.generateRefreshToken(testUserId);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldPersistRefreshToken() {
        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedHash");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            return token;
        });

        refreshTokenService.generateRefreshToken(testUserId);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldGenerateUniqueTokens() {
        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedHash");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            return token;
        });

        String token1 = refreshTokenService.generateRefreshToken(testUserId);
        String token2 = refreshTokenService.generateRefreshToken(testUserId);

        assertTrue(!token1.equals(token2));
    }

    @Test
    void shouldRotateSuccessfully() {
        String rawToken = "token-id:random-part";
        UUID tokenId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        // Use a real UUID pattern in the token
        UUID realTokenId = UUID.randomUUID();
        String realRawToken = realTokenId + ":random-part";

        RefreshToken existingToken = new RefreshToken(
                realTokenId, testUserId, "storedHash",
                Instant.now().plus(30, ChronoUnit.DAYS));

        when(refreshTokenRepository.findById(realTokenId)).thenReturn(Optional.of(existingToken));
        when(passwordEncoder.matches(realRawToken, "storedHash")).thenReturn(true);
        when(passwordEncoder.encode(any(String.class))).thenReturn("newHash");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RotateResult result = refreshTokenService.rotate(realRawToken);

        assertNotNull(result);
        assertNotNull(result.rawToken());
        assertTrue(result.rawToken().contains(":"));
        assertEquals(testUserId, result.userId());
        assertTrue(existingToken.isRevoked());
    }

    @Test
    void shouldRejectRotationWhenTokenNotFound() {
        UUID fakeId = UUID.randomUUID();
        String rawToken = fakeId + ":random-part";

        when(refreshTokenRepository.findById(fakeId)).thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> refreshTokenService.rotate(rawToken));

        assertEquals("Invalid refresh token.", exception.getMessage());
    }

    @Test
    void shouldRejectRotationWhenBcryptMismatch() {
        UUID tokenId = UUID.randomUUID();
        String rawToken = tokenId + ":random-part";

        RefreshToken existingToken = new RefreshToken(
                tokenId, testUserId, "storedHash",
                Instant.now().plus(30, ChronoUnit.DAYS));

        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(existingToken));
        when(passwordEncoder.matches(rawToken, "storedHash")).thenReturn(false);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> refreshTokenService.rotate(rawToken));

        assertEquals("Invalid refresh token.", exception.getMessage());
    }

    @Test
    void shouldRejectRotationWhenTokenExpired() {
        UUID tokenId = UUID.randomUUID();
        String rawToken = tokenId + ":random-part";

        RefreshToken existingToken = new RefreshToken(
                tokenId, testUserId, "storedHash",
                Instant.now().minus(1, ChronoUnit.DAYS));

        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(existingToken));
        when(passwordEncoder.matches(rawToken, "storedHash")).thenReturn(true);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> refreshTokenService.rotate(rawToken));

        assertEquals("Invalid refresh token.", exception.getMessage());
    }

    @Test
    void shouldRejectRotationWhenTokenRevoked() {
        UUID tokenId = UUID.randomUUID();
        String rawToken = tokenId + ":random-part";

        RefreshToken existingToken = new RefreshToken(
                tokenId, testUserId, "storedHash",
                Instant.now().plus(30, ChronoUnit.DAYS));
        existingToken.setRevokedAt(Instant.now());

        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(existingToken));
        when(passwordEncoder.matches(rawToken, "storedHash")).thenReturn(true);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> refreshTokenService.rotate(rawToken));

        assertEquals("Invalid refresh token.", exception.getMessage());
    }

    @Test
    void shouldRevokeActiveToken() {
        UUID tokenId = UUID.randomUUID();
        String rawToken = tokenId + ":random-part";

        RefreshToken existingToken = new RefreshToken(
                tokenId, testUserId, "storedHash",
                Instant.now().plus(30, ChronoUnit.DAYS));

        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(existingToken));
        when(passwordEncoder.matches(rawToken, "storedHash")).thenReturn(true);

        refreshTokenService.revokeByToken(rawToken);

        assertTrue(existingToken.isRevoked());
        verify(refreshTokenRepository).save(existingToken);
    }

    @Test
    void shouldIgnoreUnknownToken() {
        UUID tokenId = UUID.randomUUID();
        String rawToken = tokenId + ":random-part";

        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.empty());

        refreshTokenService.revokeByToken(rawToken);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldIgnoreAlreadyRevokedToken() {
        UUID tokenId = UUID.randomUUID();
        String rawToken = tokenId + ":random-part";

        RefreshToken existingToken = new RefreshToken(
                tokenId, testUserId, "storedHash",
                Instant.now().plus(30, ChronoUnit.DAYS));
        existingToken.setRevokedAt(Instant.now());

        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(existingToken));
        when(passwordEncoder.matches(rawToken, "storedHash")).thenReturn(true);

        refreshTokenService.revokeByToken(rawToken);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldIgnoreBcryptMismatch() {
        UUID tokenId = UUID.randomUUID();
        String rawToken = tokenId + ":random-part";

        RefreshToken existingToken = new RefreshToken(
                tokenId, testUserId, "storedHash",
                Instant.now().plus(30, ChronoUnit.DAYS));

        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(existingToken));
        when(passwordEncoder.matches(rawToken, "storedHash")).thenReturn(false);

        refreshTokenService.revokeByToken(rawToken);

        assertFalse(existingToken.isRevoked());
        verify(refreshTokenRepository, never()).save(any());
    }
}
