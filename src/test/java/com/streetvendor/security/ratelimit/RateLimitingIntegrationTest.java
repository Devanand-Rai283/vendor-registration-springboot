package com.streetvendor.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("ratelimit-test")
class RateLimitingIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.ping-on-startup", () -> "false");
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private static final String REGISTER_BODY_TEMPLATE = "{\"email\":\"rl-it-reg-%d@example.com\",\"password\":\"TestPass123!\",\"role\":\"CUSTOMER\",\"fullName\":\"Test User\"}";

    private static final String LOGIN_BODY_TEMPLATE = "{\"email\":\"rl-it-login-%d@example.com\",\"password\":\"TestPass123!\"}";

    @Test
    void loginRequests1to10ShouldNotBeRateLimited() throws Exception {
        String ip = "10.0.0.1";
        String body = LOGIN_BODY_TEMPLATE.formatted(1);
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .header("X-Forwarded-For", ip))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void loginRequest11ShouldReturn429() throws Exception {
        String ip = "10.0.0.2";
        String body = LOGIN_BODY_TEMPLATE.formatted(2);
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .header("X-Forwarded-For", ip));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-Forwarded-For", ip))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."));
    }

    @Test
    void registerRequests1to5ShouldNotBeRateLimited() throws Exception {
        String ip = "10.0.0.3";
        String body = REGISTER_BODY_TEMPLATE.formatted(3);
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .header("X-Forwarded-For", ip));
        }
    }

    @Test
    void registerRequest6ShouldReturn429() throws Exception {
        String ip = "10.0.0.4";
        String body = REGISTER_BODY_TEMPLATE.formatted(4);
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .header("X-Forwarded-For", ip));
        }

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-Forwarded-For", ip))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."));
    }

    @Test
    void discoveryEndpointGetNearbyExceedingLimitShouldReturn429() throws Exception {
        String ip = "10.0.0.5";

        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get("/api/vendors/nearby")
                    .param("lat", "12.97")
                    .param("lng", "77.59")
                    .param("radius", "5.0")
                    .header("X-Forwarded-For", ip));
        }

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "12.97")
                        .param("lng", "77.59")
                        .param("radius", "5.0")
                        .header("X-Forwarded-For", ip))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void discoveryEndpointSearchExceedingLimitShouldReturn429() throws Exception {
        String ip = "10.0.0.6";

        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get("/api/search")
                    .param("keyword", "pizza")
                    .header("X-Forwarded-For", ip));
        }

        mockMvc.perform(get("/api/search")
                        .param("keyword", "pizza")
                        .header("X-Forwarded-For", ip))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void retryAfterHeaderPresentOn429() throws Exception {
        String ip = "10.0.0.7";
        String body = LOGIN_BODY_TEMPLATE.formatted(7);
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .header("X-Forwarded-For", ip));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-Forwarded-For", ip))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("Retry-After", matchesPattern("\\d+")));
    }

    @Test
    void differentIpsHaveIndependentCounters() throws Exception {
        String exhaustedIp = "10.0.0.8";
        String freshIp = "10.0.0.9";
        String body = LOGIN_BODY_TEMPLATE.formatted(8);

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .header("X-Forwarded-For", exhaustedIp));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-Forwarded-For", exhaustedIp))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-Forwarded-For", freshIp))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonRatedEndpointsNotAffected() throws Exception {
        String ip = "10.0.0.11";

        mockMvc.perform(get("/actuator/health")
                        .header("X-Forwarded-For", ip))
                .andExpect(status().isOk());
    }

    @Test
    void redisContainerIsRunning() {
        assertNotNull(redisTemplate);
    }
}
