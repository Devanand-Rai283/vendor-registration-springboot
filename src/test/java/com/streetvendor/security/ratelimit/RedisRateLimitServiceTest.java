package com.streetvendor.security.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class RedisRateLimitServiceTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private RedisRateLimitService rateLimitService;
    private StringRedisTemplate redisTemplate;
    private LettuceConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        rateLimitService = new RedisRateLimitService(redisTemplate);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        connectionFactory.destroy();
    }

    @Test
    void requestBelowLoginLimitShouldSucceed() {
        String clientIp = "192.168.1.100";
        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(() -> rateLimitService.checkRateLimit(clientIp, RateLimitRule.LOGIN));
        }
    }

    @Test
    void requestAtLoginLimitShouldSucceed() {
        String clientIp = "192.168.1.101";
        for (int i = 0; i < 9; i++) {
            rateLimitService.checkRateLimit(clientIp, RateLimitRule.LOGIN);
        }
        assertDoesNotThrow(() -> rateLimitService.checkRateLimit(clientIp, RateLimitRule.LOGIN));
    }

    @Test
    void requestExceedingLoginLimitShouldFail() {
        String clientIp = "192.168.1.102";
        for (int i = 0; i < 10; i++) {
            rateLimitService.checkRateLimit(clientIp, RateLimitRule.LOGIN);
        }
        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> rateLimitService.checkRateLimit(clientIp, RateLimitRule.LOGIN));
        assertEquals(RateLimitRule.LOGIN, ex.getEndpoint());
        assertEquals(clientIp, ex.getClientIp());
        assertTrue(ex.getRetryAfterSeconds() > 0);
    }

    @Test
    void requestBelowRegisterLimitShouldSucceed() {
        String clientIp = "192.168.1.110";
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> rateLimitService.checkRateLimit(clientIp, RateLimitRule.REGISTER));
        }
    }

    @Test
    void requestExceedingRegisterLimitShouldFail() {
        String clientIp = "192.168.1.111";
        for (int i = 0; i < 5; i++) {
            rateLimitService.checkRateLimit(clientIp, RateLimitRule.REGISTER);
        }
        assertThrows(RateLimitExceededException.class,
                () -> rateLimitService.checkRateLimit(clientIp, RateLimitRule.REGISTER));
    }

    @Test
    void requestBelowDiscoveryLimitShouldSucceed() {
        String clientIp = "192.168.1.120";
        for (int i = 0; i < 60; i++) {
            assertDoesNotThrow(() -> rateLimitService.checkRateLimit(clientIp, RateLimitRule.DISCOVERY));
        }
    }

    @Test
    void requestExceedingDiscoveryLimitShouldFail() {
        String clientIp = "192.168.1.121";
        for (int i = 0; i < 60; i++) {
            rateLimitService.checkRateLimit(clientIp, RateLimitRule.DISCOVERY);
        }
        assertThrows(RateLimitExceededException.class,
                () -> rateLimitService.checkRateLimit(clientIp, RateLimitRule.DISCOVERY));
    }

    @Test
    void independentIpCounters() {
        String ipA = "192.168.1.130";
        String ipB = "192.168.1.131";

        for (int i = 0; i < 10; i++) {
            rateLimitService.checkRateLimit(ipA, RateLimitRule.LOGIN);
        }
        assertThrows(RateLimitExceededException.class,
                () -> rateLimitService.checkRateLimit(ipA, RateLimitRule.LOGIN));

        assertDoesNotThrow(() -> rateLimitService.checkRateLimit(ipB, RateLimitRule.LOGIN));
    }

    @Test
    void counterExpiresResetsLimit() throws InterruptedException {
        String clientIp = "192.168.1.140";
        for (int i = 0; i < 10; i++) {
            rateLimitService.checkRateLimit(clientIp, RateLimitRule.LOGIN);
        }
        assertThrows(RateLimitExceededException.class,
                () -> rateLimitService.checkRateLimit(clientIp, RateLimitRule.LOGIN));

        String redisKey = new RateLimitRule(RateLimitRule.LOGIN, 10, 60).buildRedisKey(clientIp);
        Boolean deleted = redisTemplate.delete(redisKey);
        assertTrue(deleted);

        assertDoesNotThrow(() -> rateLimitService.checkRateLimit(clientIp, RateLimitRule.LOGIN));
    }

    @Test
    void retryAfterCalculation() {
        String clientIp = "192.168.1.150";
        for (int i = 0; i < 10; i++) {
            rateLimitService.checkRateLimit(clientIp, RateLimitRule.LOGIN);
        }
        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> rateLimitService.checkRateLimit(clientIp, RateLimitRule.LOGIN));
        assertTrue(ex.getRetryAfterSeconds() > 0);
        assertTrue(ex.getRetryAfterSeconds() <= 60);
    }

    @Test
    void endpointRuleSelection() {
        assertDoesNotThrow(() -> rateLimitService.checkRateLimit("1.2.3.4", RateLimitRule.LOGIN));
        assertDoesNotThrow(() -> rateLimitService.checkRateLimit("1.2.3.4", RateLimitRule.REGISTER));
        assertDoesNotThrow(() -> rateLimitService.checkRateLimit("1.2.3.4", RateLimitRule.DISCOVERY));
    }

    @Test
    void ttlCalculation() {
        String clientIp = "192.168.1.160";
        rateLimitService.checkRateLimit(clientIp, RateLimitRule.LOGIN);

        String redisKey = new RateLimitRule(RateLimitRule.LOGIN, 10, 60).buildRedisKey(clientIp);
        Long ttl = redisTemplate.getExpire(redisKey, java.util.concurrent.TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0);
        assertTrue(ttl <= 60);
    }
}
