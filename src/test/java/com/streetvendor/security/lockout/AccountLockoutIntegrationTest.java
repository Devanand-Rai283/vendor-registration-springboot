package com.streetvendor.security.lockout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.dto.LoginRequest;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.common.audit.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@ActiveProfiles("lockout-test")
class AccountLockoutIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private static String redisHost() {
        String host = redis.getHost();
        return "localhost".equals(host) ? "127.0.0.1" : host;
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", AccountLockoutIntegrationTest::redisHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.ping-on-startup", () -> "false");
        registry.add("account.lock.threshold", () -> "5");
        registry.add("account.lock.duration-minutes", () -> "1");
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String EMAIL = "lockout-test@example.com";
    private static final String PASSWORD = "Password1!";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        redisTemplate.delete("ratelimit:127.0.0.1:login");
        userRepository.deleteAll();
        auditLogRepository.deleteAll();

        User user = new User(
                UUID.randomUUID(),
                EMAIL,
                passwordEncoder.encode(PASSWORD),
                Role.CUSTOMER,
                AccountStatus.ACTIVE
        );
        userRepository.save(user);

        User successClearUser = new User(
                UUID.randomUUID(),
                "success-clear@example.com",
                passwordEncoder.encode(PASSWORD),
                Role.CUSTOMER,
                AccountStatus.ACTIVE
        );
        userRepository.save(successClearUser);

        User isolatedA = new User(
                UUID.randomUUID(),
                "isolated-a@example.com",
                passwordEncoder.encode(PASSWORD),
                Role.CUSTOMER,
                AccountStatus.ACTIVE
        );
        userRepository.save(isolatedA);

        User isolatedB = new User(
                UUID.randomUUID(),
                "isolated-b@example.com",
                passwordEncoder.encode(PASSWORD),
                Role.CUSTOMER,
                AccountStatus.ACTIVE
        );
        userRepository.save(isolatedB);

        User rateLimitUser = new User(
                UUID.randomUUID(),
                "ratelimit-lockout@example.com",
                passwordEncoder.encode(PASSWORD),
                Role.CUSTOMER,
                AccountStatus.ACTIVE
        );
        userRepository.save(rateLimitUser);
    }

    @Test
    void attempts1To4ShouldReturn401() throws Exception {
        for (int i = 0; i < 4; i++) {
            LoginRequest request = new LoginRequest(EMAIL, "WrongPassword!");
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }
    }

    @Test
    void attempt5ShouldReturn403() throws Exception {
        for (int i = 0; i < 4; i++) {
            LoginRequest request = new LoginRequest(EMAIL, "WrongPassword!");
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        LoginRequest request = new LoginRequest(EMAIL, "WrongPassword!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Account temporarily locked")));
    }

    @Test
    void lockedAccountReturns403EvenWithCorrectPassword() throws Exception {
        for (int i = 0; i < 5; i++) {
            LoginRequest request = new LoginRequest(EMAIL, "WrongPassword!");
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void lockedAccountReturns403WithIncorrectPassword() throws Exception {
        for (int i = 0; i < 5; i++) {
            LoginRequest request = new LoginRequest(EMAIL, "WrongPassword!");
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        LoginRequest request = new LoginRequest(EMAIL, "AnotherWrongPassword!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void successfulLoginClearsCounter() throws Exception {
        String email = "success-clear@example.com";
        String userPassword = "Password1!";

        for (int i = 0; i < 4; i++) {
            LoginRequest request = new LoginRequest(email, "WrongPassword!");
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        assertTrue(redisTemplate.hasKey("lockout:" + email));

        LoginRequest request = new LoginRequest(email, userPassword);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertFalse(redisTemplate.hasKey("lockout:" + email));
    }

    @Test
    void lockingOneEmailDoesNotAffectAnother() throws Exception {
        String emailA = "isolated-a@example.com";
        String emailB = "isolated-b@example.com";

        for (int i = 0; i < 5; i++) {
            LoginRequest request = new LoginRequest(emailA, "WrongPassword!");
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(emailA, "WrongPassword!"))))
                .andExpect(status().isForbidden());

        LoginRequest requestB = new LoginRequest(emailB, "WrongPassword!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestB)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rateLimitingStillFunctions() throws Exception {
        String ip = "192.168.99.99";
        String email = "ratelimit-lockout@example.com";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest(email, "WrongPassword!")))
                    .header("X-Forwarded-For", ip));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "WrongPassword!")))
                        .header("X-Forwarded-For", ip))
                .andExpect(status().isTooManyRequests());
    }
}
