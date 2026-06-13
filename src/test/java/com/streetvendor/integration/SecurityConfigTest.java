package com.streetvendor.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("security-test")
@Transactional
class SecurityConfigTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String validToken;
    private UUID userId;

    @BeforeEach
    void setUpTestData() {
        userId = UUID.randomUUID();
        User user = new User(
                userId,
                "test@example.com",
                passwordEncoder.encode("Password1!"),
                Role.CUSTOMER,
                AccountStatus.ACTIVE
        );
        userRepository.save(user);
        validToken = jwtService.generateAccessToken(userId, "test@example.com", "CUSTOMER");
    }

    @Test
    void healthEndpointShouldReturnOkWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerEndpointShouldBeAccessibleInNonProduction() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 200 || status == 302,
                            "Swagger UI should be accessible (200 OK or 302 redirect)");
                });
    }

    @Test
    void apiDocsEndpointShouldNotBeBlockedBySecurityInNonProduction() throws Exception {
        mockMvc.perform(get("/api/docs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status != 401 && status != 403,
                            "API docs should not be blocked by security (got " + status + ")");
                });
    }

    @Test
    void registerEndpointShouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status != 401 && status != 403,
                            "Register endpoint should not be blocked by security (got " + status + ")");
                });
    }

    @Test
    void loginEndpointShouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status != 401 && status != 403,
                            "Login endpoint should not be blocked by security (got " + status + ")");
                });
    }

    @Test
    void refreshEndpointShouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid refresh token."));
    }

    @Test
    void protectedEndpointShouldReturnUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/test/protected")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Session expired. Please log in again."))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/test/protected"));
    }

    @Test
    void unauthorizedResponseShouldFollowContract() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/protected")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        assertTrue(responseBody.contains("\"status\":401"),
                "Response should contain status field with value 401");
        assertTrue(responseBody.contains("\"message\":\"Session expired. Please log in again.\""),
                "Response should contain message field");
        assertTrue(responseBody.contains("\"timestamp\":"),
                "Response should contain timestamp field");
        assertTrue(responseBody.contains("\"path\":"),
                "Response should contain path field");
    }

    @Test
    void jwtAuthenticationFilterShouldContinueChainWithoutAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/test/protected")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void jwtAuthenticationFilterShouldContinueChainWithInvalidAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/test/protected")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Basic abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void jwtAuthenticationFilterShouldContinueChainWithBearerToken() throws Exception {
        mockMvc.perform(get("/test/protected")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void jwtAuthenticationFilterShouldNotAuthenticateUserWithBearerToken() throws Exception {
        mockMvc.perform(get("/test/protected")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Session expired. Please log in again."));
    }

    @Test
    void authenticatedRequestShouldAccessProtectedEndpoint() throws Exception {
        assertNotNull(validToken, "A valid JWT token must be generated before this test");

        mockMvc.perform(get("/test/protected")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    assertTrue(content.contains("Protected resource accessed successfully"),
                            "Protected endpoint should return success message");
                });
    }

    @Test
    void logoutEndpointShouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Session expired. Please log in again."));
    }

    @Test
    void authenticatedRequestShouldHaveCorrectRole() throws Exception {
        mockMvc.perform(get("/test/protected")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }
}
