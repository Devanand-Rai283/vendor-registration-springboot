package com.streetvendor.security.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RedisRateLimitService implements RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private final Map<String, RateLimitRule> rules;

    public RedisRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rules = new ConcurrentHashMap<>();
        rules.put(RateLimitRule.LOGIN, RateLimitRule.forLogin());
        rules.put(RateLimitRule.REGISTER, RateLimitRule.forRegister());
        rules.put(RateLimitRule.DISCOVERY, RateLimitRule.forDiscovery());
    }

    @Override
    public void checkRateLimit(String clientIp, String endpoint) {
        RateLimitRule rule = rules.get(endpoint);
        if (rule == null) {
            return;
        }

        String redisKey = rule.buildRedisKey(clientIp);

        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count == null) {
            return;
        }

        if (count == 1) {
            redisTemplate.expire(redisKey, rule.windowSeconds(), TimeUnit.SECONDS);
        }

        if (count > rule.limit()) {
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            int retryAfter = ttl != null ? ttl.intValue() : rule.windowSeconds();
            throw new RateLimitExceededException(retryAfter, endpoint, clientIp);
        }
    }
}
