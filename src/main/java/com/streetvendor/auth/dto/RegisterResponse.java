package com.streetvendor.auth.dto;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String email,
        Role role,
        AccountStatus accountStatus
) {
}
