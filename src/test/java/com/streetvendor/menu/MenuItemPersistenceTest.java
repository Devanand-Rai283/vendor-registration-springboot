package com.streetvendor.menu;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.support.AbstractIntegrationTest;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("audit-test")
@Transactional
class MenuItemPersistenceTest extends AbstractIntegrationTest {

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

    private MenuCategory createTestCategory(Vendor vendor) {
        MenuCategory category = new MenuCategory(UUID.randomUUID(), vendor, "Main Dishes", 0);
        entityManager.persist(category);
        return category;
    }

    @Test
    void shouldPersistMenuItemWithAllFields() {
        Vendor vendor = createTestVendor();
        MenuCategory category = createTestCategory(vendor);
        UUID itemId = UUID.randomUUID();

        MenuItem item = new MenuItem(itemId, category, vendor, "Burger", new BigDecimal("9.99"));
        item.setDescription("Juicy beef burger");
        item.setDietaryTag("Vegetarian");
        item.setImageUrl("https://example.com/burger.jpg");
        item.setAvailable(true);

        entityManager.persist(item);
        entityManager.flush();
        entityManager.clear();

        MenuItem result = entityManager.find(MenuItem.class, itemId);
        assertNotNull(result, "MenuItem should be persisted");
        assertEquals("Burger", result.getName());
        assertEquals(0, new BigDecimal("9.99").compareTo(result.getPrice()));
        assertEquals("Juicy beef burger", result.getDescription());
        assertEquals("Vegetarian", result.getDietaryTag());
        assertEquals("https://example.com/burger.jpg", result.getImageUrl());
        assertTrue(result.isAvailable());
        assertNotNull(result.getCreatedAt(), "createdAt should not be null after persist");
        assertNotNull(result.getUpdatedAt(), "updatedAt should not be null after persist");
    }

    @Test
    void shouldDefaultIsAvailableToTrue() {
        Vendor vendor = createTestVendor();
        MenuCategory category = createTestCategory(vendor);
        UUID itemId = UUID.randomUUID();

        MenuItem item = new MenuItem(itemId, category, vendor, "Fries", new BigDecimal("3.50"));

        entityManager.persist(item);
        entityManager.flush();
        entityManager.clear();

        MenuItem result = entityManager.find(MenuItem.class, itemId);
        assertTrue(result.isAvailable(), "isAvailable should default to true");
    }

    @Test
    void shouldRejectNegativePrice() {
        Vendor vendor = createTestVendor();
        MenuCategory category = createTestCategory(vendor);

        MenuItem item = new MenuItem(UUID.randomUUID(), category, vendor, "Negative Item", new BigDecimal("-1.00"));

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(item);
            entityManager.flush();
        });
    }

    @Test
    void shouldRejectNullName() {
        Vendor vendor = createTestVendor();
        MenuCategory category = createTestCategory(vendor);

        MenuItem item = new MenuItem(UUID.randomUUID(), category, vendor, null, new BigDecimal("5.00"));

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(item);
            entityManager.flush();
        });
    }

    @Test
    void shouldRejectNullPrice() {
        Vendor vendor = createTestVendor();
        MenuCategory category = createTestCategory(vendor);

        MenuItem item = new MenuItem(UUID.randomUUID(), category, vendor, "Null Price", null);

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(item);
            entityManager.flush();
        });
    }

    @Test
    void shouldPopulateCreatedAtOnPersist() {
        Vendor vendor = createTestVendor();
        MenuCategory category = createTestCategory(vendor);
        UUID itemId = UUID.randomUUID();

        MenuItem item = new MenuItem(itemId, category, vendor, "Pizza", new BigDecimal("12.99"));

        entityManager.persist(item);
        entityManager.flush();
        entityManager.clear();

        MenuItem result = entityManager.find(MenuItem.class, itemId);
        assertNotNull(result.getCreatedAt(), "createdAt should not be null after persist");
        assertNotNull(result.getUpdatedAt(), "updatedAt should not be null after persist");
    }

    @Test
    void shouldMaintainForeignKeyRelationshipToCategory() {
        Vendor vendor = createTestVendor();
        MenuCategory category = createTestCategory(vendor);
        UUID itemId = UUID.randomUUID();

        MenuItem item = new MenuItem(itemId, category, vendor, "Pasta", new BigDecimal("10.99"));

        entityManager.persist(item);
        entityManager.flush();
        entityManager.clear();

        MenuItem result = entityManager.find(MenuItem.class, itemId);
        assertNotNull(result.getCategory());
        assertEquals(category.getId(), result.getCategory().getId());
    }

    @Test
    void shouldMaintainForeignKeyRelationshipToVendor() {
        Vendor vendor = createTestVendor();
        MenuCategory category = createTestCategory(vendor);
        UUID itemId = UUID.randomUUID();

        MenuItem item = new MenuItem(itemId, category, vendor, "Salad", new BigDecimal("7.99"));

        entityManager.persist(item);
        entityManager.flush();
        entityManager.clear();

        MenuItem result = entityManager.find(MenuItem.class, itemId);
        assertNotNull(result.getVendor());
        assertEquals(vendor.getId(), result.getVendor().getId());
    }
}
