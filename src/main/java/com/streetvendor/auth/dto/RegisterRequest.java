package com.streetvendor.auth.dto;

import com.streetvendor.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Password must be at least 8 characters with uppercase, lowercase, digit, and special character"
        )
        String password,

        @NotNull(message = "Role is required")
        Role role,

        String fullName,

        String phone,

        String address,

        BigDecimal latitude,

        BigDecimal longitude
) {
}
