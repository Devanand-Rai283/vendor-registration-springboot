package com.streetvendor.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Abstract base class for security-focused integration tests.
 *
 * <p>Extends {@link AbstractIntegrationTest} to provide security-aware MockMvc
 * configuration with Spring Security filter chain applied.
 *
 * <p>Provides:
 * <ul>
 *   <li>All infrastructure from {@link AbstractIntegrationTest}</li>
 *   <li>Security-aware MockMvc with Spring Security filter chain</li>
 *   <li>Foundation for future authenticated-request helpers</li>
 * </ul>
 *
 * <p>This class does NOT implement authentication helpers or introduce
 * {@code @WithMockUser} usage. It establishes the reusable structure
 * for future security testing infrastructure.
 *
 * <p>Usage:
 * <pre>{@code
 * @ActiveProfiles("security-test")
 * class SecurityConfigTest extends AbstractSecurityTest {
 *
 *     @Test
 *     void shouldReturnUnauthorizedForProtectedEndpoint() throws Exception {
 *         mockMvc.perform(get("/api/protected"))
 *             .andExpect(status().isUnauthorized());
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractSecurityTest extends AbstractIntegrationTest {

    @BeforeEach
    void setUpSecurityMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }
}
