package com.streetvendor.auth;

import com.streetvendor.auth.service.LogoutService;
import com.streetvendor.auth.service.LogoutServiceImpl;
import com.streetvendor.auth.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    private LogoutService logoutService;

    @BeforeEach
    void setUp() {
        logoutService = new LogoutServiceImpl(refreshTokenService);
    }

    @Test
    void shouldDelegateToRefreshTokenService() {
        String rawToken = "some-token";

        logoutService.logout(rawToken);

        verify(refreshTokenService).revokeByToken(rawToken);
    }
}
