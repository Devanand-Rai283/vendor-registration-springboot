package com.streetvendor.security;

import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public String extractUsername(String token) {
        throw new UnsupportedOperationException("JWT implementation scheduled for AUTH phase");
    }

    public boolean validateToken(String token) {
        throw new UnsupportedOperationException("JWT implementation scheduled for AUTH phase");
    }
}
