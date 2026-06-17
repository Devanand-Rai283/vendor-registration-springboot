package com.streetvendor.order;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.enums.PaymentStatus;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("order-test")
@Transactional
class OrderPersistenceTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VendorRepository vendorRepository;

    private Customer testCustomer;
    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        User customerUser = new User(UUID.randomUUID(), "customer@example.com", "hashedPassword", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);

        testCustomer = new Customer(UUID.randomUUID(), customerUser.getId(), "Test Customer", "1234567890", "123 Main St", null, null);
        customerRepository.save(testCustomer);

        User vendorUser = new User(UUID.randomUUID(), "vendor@example.com", "hashedPassword", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);

        testVendor = new Vendor(UUID.randomUUID(), vendorUser, "Test Vendor Business");
        vendorRepository.save(testVendor);
    }

    @Test
    void shouldSaveAndRetrieveOrder() {
        UUID orderId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        Order order = new Order(orderId, testCustomer, testVendor, new BigDecimal("150.00"), idempotencyKey);
        order.setNotes("Extra spicy please");

        orderRepository.save(order);
        orderRepository.flush();

        Optional<Order> found = orderRepository.findById(orderId);

        assertTrue(found.isPresent());
        assertEquals(orderId, found.get().getId());
        assertEquals(testCustomer.getId(), found.get().getCustomer().getId());
        assertEquals(testVendor.getId(), found.get().getVendor().getId());
        assertEquals(OrderStatus.PLACED, found.get().getStatus());
        assertEquals(new BigDecimal("150.00"), found.get().getTotalAmount());
        assertEquals(PaymentStatus.PENDING, found.get().getPaymentStatus());
        assertEquals(idempotencyKey, found.get().getIdempotencyKey());
        assertEquals("Extra spicy please", found.get().getNotes());
        assertNotNull(found.get().getCreatedAt());
        assertNotNull(found.get().getUpdatedAt());
    }

    @Test
    void shouldStoreOrderStatusAsString() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, testCustomer, testVendor, new BigDecimal("100.00"), UUID.randomUUID().toString());
        order.setStatus(OrderStatus.ACCEPTED);

        orderRepository.save(order);
        orderRepository.flush();

        Optional<Order> found = orderRepository.findById(orderId);
        assertTrue(found.isPresent());
        assertEquals(OrderStatus.ACCEPTED, found.get().getStatus());
    }

    @Test
    void shouldStorePaymentStatusAsString() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, testCustomer, testVendor, new BigDecimal("100.00"), UUID.randomUUID().toString());
        order.setPaymentStatus(PaymentStatus.PAID);

        orderRepository.save(order);
        orderRepository.flush();

        Optional<Order> found = orderRepository.findById(orderId);
        assertTrue(found.isPresent());
        assertEquals(PaymentStatus.PAID, found.get().getPaymentStatus());
    }

    @Test
    void shouldDefaultStatusToPlaced() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, testCustomer, testVendor, new BigDecimal("50.00"), UUID.randomUUID().toString());

        orderRepository.save(order);
        orderRepository.flush();

        Optional<Order> found = orderRepository.findById(orderId);
        assertTrue(found.isPresent());
        assertEquals(OrderStatus.PLACED, found.get().getStatus());
    }

    @Test
    void shouldDefaultPaymentStatusToPending() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, testCustomer, testVendor, new BigDecimal("50.00"), UUID.randomUUID().toString());

        orderRepository.save(order);
        orderRepository.flush();

        Optional<Order> found = orderRepository.findById(orderId);
        assertTrue(found.isPresent());
        assertEquals(PaymentStatus.PENDING, found.get().getPaymentStatus());
    }

    @Test
    void shouldRejectDuplicateIdempotencyKey() {
        String idempotencyKey = UUID.randomUUID().toString();

        Order order1 = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("100.00"), idempotencyKey);
        orderRepository.save(order1);
        orderRepository.flush();

        Order order2 = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("200.00"), idempotencyKey);

        assertThrows(DataIntegrityViolationException.class, () -> {
            orderRepository.save(order2);
            orderRepository.flush();
        });
    }

    @Test
    void shouldFindByCustomerId() {
        Order order = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("100.00"), UUID.randomUUID().toString());
        orderRepository.save(order);
        orderRepository.flush();

        List<Order> found = orderRepository.findByCustomerId(testCustomer.getId());

        assertFalse(found.isEmpty());
        assertEquals(1, found.size());
        assertEquals(testCustomer.getId(), found.get(0).getCustomer().getId());
    }

    @Test
    void shouldFindByVendorId() {
        Order order = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("100.00"), UUID.randomUUID().toString());
        orderRepository.save(order);
        orderRepository.flush();

        List<Order> found = orderRepository.findByVendorId(testVendor.getId());

        assertFalse(found.isEmpty());
        assertEquals(1, found.size());
        assertEquals(testVendor.getId(), found.get(0).getVendor().getId());
    }

    @Test
    void shouldFindByIdempotencyKey() {
        String idempotencyKey = UUID.randomUUID().toString();
        Order order = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("100.00"), idempotencyKey);
        orderRepository.save(order);
        orderRepository.flush();

        Optional<Order> found = orderRepository.findByIdempotencyKey(idempotencyKey);

        assertTrue(found.isPresent());
        assertEquals(idempotencyKey, found.get().getIdempotencyKey());
    }

    @Test
    void shouldReturnEmptyForNonExistentIdempotencyKey() {
        Optional<Order> found = orderRepository.findByIdempotencyKey("non-existent-key");

        assertFalse(found.isPresent());
    }

    @Test
    void shouldFindByStatus() {
        Order order1 = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("100.00"), UUID.randomUUID().toString());
        order1.setStatus(OrderStatus.PREPARING);
        orderRepository.save(order1);

        Order order2 = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("200.00"), UUID.randomUUID().toString());
        order2.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order2);
        orderRepository.flush();

        List<Order> preparing = orderRepository.findByStatus(OrderStatus.PREPARING);
        assertEquals(1, preparing.size());

        List<Order> completed = orderRepository.findByStatus(OrderStatus.COMPLETED);
        assertEquals(1, completed.size());
    }

    @Test
    void shouldPersistAllOrderStatusValues() {
        for (OrderStatus status : OrderStatus.values()) {
            Order order = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("10.00"), UUID.randomUUID().toString());
            order.setStatus(status);
            orderRepository.save(order);
        }
        orderRepository.flush();

        List<Order> all = orderRepository.findAll();
        assertEquals(OrderStatus.values().length, all.size());
    }

    @Test
    void shouldPersistAllPaymentStatusValues() {
        for (PaymentStatus status : PaymentStatus.values()) {
            Order order = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("10.00"), UUID.randomUUID().toString());
            order.setPaymentStatus(status);
            orderRepository.save(order);
        }
        orderRepository.flush();

        List<Order> all = orderRepository.findAll();
        assertEquals(PaymentStatus.values().length, all.size());
    }
}
