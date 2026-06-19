package com.streetvendor.auth.service;

import com.streetvendor.auth.dto.LoginRequest;
import com.streetvendor.auth.dto.LoginResult;
import com.streetvendor.auth.dto.LoginResponse;
import com.streetvendor.auth.dto.RegisterRequest;
import com.streetvendor.auth.dto.RegisterResponse;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.common.audit.AuditService;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.customer.service.CustomerService;
import com.streetvendor.security.JwtService;
import com.streetvendor.security.lockout.AccountLockedException;
import com.streetvendor.security.lockout.AccountLockoutService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerService customerService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AccountLockoutService accountLockoutService;
    private final AuditService auditService;

    @Value("${jwt.access-expiration}")
    private long accessExpirationMs;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CustomerService customerService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AccountLockoutService accountLockoutService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerService = customerService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.accountLockoutService = accountLockoutService;
        this.auditService = auditService;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new ForbiddenException("Admin registration is not allowed");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = new User(
                UUID.randomUUID(),
                request.email(),
                encodedPassword,
                request.role(),
                AccountStatus.ACTIVE
        );

        User savedUser = userRepository.save(user);

        if (request.role() == Role.CUSTOMER) {
            customerService.createProfile(
                    savedUser.getId(),
                    request.fullName(),
                    request.phone(),
                    request.address(),
                    request.latitude(),
                    request.longitude()
            );
        }

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getAccountStatus()
        );
    }

    @Override
    public LoginResult login(LoginRequest request) {
        String email = request.email();

        if (accountLockoutService.isLocked(email)) {
            long remainingSeconds = accountLockoutService.getRemainingLockDurationSeconds(email);
            long remainingMinutes = Math.max(1, (remainingSeconds + 59) / 60);
            throw new AccountLockedException(remainingMinutes);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        auditService.logEvent(AuditEventType.LOGIN_ATTEMPT, user.getId(), "Login attempt for " + email);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            accountLockoutService.recordFailedAttempt(email);
            auditService.logEvent(AuditEventType.LOGIN_FAILED, user.getId(), "Login failed - invalid password for " + email);

            if (accountLockoutService.isLocked(email)) {
                long remainingSeconds = accountLockoutService.getRemainingLockDurationSeconds(email);
                long remainingMinutes = Math.max(1, (remainingSeconds + 59) / 60);
                auditService.logEvent(AuditEventType.ACCOUNT_LOCKED, user.getId(),
                        "Account locked due to repeated failed login attempts for " + email);
                throw new AccountLockedException(remainingMinutes);
            }

            throw new UnauthorizedException("Invalid email or password.");
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("Account is not active.");
        }

        accountLockoutService.clearLockout(email);
        auditService.logEvent(AuditEventType.LOGIN_SUCCESS, user.getId(), "Login successful for " + email);

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String rawRefreshToken = refreshTokenService.generateRefreshToken(user.getId());

        long expiresIn = accessExpirationMs / 1000;

        LoginResponse response = new LoginResponse(accessToken, "Bearer", expiresIn);
        return new LoginResult(response, rawRefreshToken);
    }

    @Override
    public LoginResult refresh(String rawRefreshToken) {
        RotateResult rotateResult = refreshTokenService.rotate(rawRefreshToken);

        User user = userRepository.findById(rotateResult.userId())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token."));

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        long expiresIn = accessExpirationMs / 1000;

        LoginResponse response = new LoginResponse(accessToken, "Bearer", expiresIn);
        return new LoginResult(response, rotateResult.rawToken());
    }
}
