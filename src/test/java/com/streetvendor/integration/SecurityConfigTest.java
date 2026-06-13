package com.streetvendor.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("security-test")
class SecurityConfigTest extends AbstractSecurityTest {

    @Autowired
    private JwtService jwtService;

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
    void jwtServiceExtractUsernameShouldThrowUnsupportedOperationException() {
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> jwtService.extractUsername("test-token")
        );
        assertEquals("JWT implementation scheduled for AUTH phase", exception.getMessage());
    }

    @Test
    void jwtServiceValidateTokenShouldThrowUnsupportedOperationException() {
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> jwtService.validateToken("test-token")
        );
        assertEquals("JWT implementation scheduled for AUTH phase", exception.getMessage());
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
}
