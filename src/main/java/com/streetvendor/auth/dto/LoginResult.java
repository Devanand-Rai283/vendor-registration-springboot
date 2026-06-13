package com.streetvendor.auth.dto;

public record LoginResult(
        LoginResponse response,
        String refreshToken
) {
}
