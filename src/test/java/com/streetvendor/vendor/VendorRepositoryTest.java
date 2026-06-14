package com.streetvendor.vendor;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("vendor-test")
@Transactional
class VendorRepositoryTest {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(UUID.randomUUID(), "vendor@example.com", "hashedPassword", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(testUser);
    }

    @Test
    void shouldSaveAndRetrieveVendor() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = new Vendor(vendorId, testUser, "Test Business");
        vendor.setOwnerName("Owner Name");
        vendor.setPhone("1234567890");
        vendor.setFoodType("Indian");
        vendor.setDescription("Delicious food");
        vendor.setAddress("123 Main St");
        vendor.setLatitude(new BigDecimal("12.9716"));
        vendor.setLongitude(new BigDecimal("77.5946"));
        vendor.setStatus(VendorStatus.PENDING_REVIEW);
        vendor.setAverageRating(new BigDecimal("4.5"));
        vendor.setTotalReviews(10);

        vendorRepository.save(vendor);
        vendorRepository.flush();

        Optional<Vendor> found = vendorRepository.findById(vendorId);

        assertTrue(found.isPresent());
        assertEquals(vendorId, found.get().getId());
        assertEquals(testUser.getId(), found.get().getUser().getId());
        assertEquals("Test Business", found.get().getBusinessName());
        assertEquals("Owner Name", found.get().getOwnerName());
        assertEquals("1234567890", found.get().getPhone());
        assertEquals("Indian", found.get().getFoodType());
        assertEquals("Delicious food", found.get().getDescription());
        assertEquals("123 Main St", found.get().getAddress());
        assertEquals(new BigDecimal("12.9716"), found.get().getLatitude());
        assertEquals(new BigDecimal("77.5946"), found.get().getLongitude());
        assertEquals(VendorStatus.PENDING_REVIEW, found.get().getStatus());
        assertEquals(new BigDecimal("4.5"), found.get().getAverageRating());
        assertEquals(10, found.get().getTotalReviews());
        assertNotNull(found.get().getCreatedAt());
        assertNotNull(found.get().getUpdatedAt());
    }

    @Test
    void shouldFindByUserId() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = new Vendor(vendorId, testUser, "Business");
        vendorRepository.save(vendor);
        vendorRepository.flush();

        Optional<Vendor> found = vendorRepository.findByUserId(testUser.getId());

        assertTrue(found.isPresent());
        assertEquals(vendorId, found.get().getId());
    }

    @Test
    void shouldReturnEmptyWhenUserIdNotFound() {
        Optional<Vendor> found = vendorRepository.findByUserId(UUID.randomUUID());

        assertFalse(found.isPresent());
    }

    @Test
    void shouldCheckExistsByUserId() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = new Vendor(vendorId, testUser, "Business");
        vendorRepository.save(vendor);
        vendorRepository.flush();

        assertTrue(vendorRepository.existsByUserId(testUser.getId()));
        assertFalse(vendorRepository.existsByUserId(UUID.randomUUID()));
    }

    @Test
    void shouldRejectDuplicateUserId() {
        UUID vendorId1 = UUID.randomUUID();
        UUID vendorId2 = UUID.randomUUID();
        Vendor vendor1 = new Vendor(vendorId1, testUser, "Business1");
        Vendor vendor2 = new Vendor(vendorId2, testUser, "Business2");

        vendorRepository.save(vendor1);
        vendorRepository.flush();

        try {
            vendorRepository.save(vendor2);
            vendorRepository.flush();
            // If no exception, the test should fail
            assertTrue(false, "Expected DataIntegrityViolationException");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Expected
        }
    }

    @Test
    void shouldDefaultStatusToPendingReview() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = new Vendor(vendorId, testUser, "Business");
        vendorRepository.save(vendor);
        vendorRepository.flush();

        Optional<Vendor> found = vendorRepository.findById(vendorId);
        assertTrue(found.isPresent());
        assertEquals(VendorStatus.PENDING_REVIEW, found.get().getStatus());
    }

    @Test
    void shouldDefaultAverageRatingToZero() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = new Vendor(vendorId, testUser, "Business");
        vendorRepository.save(vendor);
        vendorRepository.flush();

        Optional<Vendor> found = vendorRepository.findById(vendorId);
        assertTrue(found.isPresent());
        assertEquals(BigDecimal.ZERO, found.get().getAverageRating());
    }

    @Test
    void shouldDefaultTotalReviewsToZero() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = new Vendor(vendorId, testUser, "Business");
        vendorRepository.save(vendor);
        vendorRepository.flush();

        Optional<Vendor> found = vendorRepository.findById(vendorId);
        assertTrue(found.isPresent());
        assertEquals(0, found.get().getTotalReviews());
    }
}