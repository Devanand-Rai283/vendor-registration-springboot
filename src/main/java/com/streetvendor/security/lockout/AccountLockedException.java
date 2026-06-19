package com.streetvendor.security.lockout;

public class AccountLockedException extends RuntimeException {

    private final long remainingMinutes;

    public AccountLockedException(long remainingMinutes) {
        super("Account temporarily locked due to repeated failed login attempts. Try again in " + remainingMinutes + " minutes.");
        this.remainingMinutes = remainingMinutes;
    }

    public long getRemainingMinutes() {
        return remainingMinutes;
    }
}
