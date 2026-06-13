package com.streetvendor.auth.service;

import org.springframework.stereotype.Service;

@Service
public class LogoutServiceImpl implements LogoutService {

    private final RefreshTokenService refreshTokenService;

    public LogoutServiceImpl(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeByToken(rawRefreshToken);
    }
}
