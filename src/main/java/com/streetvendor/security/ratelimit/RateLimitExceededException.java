package com.streetvendor.security.ratelimit;

public class RateLimitExceededException extends RuntimeException {

    private final int retryAfterSeconds;
    private final String endpoint;
    private final String clientIp;

    public RateLimitExceededException(int retryAfterSeconds, String endpoint, String clientIp) {
        super("Too many requests. Please try again later.");
        this.retryAfterSeconds = retryAfterSeconds;
        this.endpoint = endpoint;
        this.clientIp = clientIp;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getClientIp() {
        return clientIp;
    }
}
