package com.streetvendor.security.lockout;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisAccountLockoutService implements AccountLockoutService {

    private static final String LOCKOUT_KEY_PREFIX = "lockout:";

    private final StringRedisTemplate redisTemplate;
    private final int threshold;
    private final int lockDurationMinutes;

    public RedisAccountLockoutService(
            StringRedisTemplate redisTemplate,
            @Value("${account.lock.threshold:5}") int threshold,
            @Value("${account.lock.duration-minutes:15}") int lockDurationMinutes) {
        this.redisTemplate = redisTemplate;
        this.threshold = threshold;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    @Override
    public void recordFailedAttempt(String email) {
        String key = buildKey(email);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, lockDurationMinutes, TimeUnit.MINUTES);
    }

    @Override
    public boolean isLocked(String email) {
        String key = buildKey(email);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return false;
        }
        int count = Integer.parseInt(value);
        return count >= threshold;
    }

    @Override
    public void clearLockout(String email) {
        String key = buildKey(email);
        redisTemplate.delete(key);
    }

    @Override
    public long getRemainingLockDurationSeconds(String email) {
        String key = buildKey(email);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            return 0;
        }
        return ttl;
    }

    private static String buildKey(String email) {
        return LOCKOUT_KEY_PREFIX + email;
    }
}
