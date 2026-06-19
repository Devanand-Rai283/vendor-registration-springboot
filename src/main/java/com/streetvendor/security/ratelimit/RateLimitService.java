package com.streetvendor.security.ratelimit;

public interface RateLimitService {

    void checkRateLimit(String clientIp, String endpoint);
}
