package com.streetvendor.auth.service;

import org.springframework.stereotype.Service;

@Service
public class NoOpLockService implements LockService {

    @Override
    public boolean isLocked(String email) {
        // No-op implementation until SECURITY-004 is implemented
        return false;
    }
}
