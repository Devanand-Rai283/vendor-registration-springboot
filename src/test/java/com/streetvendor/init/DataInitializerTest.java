package com.streetvendor.init;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.config.AdminProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private DataInitializer dataInitializer;

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "SecureAdmin123!";
    private static final String HASHED_PASSWORD = "$2a$12$hashedPasswordValue";

    @BeforeEach
    void setUp() {
        AdminProperties adminProperties = new AdminProperties(ADMIN_EMAIL, ADMIN_PASSWORD);
        ReflectionTestUtils.setField(dataInitializer, "adminProperties", adminProperties);
    }

    @Test
    void shouldCreateBootstrapAdminWhenNoneExists() throws Exception {
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run(applicationArguments);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldAssignAdminRoleOnBootstrapCreation() throws Exception {
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run(applicationArguments);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.ADMIN, captor.getValue().getRole());
    }

    @Test
    void shouldAssignActiveStatusOnBootstrapCreation() throws Exception {
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run(applicationArguments);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(AccountStatus.ACTIVE, captor.getValue().getAccountStatus());
    }

    @Test
    void shouldBCryptHashPasswordBeforePersisting() throws Exception {
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run(applicationArguments);

        // Verify the encoder was called with the raw value
        verify(passwordEncoder).encode(ADMIN_PASSWORD);

        // Verify the persisted user holds the hashed value, not the raw one
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(HASHED_PASSWORD, captor.getValue().getPasswordHash());
        assertNotEquals(ADMIN_PASSWORD, captor.getValue().getPasswordHash());
    }

    @Test
    void shouldAssignUuidOnBootstrapCreation() throws Exception {
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run(applicationArguments);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertNotNull(captor.getValue().getId());
    }

    @Test
    void shouldSkipCreationWhenAdminAlreadyExists() throws Exception {
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(true);

        dataInitializer.run(applicationArguments);

        // Idempotency: save must never be called
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldUseConfiguredEmailAsAdminEmail() throws Exception {
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run(applicationArguments);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(ADMIN_EMAIL, captor.getValue().getEmail());
    }

    @Test
    void shouldCheckExistenceByConfiguredEmail() throws Exception {
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(true);

        dataInitializer.run(applicationArguments);

        // Idempotency check targets the exact configured email
        verify(userRepository).existsByEmail(ADMIN_EMAIL);
        verify(userRepository, never()).save(any(User.class));
    }
}
