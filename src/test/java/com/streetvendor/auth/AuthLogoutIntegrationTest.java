package com.streetvendor.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.dto.LoginRequest;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.RefreshToken;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.RefreshTokenRepository;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.security.ratelimit.RateLimitService;
import com.streetvendor.support.AbstractSecurityTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("auth-test")
@Transactional
@Import({AuthTestRedisConfig.class, AuthLogoutIntegrationTest.LogoutTestConfig.class})
class AuthLogoutIntegrationTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;
    private String validJwt;
    private String rawRefreshToken;

    @BeforeEach
    void setUp() throws Exception {
        String encodedPassword = passwordEncoder.encode("Password1!");
        testUser = new User(
                UUID.randomUUID(),
                "logout-user@example.com",
                encodedPassword,
                Role.CUSTOMER,
                AccountStatus.ACTIVE
        );
        userRepository.save(testUser);

        validJwt = jwtService.generateAccessToken(
                testUser.getId(),
                testUser.getEmail(),
                testUser.getRole().name()
        );

        LoginRequest loginRequest = new LoginRequest("logout-user@example.com", "Password1!");

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        jakarta.servlet.http.Cookie refreshCookie = result.getResponse().getCookie("refreshToken");
        assertNotNull(refreshCookie);
        rawRefreshToken = refreshCookie.getValue();
    }

    @Test
    void shouldRevokeActiveRefreshTokenOnLogout() throws Exception {
        List<RefreshToken> tokensBefore = refreshTokenRepository.findByUserId(testUser.getId());
        assertEquals(1, tokensBefore.size());
        assertTrue(!tokensBefore.get(0).isRevoked());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validJwt)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                .andExpect(status().isOk());

        List<RefreshToken> tokensAfter = refreshTokenRepository.findByUserId(testUser.getId());
        assertEquals(1, tokensAfter.size());
        assertTrue(tokensAfter.get(0).isRevoked());
    }

    @Test
    void shouldClearRefreshCookieOnLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validJwt)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/auth"));
    }

    @Test
    void shouldReturn200OnLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validJwt)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    void shouldFailRefreshWithOldTokenAfterLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validJwt)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid refresh token."));
    }

    @Test
    void shouldReturn200OnRepeatedLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validJwt)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validJwt)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @TestConfiguration
    static class LogoutTestConfig {

        @Bean
        public RateLimitService rateLimitService() {
            return mock(RateLimitService.class);
        }
    }
}
