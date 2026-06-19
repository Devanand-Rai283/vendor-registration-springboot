package com.streetvendor.auth;

import com.streetvendor.auth.dto.LoginRequest;
import com.streetvendor.auth.dto.LoginResult;
import com.streetvendor.auth.dto.LoginResponse;
import com.streetvendor.auth.dto.RegisterRequest;
import com.streetvendor.auth.dto.RegisterResponse;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.auth.service.AuthServiceImpl;
import com.streetvendor.auth.service.RefreshTokenService;
import com.streetvendor.auth.service.RotateResult;
import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.common.audit.AuditService;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.customer.service.CustomerService;
import com.streetvendor.security.JwtService;
import com.streetvendor.security.lockout.AccountLockedException;
import com.streetvendor.security.lockout.AccountLockoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomerService customerService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AccountLockoutService accountLockoutService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest customerRequest;
    private RegisterRequest vendorRequest;
    private RegisterRequest adminRequest;
    private LoginRequest loginRequest;
    private User activeUser;
    private User suspendedUser;

    @BeforeEach
    void setUp() {
        customerRequest = new RegisterRequest(
                "customer@example.com",
                "Password1!",
                Role.CUSTOMER,
                "John Doe",
                "1234567890",
                "123 Main St",
                new BigDecimal("40.71280000"),
                new BigDecimal("-74.00600000")
        );
        vendorRequest = new RegisterRequest(
                "vendor@example.com",
                "Password1!",
                Role.VENDOR,
                null,
                null,
                null,
                null,
                null
        );
        adminRequest = new RegisterRequest(
                "admin@example.com",
                "Password1!",
                Role.ADMIN,
                null,
                null,
                null,
                null,
                null
        );
        loginRequest = new LoginRequest("user@example.com", "Password1!");

        activeUser = new User(
                UUID.randomUUID(),
                "user@example.com",
                "$2a$12$encodedHash",
                Role.CUSTOMER,
                AccountStatus.ACTIVE
        );

        suspendedUser = new User(
                UUID.randomUUID(),
                "suspended@example.com",
                "$2a$12$encodedHash",
                Role.CUSTOMER,
                AccountStatus.SUSPENDED
        );
    }

    @Test
    void shouldRegisterCustomerSuccessfully() {
        when(userRepository.existsByEmail(any(String.class))).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });

        RegisterResponse response = authService.register(customerRequest);

        assertNotNull(response);
        assertEquals("customer@example.com", response.email());
        assertEquals(Role.CUSTOMER, response.role());
        assertEquals(AccountStatus.ACTIVE, response.accountStatus());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRegisterVendorSuccessfully() {
        when(userRepository.existsByEmail(any(String.class))).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });

        RegisterResponse response = authService.register(vendorRequest);

        assertNotNull(response);
        assertEquals("vendor@example.com", response.email());
        assertEquals(Role.VENDOR, response.role());
        assertEquals(AccountStatus.ACTIVE, response.accountStatus());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRejectAdminRegistration() {
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            authService.register(adminRequest);
        });

        assertEquals("Admin registration is not allowed", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        RegisterRequest duplicateRequest = new RegisterRequest(
                "duplicate@example.com",
                "Password1!",
                Role.CUSTOMER,
                null,
                null,
                null,
                null,
                null
        );

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            authService.register(duplicateRequest);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldHashPasswordWithBCrypt() {
        when(userRepository.existsByEmail(any(String.class))).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("$2a$12$encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });

        authService.register(customerRequest);

        verify(passwordEncoder).encode("Password1!");
    }

    @Test
    void shouldGenerateUUIDForNewUser() {
        when(userRepository.existsByEmail(any(String.class))).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });

        RegisterResponse response = authService.register(customerRequest);

        assertNotNull(response.id());
    }

    @Test
    void shouldCreateCustomerProfileForCustomerRegistration() {
        when(userRepository.existsByEmail(any(String.class))).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });

        authService.register(customerRequest);

        verify(customerService).createProfile(
                any(UUID.class),
                eq("John Doe"),
                eq("1234567890"),
                eq("123 Main St"),
                eq(new BigDecimal("40.71280000")),
                eq(new BigDecimal("-74.00600000"))
        );
    }

    @Test
    void shouldNotCreateCustomerProfileForVendorRegistration() {
        when(userRepository.existsByEmail(any(String.class))).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });

        authService.register(vendorRequest);

        verify(customerService, never()).createProfile(
                any(UUID.class),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull()
        );
    }

    @Test
    void shouldLoginSuccessfully() {
        when(accountLockoutService.isLocked("user@example.com")).thenReturn(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Password1!", "$2a$12$encodedHash")).thenReturn(true);
        when(jwtService.generateAccessToken(any(UUID.class), eq("user@example.com"), eq("CUSTOMER")))
                .thenReturn("jwt-token");
        when(refreshTokenService.generateRefreshToken(any(UUID.class))).thenReturn("raw-refresh-token");

        LoginResult result = authService.login(loginRequest);

        assertNotNull(result);
        assertEquals("jwt-token", result.response().accessToken());
        assertEquals("Bearer", result.response().tokenType());
        assertEquals("raw-refresh-token", result.refreshToken());
        verify(accountLockoutService).clearLockout("user@example.com");
        verify(auditService).logEvent(eq(AuditEventType.LOGIN_ATTEMPT), eq(activeUser.getId()), anyString());
        verify(auditService).logEvent(eq(AuditEventType.LOGIN_SUCCESS), eq(activeUser.getId()), anyString());
        verify(refreshTokenService).generateRefreshToken(any(UUID.class));
    }

    @Test
    void shouldRejectInvalidEmail() {
        when(accountLockoutService.isLocked("nonexistent@example.com")).thenReturn(false);
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        LoginRequest invalidRequest = new LoginRequest("nonexistent@example.com", "Password1!");

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            authService.login(invalidRequest);
        });

        assertEquals("Invalid email or password.", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidPassword() {
        when(accountLockoutService.isLocked("user@example.com")).thenReturn(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("WrongPassword!", "$2a$12$encodedHash")).thenReturn(false);

        LoginRequest wrongPasswordRequest = new LoginRequest("user@example.com", "WrongPassword!");

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            authService.login(wrongPasswordRequest);
        });

        assertEquals("Invalid email or password.", exception.getMessage());
        verify(accountLockoutService).recordFailedAttempt("user@example.com");
        verify(auditService).logEvent(eq(AuditEventType.LOGIN_FAILED), eq(activeUser.getId()), anyString());
    }

    @Test
    void shouldRejectSuspendedAccount() {
        when(accountLockoutService.isLocked("suspended@example.com")).thenReturn(false);
        when(userRepository.findByEmail("suspended@example.com")).thenReturn(Optional.of(suspendedUser));
        when(passwordEncoder.matches("Password1!", "$2a$12$encodedHash")).thenReturn(true);

        LoginRequest suspendedRequest = new LoginRequest("suspended@example.com", "Password1!");

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            authService.login(suspendedRequest);
        });

        assertEquals("Account is not active.", exception.getMessage());
    }

    @Test
    void shouldRejectLockedAccount() {
        when(accountLockoutService.isLocked("user@example.com")).thenReturn(true);
        when(accountLockoutService.getRemainingLockDurationSeconds("user@example.com")).thenReturn(300L);

        AccountLockedException exception = assertThrows(AccountLockedException.class, () -> {
            authService.login(loginRequest);
        });

        assertTrue(exception.getMessage().contains("Account temporarily locked"));
        verify(accountLockoutService, never()).clearLockout(anyString());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void shouldPersistRefreshTokenOnLogin() {
        when(accountLockoutService.isLocked("user@example.com")).thenReturn(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Password1!", "$2a$12$encodedHash")).thenReturn(true);
        when(jwtService.generateAccessToken(any(UUID.class), eq("user@example.com"), eq("CUSTOMER")))
                .thenReturn("jwt-token");
        when(refreshTokenService.generateRefreshToken(any(UUID.class))).thenReturn("raw-refresh-token");

        authService.login(loginRequest);

        verify(refreshTokenService).generateRefreshToken(activeUser.getId());
    }

    @Test
    void shouldThrowAccountLockedWhenThresholdReached() {
        when(accountLockoutService.isLocked("user@example.com")).thenReturn(false).thenReturn(true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Password1!", "$2a$12$encodedHash")).thenReturn(false);
        when(accountLockoutService.getRemainingLockDurationSeconds("user@example.com")).thenReturn(900L);

        LoginRequest req = new LoginRequest("user@example.com", "Password1!");

        AccountLockedException exception = assertThrows(AccountLockedException.class, () -> authService.login(req));
        assertTrue(exception.getMessage().contains("Account temporarily locked"));
        verify(accountLockoutService).recordFailedAttempt("user@example.com");
        verify(auditService).logEvent(eq(AuditEventType.LOGIN_FAILED), eq(activeUser.getId()), anyString());
        verify(auditService).logEvent(eq(AuditEventType.ACCOUNT_LOCKED), eq(activeUser.getId()), anyString());
        verify(accountLockoutService, never()).clearLockout(anyString());
    }

    @Test
    void shouldRefreshSuccessfully() {
        UUID userId = activeUser.getId();
        String rawRefreshToken = userId + ":random-data";
        String newRawToken = userId + ":new-random-data";
        RotateResult rotateResult = new RotateResult(newRawToken, userId);
        when(refreshTokenService.rotate(rawRefreshToken)).thenReturn(rotateResult);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(jwtService.generateAccessToken(userId, "user@example.com", "CUSTOMER"))
                .thenReturn("new-jwt-token");

        LoginResult result = authService.refresh(rawRefreshToken);

        assertNotNull(result);
        assertEquals("new-jwt-token", result.response().accessToken());
        assertEquals("Bearer", result.response().tokenType());
        assertEquals(newRawToken, result.refreshToken());
        verify(refreshTokenService).rotate(rawRefreshToken);
    }

    @Test
    void shouldRejectRefreshWithInvalidToken() {
        String invalidToken = "invalid-token";
        when(refreshTokenService.rotate(invalidToken))
                .thenThrow(new UnauthorizedException("Invalid refresh token."));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.refresh(invalidToken));

        assertEquals("Invalid refresh token.", exception.getMessage());
    }

    @Test
    void shouldRejectRefreshWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        String rawRefreshToken = userId + ":random-data";
        String newRawToken = userId + ":new-random-data";
        RotateResult rotateResult = new RotateResult(newRawToken, userId);
        when(refreshTokenService.rotate(rawRefreshToken)).thenReturn(rotateResult);
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.refresh(rawRefreshToken));

        assertEquals("Invalid refresh token.", exception.getMessage());
    }
}
