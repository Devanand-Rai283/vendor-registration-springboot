package com.streetvendor.menu;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.common.audit.AuditConfig;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.support.AbstractIntegrationTest;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("audit-test")
@Import(AuditConfig.class)
@Transactional
class MenuCategoryPersistenceTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Vendor createTestVendor() {
        User user = new User(
                UUID.randomUUID(),
                "test-" + UUID.randomUUID() + "@example.com",
                "hashed-password",
                Role.VENDOR,
                AccountStatus.ACTIVE
        );
        userRepository.save(user);

        Vendor vendor = new Vendor(UUID.randomUUID(), user, "Test Business");
        return vendorRepository.save(vendor);
    }

    @Test
    void shouldPopulateCreatedAtOnPersist() {
        Vendor vendor = createTestVendor();
        UUID categoryId = UUID.randomUUID();
        MenuCategory category = new MenuCategory(categoryId, vendor, "Main Dishes", 0);

        entityManager.persist(category);
        entityManager.flush();
        entityManager.clear();

        MenuCategory result = entityManager.find(MenuCategory.class, categoryId);
        assertNotNull(result, "MenuCategory should be persisted");
        assertNotNull(result.getCreatedAt(), "createdAt should not be null after persist");
    }

    @Test
    void shouldSetCreatedAtToUtcTimestamp() {
        Vendor vendor = createTestVendor();
        Instant before = Instant.now();
        UUID categoryId = UUID.randomUUID();
        MenuCategory category = new MenuCategory(categoryId, vendor, "Appetizers", 1);

        entityManager.persist(category);
        entityManager.flush();
        entityManager.clear();

        Instant after = Instant.now();
        MenuCategory result = entityManager.find(MenuCategory.class, categoryId);

        assertNotNull(result.getCreatedAt(), "createdAt should not be null after persist");
        assertFalse(result.getCreatedAt().isBefore(before), "createdAt should not be before test started");
        assertFalse(result.getCreatedAt().isAfter(after), "createdAt should not be after test ended");
    }

    @Test
    void shouldKeepCreatedAtUnchangedOnUpdate() throws InterruptedException {
        Vendor vendor = createTestVendor();
        UUID categoryId = UUID.randomUUID();
        MenuCategory category = new MenuCategory(categoryId, vendor, "Desserts", 2);

        entityManager.persist(category);
        entityManager.flush();
        entityManager.clear();

        MenuCategory result = entityManager.find(MenuCategory.class, categoryId);
        Instant originalCreatedAt = result.getCreatedAt();

        entityManager.detach(result);

        Thread.sleep(1);

        result.setName("Updated Desserts");
        entityManager.merge(result);
        entityManager.flush();
        entityManager.clear();

        MenuCategory updatedResult = entityManager.find(MenuCategory.class, categoryId);
        assertEquals(originalCreatedAt, updatedResult.getCreatedAt(), "createdAt should remain unchanged after update");
    }
}
