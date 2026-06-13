package com.streetvendor.auth.service;

import java.util.UUID;

public interface RefreshTokenService {

    String generateRefreshToken(UUID userId);

    RotateResult rotate(String rawToken);

    void revokeByToken(String rawToken);
}
