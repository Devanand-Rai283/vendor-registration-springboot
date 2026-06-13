package com.streetvendor.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Abstract base class for integration tests that require a full Spring
 * application context.
 *
 * <p>
 * Provides common infrastructure including:
 * <ul>
 * <li>Spring Boot application context loading</li>
 * <li>MockMvc initialization with web application context</li>
 * <li>WebApplicationContext injection</li>
 * </ul>
 *
 * <p>
 * Subclasses can customize the active profile by annotating with
 * {@code @ActiveProfiles}.
 * If no profile is specified, the default Spring profile is used.
 *
 * <p>
 * Usage:
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;ActiveProfiles("my-test-profile")
 *     class MyIntegrationTest extends AbstractIntegrationTest {
 *
 *         @Test
 *         void shouldDoSomething() throws Exception {
 *             mockMvc.perform(get("/api/resource"))
 *                     .andExpect(status().isOk());
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>
 * This class does not introduce any test data factories, authentication
 * helpers,
 * or business-specific utilities. It is intentionally minimal to serve as a
 * reusable foundation for future integration tests.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
    }
}
