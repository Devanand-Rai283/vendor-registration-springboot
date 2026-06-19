package com.streetvendor.security.lockout;

public interface AccountLockoutService {

    void recordFailedAttempt(String email);

    boolean isLocked(String email);

    void clearLockout(String email);

    long getRemainingLockDurationSeconds(String email);
}
