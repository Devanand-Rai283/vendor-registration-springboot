package com.streetvendor.init;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.config.AdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Bootstrap component that seeds the first ADMIN account on application startup.
 *
 * <p><strong>Execution:</strong> implements {@link ApplicationRunner} so this
 * runs after the full Spring context is ready (Flyway migrations complete,
 * all beans wired).
 *
 * <p><strong>Idempotency:</strong> the admin email is checked via
 * {@code userRepository.existsByEmail()} before any write is attempted.
 * Repeated application restarts will detect the existing account and skip
 * creation without error.
 *
 * <p><strong>Security:</strong>
 * <ul>
 *   <li>The raw password from {@link AdminProperties} is BCrypt-hashed at
 *       strength 12 (enforced by the shared {@link PasswordEncoder} bean).</li>
 *   <li>The raw value and the hash are never written to any log.</li>
 *   <li>After hashing the reference is not retained — it is passed directly
 *       to the {@link User} constructor and discarded.</li>
 * </ul>
 *
 * <p><strong>Failure modes:</strong> if {@code ADMIN_EMAIL} or
 * {@code ADMIN_PASSWORD} are missing or blank, Spring's {@code @Validated}
 * constraint on {@link AdminProperties} prevents context startup before this
 * class ever executes.
 */
@Component
@ConditionalOnProperty(name = "bootstrap.admin.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AdminProperties.class)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AdminProperties adminProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String adminEmail = adminProperties.getEmail();

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Bootstrap administrator already exists. Skipping initialization.");
            return;
        }

        String encodedPassword = passwordEncoder.encode(adminProperties.getPassword());

        User admin = new User(
                UUID.randomUUID(),
                adminEmail,
                encodedPassword,
                Role.ADMIN,
                AccountStatus.ACTIVE
        );

        userRepository.save(admin);

        log.info("Bootstrap administrator created successfully.");
    }
}
