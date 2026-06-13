package com.streetvendor.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.dto.LoginRequest;
import com.streetvendor.auth.dto.LoginResult;
import com.streetvendor.auth.dto.LoginResponse;
import com.streetvendor.auth.dto.RegisterRequest;
import com.streetvendor.auth.dto.RegisterResponse;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.service.AuthService;
import com.streetvendor.auth.service.LogoutService;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("auth-test")
class AuthControllerTest extends AbstractIntegrationTest {

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private LogoutService logoutService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturn201OnSuccessfulRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "Password1!",
                Role.CUSTOMER,
                "John Doe",
                "1234567890",
                "123 Main St",
                null,
                null
        );
        RegisterResponse response = new RegisterResponse(UUID.randomUUID(), "test@example.com", Role.CUSTOMER, AccountStatus.ACTIVE);

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
    }

    @Test
    void shouldReturn400OnValidationFailure() throws Exception {
        RegisterRequest request = new RegisterRequest("", "", null, null, null, null, null, null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn403OnAdminRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "admin@example.com",
                "Password1!",
                Role.ADMIN,
                null,
                null,
                null,
                null,
                null
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ForbiddenException("Admin registration is not allowed"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Admin registration is not allowed"));
    }

    @Test
    void shouldReturn409OnDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com",
                "Password1!",
                Role.CUSTOMER,
                null,
                null,
                null,
                null,
                null
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ConflictException("Email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void shouldReturn400OnInvalidEmailFormat() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "invalid-email",
                "Password1!",
                Role.CUSTOMER,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn400OnWeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "weak",
                Role.CUSTOMER,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn200OnSuccessfulLogin() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Password1!");
        LoginResponse response = new LoginResponse("jwt-token", "Bearer", 900);
        LoginResult result = new LoginResult(response, "some-refresh-token");

        when(authService.login(any(LoginRequest.class))).thenReturn(result);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900));
    }

    @Test
    void shouldReturn400OnLoginValidationFailure() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn401OnInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "WrongPassword!");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid email or password."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void shouldReturn403OnLockedAccount() throws Exception {
        LoginRequest request = new LoginRequest("locked@example.com", "Password1!");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ForbiddenException("Account is locked."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Account is locked."));
    }

    @Test
    void shouldSetRefreshTokenCookieOnLogin() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Password1!");
        LoginResponse response = new LoginResponse("jwt-token", "Bearer", 900);
        LoginResult result = new LoginResult(response, "refresh-token-value");

        when(authService.login(any(LoginRequest.class))).thenReturn(result);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/auth"));
    }

    @Test
    void shouldReturn200OnSuccessfulRefresh() throws Exception {
        String refreshTokenValue = "refresh-token-id:random-data";
        LoginResponse response = new LoginResponse("new-jwt-token", "Bearer", 900);
        LoginResult result = new LoginResult(response, "new-refresh-token-id:new-data");

        when(authService.refresh(refreshTokenValue)).thenReturn(result);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshTokenValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("new-jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/auth"));
    }

    @Test
    void shouldReturn401WhenRefreshTokenCookieMissing() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid refresh token."));
    }

    @Test
    void shouldReturn401WhenRefreshTokenIsInvalid() throws Exception {
        String invalidToken = "invalid-token";

        when(authService.refresh(invalidToken))
                .thenThrow(new UnauthorizedException("Invalid refresh token."));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", invalidToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid refresh token."));
    }

    @Test
    void shouldReturn200OnLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "some-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    void shouldClearRefreshCookieOnLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "some-token")))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/auth"));
    }

    @Test
    void shouldInvokeLogoutServiceWhenCookiePresent() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "some-token")))
                .andExpect(status().isOk());

        verify(logoutService).logout("some-token");
    }

    @Test
    void shouldReturn200EvenWithoutCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }
}
