package com.streetvendor.menu;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("vendor-test")
@Transactional
class MenuCategoryRepositoryTest {

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        User user = new User(UUID.randomUUID(), "vendor@example.com", "hashedPassword", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(user);

        testVendor = new Vendor(UUID.randomUUID(), user, "Test Business");
        vendorRepository.save(testVendor);
    }

    @Test
    void shouldFindByVendorIdOrderByDisplayOrderAsc() {
        menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), testVendor, "Desserts", 2));
        menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), testVendor, "Appetizers", 1));
        menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), testVendor, "Main Dishes", 0));

        List<MenuCategory> found = menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(testVendor.getId());

        assertEquals(3, found.size());
        assertEquals("Main Dishes", found.get(0).getName());
        assertEquals("Appetizers", found.get(1).getName());
        assertEquals("Desserts", found.get(2).getName());
    }

    @Test
    void shouldReturnEmptyListWhenNoCategoriesForVendor() {
        List<MenuCategory> found = menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(testVendor.getId());

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnOnlyCategoriesBelongingToVendor() {
        User otherUser = new User(UUID.randomUUID(), "other@example.com", "hashedPassword", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(otherUser);
        Vendor otherVendor = new Vendor(UUID.randomUUID(), otherUser, "Other Business");
        vendorRepository.save(otherVendor);

        menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), testVendor, "My Category", 0));
        menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), otherVendor, "Other Category", 0));

        List<MenuCategory> found = menuCategoryRepository.findByVendorIdOrderByDisplayOrderAsc(testVendor.getId());

        assertEquals(1, found.size());
        assertEquals("My Category", found.get(0).getName());
    }

    @Test
    void shouldFindByIdAndVendorIdWhenMatch() {
        UUID categoryId = UUID.randomUUID();
        menuCategoryRepository.save(new MenuCategory(categoryId, testVendor, "Drinks", 0));

        Optional<MenuCategory> found = menuCategoryRepository.findByIdAndVendorId(categoryId, testVendor.getId());

        assertTrue(found.isPresent());
        assertEquals(categoryId, found.get().getId());
        assertEquals("Drinks", found.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenVendorIdDoesNotMatch() {
        UUID categoryId = UUID.randomUUID();
        menuCategoryRepository.save(new MenuCategory(categoryId, testVendor, "Drinks", 0));

        UUID otherVendorId = UUID.randomUUID();
        Optional<MenuCategory> found = menuCategoryRepository.findByIdAndVendorId(categoryId, otherVendorId);

        assertFalse(found.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenCategoryIdDoesNotExist() {
        Optional<MenuCategory> found = menuCategoryRepository.findByIdAndVendorId(UUID.randomUUID(), testVendor.getId());

        assertFalse(found.isPresent());
    }

    @Test
    void shouldDetectDuplicateNameForSameVendor() {
        menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), testVendor, "Specials", 0));

        boolean exists = menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(testVendor.getId(), "Specials");

        assertTrue(exists);
    }

    @Test
    void shouldDetectDuplicateNameCaseInsensitive() {
        menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), testVendor, "Specials", 0));

        boolean existsLowerCase = menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(testVendor.getId(), "specials");
        boolean existsUpperCase = menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(testVendor.getId(), "SPECIALS");
        boolean existsMixedCase = menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(testVendor.getId(), "SpEcIaLs");

        assertTrue(existsLowerCase);
        assertTrue(existsUpperCase);
        assertTrue(existsMixedCase);
    }

    @Test
    void shouldReturnFalseWhenNameDoesNotExistForVendor() {
        menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), testVendor, "Specials", 0));

        boolean exists = menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(testVendor.getId(), "Nonexistent");

        assertFalse(exists);
    }

    @Test
    void shouldAllowSameNameUnderDifferentVendors() {
        User otherUser = new User(UUID.randomUUID(), "other@example.com", "hashedPassword", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(otherUser);
        Vendor otherVendor = new Vendor(UUID.randomUUID(), otherUser, "Other Business");
        vendorRepository.save(otherVendor);

        menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), testVendor, "Specials", 0));
        menuCategoryRepository.save(new MenuCategory(UUID.randomUUID(), otherVendor, "Specials", 0));

        boolean existsForVendor = menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(testVendor.getId(), "Specials");
        boolean existsForOther = menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(otherVendor.getId(), "Specials");

        assertTrue(existsForVendor);
        assertTrue(existsForOther);
    }

    @Test
    void shouldReturnFalseWhenVendorHasNoCategories() {
        boolean exists = menuCategoryRepository.existsByVendorIdAndNameIgnoreCase(testVendor.getId(), "Anything");

        assertFalse(exists);
    }
}
