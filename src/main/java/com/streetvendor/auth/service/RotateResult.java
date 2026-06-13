package com.streetvendor.auth.service;

import java.util.UUID;

public record RotateResult(String rawToken, UUID userId) {
}
