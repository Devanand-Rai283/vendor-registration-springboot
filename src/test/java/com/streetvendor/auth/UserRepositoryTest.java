package com.streetvendor.auth;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("auth-test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndRetrieveUser() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "test@example.com", "hashedPassword", Role.CUSTOMER, AccountStatus.ACTIVE);

        userRepository.save(user);
        userRepository.flush();

        Optional<User> found = userRepository.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals("test@example.com", found.get().getEmail());
        assertEquals("hashedPassword", found.get().getPasswordHash());
        assertEquals(Role.CUSTOMER, found.get().getRole());
        assertEquals(AccountStatus.ACTIVE, found.get().getAccountStatus());
        assertNotNull(found.get().getCreatedAt());
        assertNotNull(found.get().getUpdatedAt());
    }

    @Test
    void shouldFindByEmail() {
        User user = new User(UUID.randomUUID(), "find@example.com", "hashedPassword", Role.VENDOR, AccountStatus.ACTIVE);

        userRepository.save(user);
        userRepository.flush();

        Optional<User> found = userRepository.findByEmail("find@example.com");

        assertTrue(found.isPresent());
        assertEquals("find@example.com", found.get().getEmail());
    }

    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertFalse(found.isPresent());
    }

    @Test
    void shouldCheckEmailExists() {
        User user = new User(UUID.randomUUID(), "exists@example.com", "hashedPassword", Role.CUSTOMER, AccountStatus.ACTIVE);

        userRepository.save(user);
        userRepository.flush();

        assertTrue(userRepository.existsByEmail("exists@example.com"));
        assertFalse(userRepository.existsByEmail("other@example.com"));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        User user1 = new User(UUID.randomUUID(), "duplicate@example.com", "hashedPassword1", Role.CUSTOMER, AccountStatus.ACTIVE);
        User user2 = new User(UUID.randomUUID(), "duplicate@example.com", "hashedPassword2", Role.VENDOR, AccountStatus.ACTIVE);

        userRepository.save(user1);
        userRepository.flush();

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.save(user2);
            userRepository.flush();
        });
    }

    @Test
    void shouldSaveUserWithAllRoles() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        User customer = new User(id1, "customer@example.com", "hash1", Role.CUSTOMER, AccountStatus.ACTIVE);
        User vendor = new User(id2, "vendor@example.com", "hash2", Role.VENDOR, AccountStatus.ACTIVE);
        User admin = new User(id3, "admin@example.com", "hash3", Role.ADMIN, AccountStatus.ACTIVE);

        userRepository.save(customer);
        userRepository.save(vendor);
        userRepository.save(admin);
        userRepository.flush();

        assertEquals(3, userRepository.count());
    }

    @Test
    void shouldSaveUserWithAllAccountStatuses() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        User active = new User(id1, "active@example.com", "hash1", Role.CUSTOMER, AccountStatus.ACTIVE);
        User suspended = new User(id2, "suspended@example.com", "hash2", Role.CUSTOMER, AccountStatus.SUSPENDED);
        User deleted = new User(id3, "deleted@example.com", "hash3", Role.CUSTOMER, AccountStatus.DELETED);

        userRepository.save(active);
        userRepository.save(suspended);
        userRepository.save(deleted);
        userRepository.flush();

        assertEquals(3, userRepository.count());
    }
}
