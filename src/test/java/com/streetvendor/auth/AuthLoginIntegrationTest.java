package com.streetvendor.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.dto.LoginRequest;
import com.streetvendor.auth.dto.RegisterRequest;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.RefreshToken;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.RefreshTokenRepository;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("auth-test")
@Transactional
class AuthLoginIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        String encodedPassword = passwordEncoder.encode("Password1!");
        User user = new User(
                UUID.randomUUID(),
                "user@example.com",
                encodedPassword,
                Role.CUSTOMER,
                AccountStatus.ACTIVE
        );
        userRepository.save(user);
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Password1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900));
    }

    @Test
    void shouldPersistRefreshToken() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Password1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        assertFalse(refreshTokenRepository.findByUserId(user.getId()).isEmpty());
    }

    @Test
    void shouldStoreBCryptHashOfRefreshToken() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Password1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(user.getId()).get(0);

        assertNotNull(refreshToken.getTokenHash());
        assertTrue(refreshToken.getTokenHash().startsWith("$2a$"));
    }

    @Test
    void shouldNotIncludeRefreshTokenInResponseBody() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Password1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    @Test
    void shouldReturnRefreshTokenAsHttpOnlyCookie() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Password1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/auth"));
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void shouldRejectNonexistentEmail() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "Password1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void shouldRejectSuspendedAccount() throws Exception {
        String encodedPassword = passwordEncoder.encode("Password1!");
        User suspendedUser = new User(
                UUID.randomUUID(),
                "suspended@example.com",
                encodedPassword,
                Role.CUSTOMER,
                AccountStatus.SUSPENDED
        );
        userRepository.save(suspendedUser);

        LoginRequest request = new LoginRequest("suspended@example.com", "Password1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is not active."));
    }
}
