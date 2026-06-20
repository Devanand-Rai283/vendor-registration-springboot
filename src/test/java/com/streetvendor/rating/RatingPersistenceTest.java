package com.streetvendor.rating;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.rating.entity.Rating;
import com.streetvendor.rating.repository.RatingRepository;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("order-test")
@Transactional
class RatingPersistenceTest {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    private Customer testCustomer;
    private Vendor testVendor;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        User customerUser = userRepository.save(
                new User(UUID.randomUUID(), "cust@test.com", "hash", Role.CUSTOMER, AccountStatus.ACTIVE));
        testCustomer = customerRepository.save(
                new Customer(UUID.randomUUID(), customerUser.getId(), "Cust Name", "12345", "Addr", null, null));

        User vendorUser = userRepository.save(
                new User(UUID.randomUUID(), "vend@test.com", "hash", Role.VENDOR, AccountStatus.ACTIVE));
        testVendor = vendorRepository.save(
                new Vendor(UUID.randomUUID(), vendorUser, "Business Name"));

        testOrder = orderRepository.save(
                new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("100.00"), UUID.randomUUID().toString()));
    }

    @Test
    void shouldSaveAndRetrieveRating() {
        UUID ratingId = UUID.randomUUID();
        Rating rating = new Rating(ratingId, testOrder, testCustomer, testVendor, 5, "Excellent!");

        ratingRepository.save(rating);
        ratingRepository.flush();

        Optional<Rating> found = ratingRepository.findById(ratingId);
        assertTrue(found.isPresent());
        assertEquals(ratingId, found.get().getId());
        assertEquals(testOrder.getId(), found.get().getOrder().getId());
        assertEquals(testCustomer.getId(), found.get().getCustomer().getId());
        assertEquals(testVendor.getId(), found.get().getVendor().getId());
        assertEquals(5, found.get().getStars());
        assertEquals("Excellent!", found.get().getReviewText());
        assertNotNull(found.get().getCreatedAt());
    }

    @Test
    void shouldRejectDuplicateOrderRating() {
        Rating rating1 = new Rating(UUID.randomUUID(), testOrder, testCustomer, testVendor, 4, "Good");
        ratingRepository.save(rating1);
        ratingRepository.flush();

        Rating rating2 = new Rating(UUID.randomUUID(), testOrder, testCustomer, testVendor, 5, "Amazing");

        assertThrows(DataIntegrityViolationException.class, () -> {
            ratingRepository.save(rating2);
            ratingRepository.flush();
        });
    }

    @Test
    void shouldRejectStarsLessThanOne() {
        Rating rating = new Rating(UUID.randomUUID(), testOrder, testCustomer, testVendor, 0, "Terrible");

        assertThrows(ConstraintViolationException.class, () -> {
            ratingRepository.save(rating);
            ratingRepository.flush();
        });
    }

    @Test
    void shouldRejectStarsGreaterThanFive() {
        Rating rating = new Rating(UUID.randomUUID(), testOrder, testCustomer, testVendor, 6, "Beyond expectations");

        assertThrows(ConstraintViolationException.class, () -> {
            ratingRepository.save(rating);
            ratingRepository.flush();
        });
    }

    @Test
    void shouldRetrieveRatingsInDescendingOrderOfCreation() throws InterruptedException {
        Rating rating1 = new Rating(UUID.randomUUID(), testOrder, testCustomer, testVendor, 3, "Oldest");
        ratingRepository.save(rating1);

        Order order2 = orderRepository.save(
                new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("100.00"), UUID.randomUUID().toString()));
        Rating rating2 = new Rating(UUID.randomUUID(), order2, testCustomer, testVendor, 4, "Older");
        ratingRepository.save(rating2);

        Order order3 = orderRepository.save(
                new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("100.00"), UUID.randomUUID().toString()));
        Rating rating3 = new Rating(UUID.randomUUID(), order3, testCustomer, testVendor, 5, "Newest");
        ratingRepository.save(rating3);

        ratingRepository.flush();

        // Update the created_at field directly in the DB using native queries to bypass JPA Auditing overrides
        entityManager.createNativeQuery("UPDATE ratings SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", Instant.now().minusSeconds(100))
                .setParameter("id", rating1.getId())
                .executeUpdate();

        entityManager.createNativeQuery("UPDATE ratings SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", Instant.now().minusSeconds(50))
                .setParameter("id", rating2.getId())
                .executeUpdate();

        entityManager.createNativeQuery("UPDATE ratings SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", Instant.now())
                .setParameter("id", rating3.getId())
                .executeUpdate();

        // Clear the L1 cache so Hibernate loads the updated values from the database
        entityManager.clear();

        Page<Rating> page = ratingRepository.findByVendorIdOrderByCreatedAtDesc(testVendor.getId(), PageRequest.of(0, 10));
        List<Rating> ratings = page.getContent();

        assertEquals(3, ratings.size());
        assertEquals("Newest", ratings.get(0).getReviewText());
        assertEquals("Older", ratings.get(1).getReviewText());
        assertEquals("Oldest", ratings.get(2).getReviewText());
    }
}
