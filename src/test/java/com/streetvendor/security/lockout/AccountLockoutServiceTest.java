package com.streetvendor.security.lockout;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class AccountLockoutServiceTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private RedisAccountLockoutService lockoutService;
    private StringRedisTemplate redisTemplate;
    private LettuceConnectionFactory connectionFactory;

    private static final String EMAIL = "test@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        lockoutService = new RedisAccountLockoutService(redisTemplate, 5, 15);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        connectionFactory.destroy();
    }

    @Test
    void shouldNotBeLockedInitially() {
        assertFalse(lockoutService.isLocked(EMAIL));
    }

    @Test
    void shouldIncrementCounterOnFailedAttempt() {
        lockoutService.recordFailedAttempt(EMAIL);
        assertFalse(lockoutService.isLocked(EMAIL));
    }

    @Test
    void shouldDetectLockedAccount() {
        for (int i = 0; i < 5; i++) {
            lockoutService.recordFailedAttempt(EMAIL);
        }
        assertTrue(lockoutService.isLocked(EMAIL));
    }

    @Test
    void shouldNotBeLockedBelowThreshold() {
        for (int i = 0; i < 4; i++) {
            lockoutService.recordFailedAttempt(EMAIL);
        }
        assertFalse(lockoutService.isLocked(EMAIL));
    }

    @Test
    void shouldClearLockoutOnSuccessfulLogin() {
        for (int i = 0; i < 5; i++) {
            lockoutService.recordFailedAttempt(EMAIL);
        }
        assertTrue(lockoutService.isLocked(EMAIL));

        lockoutService.clearLockout(EMAIL);
        assertFalse(lockoutService.isLocked(EMAIL));
    }

    @Test
    void shouldResetCounterAfterClear() {
        for (int i = 0; i < 5; i++) {
            lockoutService.recordFailedAttempt(EMAIL);
        }
        lockoutService.clearLockout(EMAIL);

        for (int i = 0; i < 4; i++) {
            lockoutService.recordFailedAttempt(EMAIL);
        }
        assertFalse(lockoutService.isLocked(EMAIL));
    }

    @Test
    void shouldReturnRemainingLockDuration() {
        lockoutService.recordFailedAttempt(EMAIL);
        long ttl = lockoutService.getRemainingLockDurationSeconds(EMAIL);
        assertTrue(ttl > 0);
        assertTrue(ttl <= 15 * 60);
    }

    @Test
    void shouldReturnZeroWhenNotLocked() {
        long ttl = lockoutService.getRemainingLockDurationSeconds("unknown@example.com");
        assertEquals(0, ttl);
    }

    @Test
    void shouldMaintainRollingTtl() throws InterruptedException {
        lockoutService.recordFailedAttempt(EMAIL);
        
        // Redis reports TTL in whole seconds. GitHub Actions timing varies.
        // A longer delay (2 seconds) prevents flaky assertions.
        TimeUnit.SECONDS.sleep(2);
        long ttlAfterSleep = lockoutService.getRemainingLockDurationSeconds(EMAIL);

        lockoutService.recordFailedAttempt(EMAIL);
        long ttlAfterRenew = lockoutService.getRemainingLockDurationSeconds(EMAIL);

        assertTrue(ttlAfterRenew > ttlAfterSleep,
                "Rolling TTL should extend on each failed attempt");
    }

    @Test
    void shouldLockDifferentAccountsIndependently() {
        for (int i = 0; i < 5; i++) {
            lockoutService.recordFailedAttempt(EMAIL);
        }
        assertTrue(lockoutService.isLocked(EMAIL));
        assertFalse(lockoutService.isLocked(OTHER_EMAIL));

        for (int i = 0; i < 4; i++) {
            lockoutService.recordFailedAttempt(OTHER_EMAIL);
        }
        assertFalse(lockoutService.isLocked(OTHER_EMAIL));
    }

    @Test
    void shouldHandleClearOnNonExistentKey() {
        assertDoesNotThrow(() -> lockoutService.clearLockout("nonexistent@example.com"));
    }

    @Test
    void shouldHandleIsLockedForNonExistentKey() {
        assertFalse(lockoutService.isLocked("nonexistent@example.com"));
    }
}
