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
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
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
@Import(AuthTestRedisConfig.class)
class AuthRefreshIntegrationTest extends AbstractIntegrationTest {

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
    private String rawRefreshToken;

    @BeforeEach
    void setUp() throws Exception {
        String encodedPassword = passwordEncoder.encode("Password1!");
        testUser = new User(
                UUID.randomUUID(),
                "refresh-user@example.com",
                encodedPassword,
                Role.CUSTOMER,
                AccountStatus.ACTIVE
        );
        userRepository.save(testUser);

        LoginRequest loginRequest = new LoginRequest("refresh-user@example.com", "Password1!");

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
    void shouldRotateSuccessfully() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/auth"));
    }

    @Test
    void shouldReturnNewAccessToken() throws Exception {
        var result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertTrue(json.contains("accessToken"));

        String accessToken = objectMapper.readTree(json).get("data").get("accessToken").asText();
        assertNotNull(accessToken);
        assertFalse(accessToken.isEmpty());

        String extractedUserId = jwtService.extractUserId(accessToken);
        assertEquals(testUser.getId().toString(), extractedUserId);
    }

    @Test
    void shouldReturnNewCookie() throws Exception {
        var result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        jakarta.servlet.http.Cookie newCookie = result.getResponse().getCookie("refreshToken");
        assertNotNull(newCookie);
        assertNotNull(newCookie.getValue());
        assertFalse(newCookie.getValue().isEmpty());
        assertTrue(newCookie.isHttpOnly());
        assertTrue(newCookie.getSecure());
        assertEquals("/api/auth", newCookie.getPath());
    }

    @Test
    void shouldRejectOldTokenAfterRotation() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
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
    void shouldRevokeOldTokenInDatabase() throws Exception {
        List<RefreshToken> tokensBefore = refreshTokenRepository.findByUserId(testUser.getId());
        assertEquals(1, tokensBefore.size(), "Should have exactly one token before refresh");
        assertFalse(tokensBefore.get(0).isRevoked(), "Token should not be revoked before refresh");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                .andExpect(status().isOk());

        List<RefreshToken> tokensAfter = refreshTokenRepository.findByUserId(testUser.getId());
        assertEquals(2, tokensAfter.size(), "Should have two tokens after refresh (old revoked, new active)");

        RefreshToken oldToken = tokensAfter.stream()
                .filter(t -> t.getTokenHash().startsWith("$2a$"))
                .filter(t -> t.isRevoked())
                .findFirst()
                .orElse(null);
        assertNotNull(oldToken, "Old token should be revoked");

        RefreshToken newToken = tokensAfter.stream()
                .filter(t -> !t.isRevoked())
                .findFirst()
                .orElse(null);
        assertNotNull(newToken, "New token should not be revoked");
    }
}
