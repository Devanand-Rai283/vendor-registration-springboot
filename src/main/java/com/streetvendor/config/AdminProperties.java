package com.streetvendor.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly-typed binding for the {@code admin.*} configuration block.
 *
 * <p>Both {@code email} and {@code password} are mandatory. If either is absent
 * or blank the application will refuse to start — this prevents a silent
 * deployment where no administrator account can ever be created.
 *
 * <p>The raw {@code password} value is intentionally kept package-private at
 * the point of use: it is BCrypt-hashed by {@link com.streetvendor.init.DataInitializer}
 * and then discarded. It is never logged, never stored raw, and never exposed
 * through any API.
 *
 * <p>Required environment variables before first deployment:
 * <ul>
 *   <li>{@code ADMIN_EMAIL}    — email address of the bootstrap administrator</li>
 *   <li>{@code ADMIN_PASSWORD} — plaintext password; BCrypt-hashed on write</li>
 * </ul>
 */
@Validated
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {

    @NotBlank(message = "admin.email must not be blank — set the ADMIN_EMAIL environment variable")
    private final String email;

    @NotBlank(message = "admin.password must not be blank — set the ADMIN_PASSWORD environment variable")
    private final String password;

    public AdminProperties(@NotBlank String email, @NotBlank String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
