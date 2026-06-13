package com.streetvendor.auth.service;

import com.streetvendor.auth.entity.RefreshToken;
import com.streetvendor.auth.repository.RefreshTokenRepository;
import com.streetvendor.common.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final long refreshExpirationDays;
    private final SecureRandom secureRandom;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            @Value("${jwt.refresh-expiration}") long refreshExpirationDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshExpirationDays = refreshExpirationDays;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String generateRefreshToken(UUID userId) {
        byte[] tokenBytes = new byte[16];
        secureRandom.nextBytes(tokenBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        UUID tokenId = UUID.randomUUID();
        String rawToken = tokenId + ":" + randomPart;
        String tokenHash = passwordEncoder.encode(rawToken);

        Instant expiresAt = Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken(tokenId, userId, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Override
    @Transactional
    public void revokeByToken(String rawToken) {
        try {
            String[] parts = rawToken.split(":", 2);
            UUID tokenId = UUID.fromString(parts[0]);

            RefreshToken token = refreshTokenRepository.findById(tokenId).orElse(null);
            if (token == null) {
                return;
            }

            if (!passwordEncoder.matches(rawToken, token.getTokenHash())) {
                return;
            }

            if (token.isRevoked()) {
                return;
            }

            if (token.isExpired()) {
                return;
            }

            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        } catch (Exception e) {
        }
    }

    @Override
    @Transactional
    public RotateResult rotate(String rawToken) {
        String[] parts = rawToken.split(":", 2);
        UUID tokenId = UUID.fromString(parts[0]);

        RefreshToken token = refreshTokenRepository.findById(tokenId)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token."));

        if (!passwordEncoder.matches(rawToken, token.getTokenHash())) {
            throw new UnauthorizedException("Invalid refresh token.");
        }

        if (token.isExpired()) {
            throw new UnauthorizedException("Invalid refresh token.");
        }

        if (token.isRevoked()) {
            throw new UnauthorizedException("Invalid refresh token.");
        }

        UUID userId = token.getUserId();
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);

        String newRawToken = generateRefreshToken(userId);
        return new RotateResult(newRawToken, userId);
    }
}
