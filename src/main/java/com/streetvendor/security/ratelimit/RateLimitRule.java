package com.streetvendor.security.ratelimit;

public record RateLimitRule(String endpoint, int limit, int windowSeconds) {

    public static final String LOGIN = "login";
    public static final String REGISTER = "register";
    public static final String DISCOVERY = "discovery";

    public static final int LOGIN_LIMIT = 10;
    public static final int REGISTER_LIMIT = 5;
    public static final int DISCOVERY_LIMIT = 60;

    public static final int WINDOW_SECONDS = 60;

    public static final String KEY_PREFIX = "ratelimit";

    public static RateLimitRule forLogin() {
        return new RateLimitRule(LOGIN, LOGIN_LIMIT, WINDOW_SECONDS);
    }

    public static RateLimitRule forRegister() {
        return new RateLimitRule(REGISTER, REGISTER_LIMIT, WINDOW_SECONDS);
    }

    public static RateLimitRule forDiscovery() {
        return new RateLimitRule(DISCOVERY, DISCOVERY_LIMIT, WINDOW_SECONDS);
    }

    public String buildRedisKey(String clientIp) {
        return KEY_PREFIX + ":" + clientIp + ":" + endpoint;
    }
}
