package com.streetvendor.payment;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.payment.entity.Payment;
import com.streetvendor.payment.enums.PaymentStatus;
import com.streetvendor.payment.repository.PaymentRepository;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:paymenttestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.open-in-view=false",
    "spring.flyway.enabled=false",
    "spring.main.allow-bean-definition-overriding=true",
    "jwt.secret=dGhpc0lzQVZlcnlTZWN1dGVLZXlGb3JKV1RUb2tlbkdlbmVyYXRpb24=",
    "jwt.access-expiration=900000",
    "jwt.refresh-expiration=30",
    "r2.access-key=test-access-key",
    "r2.secret-key=test-secret-key",
    "r2.bucket-name=test-bucket",
    "r2.region=auto",
    "r2.endpoint=https://test.r2.cloudflarestorage.com"
})
@Transactional
class PaymentPersistenceTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VendorRepository vendorRepository;

    private Order testOrder;
    private Customer testCustomer;
    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        User customerUser = new User(UUID.randomUUID(), "payment-customer@example.com", "hashedPassword", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);

        testCustomer = new Customer(UUID.randomUUID(), customerUser.getId(), "Payment Test Customer", "9876543210", "456 Test St", null, null);
        customerRepository.save(testCustomer);

        User vendorUser = new User(UUID.randomUUID(), "payment-vendor@example.com", "hashedPassword", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);

        testVendor = new Vendor(UUID.randomUUID(), vendorUser, "Payment Test Vendor");
        vendorRepository.save(testVendor);

        testOrder = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("250.00"), UUID.randomUUID().toString());
        orderRepository.save(testOrder);
        orderRepository.flush();
    }

    @Test
    void shouldSaveAndRetrievePayment() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment(paymentId, testOrder, "order_RPay123", 25000);

        paymentRepository.save(payment);
        paymentRepository.flush();

        Optional<Payment> found = paymentRepository.findById(paymentId);

        assertTrue(found.isPresent());
        assertEquals(paymentId, found.get().getId());
        assertEquals(testOrder.getId(), found.get().getOrder().getId());
        assertEquals("order_RPay123", found.get().getRazorpayOrderId());
        assertNull(found.get().getRazorpayPaymentId());
        assertEquals(25000, found.get().getAmount());
        assertEquals("INR", found.get().getCurrency());
        assertEquals(PaymentStatus.CREATED, found.get().getStatus());
        assertNull(found.get().getPaidAt());
        assertNotNull(found.get().getCreatedAt());
    }

    @Test
    void shouldDefaultStatusToCreated() {
        Payment payment = new Payment(UUID.randomUUID(), testOrder, "order_RPay456", 10000);

        paymentRepository.save(payment);
        paymentRepository.flush();

        Optional<Payment> found = paymentRepository.findById(payment.getId());
        assertTrue(found.isPresent());
        assertEquals(PaymentStatus.CREATED, found.get().getStatus());
    }

    @Test
    void shouldDefaultCurrencyToINR() {
        Payment payment = new Payment(UUID.randomUUID(), testOrder, "order_RPay789", 5000);

        paymentRepository.save(payment);
        paymentRepository.flush();

        Optional<Payment> found = paymentRepository.findById(payment.getId());
        assertTrue(found.isPresent());
        assertEquals("INR", found.get().getCurrency());
    }

    @Test
    void shouldPopulateCreatedAt() {
        Payment payment = new Payment(UUID.randomUUID(), testOrder, "order_RPayAudit", 15000);

        paymentRepository.save(payment);
        paymentRepository.flush();

        Optional<Payment> found = paymentRepository.findById(payment.getId());
        assertTrue(found.isPresent());
        assertNotNull(found.get().getCreatedAt());
    }

    @Test
    void shouldAllowNullPaidAt() {
        Payment payment = new Payment(UUID.randomUUID(), testOrder, "order_RPayNull1", 8000);

        paymentRepository.save(payment);
        paymentRepository.flush();

        Optional<Payment> found = paymentRepository.findById(payment.getId());
        assertTrue(found.isPresent());
        assertNull(found.get().getPaidAt());
    }

    @Test
    void shouldAllowNullRazorpayPaymentId() {
        Payment payment = new Payment(UUID.randomUUID(), testOrder, "order_RPayNull2", 9000);

        paymentRepository.save(payment);
        paymentRepository.flush();

        Optional<Payment> found = paymentRepository.findById(payment.getId());
        assertTrue(found.isPresent());
        assertNull(found.get().getRazorpayPaymentId());
    }

    @Test
    void shouldRejectDuplicateOrderId() {
        Payment payment1 = new Payment(UUID.randomUUID(), testOrder, "order_RPay_dup1", 10000);
        paymentRepository.save(payment1);
        paymentRepository.flush();

        Payment payment2 = new Payment(UUID.randomUUID(), testOrder, "order_RPay_dup2", 10000);

        assertThrows(DataIntegrityViolationException.class, () -> {
            paymentRepository.save(payment2);
            paymentRepository.flush();
        });
    }

    @Test
    void shouldRejectDuplicateRazorpayPaymentId() {
        Payment payment1 = new Payment(UUID.randomUUID(), testOrder, "order_RPay_rpid1", 10000);
        payment1.setRazorpayPaymentId("pay_DUPLICATE");
        paymentRepository.save(payment1);
        paymentRepository.flush();

        Order secondOrder = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("100.00"), UUID.randomUUID().toString());
        orderRepository.save(secondOrder);
        orderRepository.flush();

        Payment payment2 = new Payment(UUID.randomUUID(), secondOrder, "order_RPay_rpid2", 10000);
        payment2.setRazorpayPaymentId("pay_DUPLICATE");

        assertThrows(DataIntegrityViolationException.class, () -> {
            paymentRepository.save(payment2);
            paymentRepository.flush();
        });
    }

    @Test
    void shouldPersistCreatedStatus() {
        Payment payment = new Payment(UUID.randomUUID(), testOrder, "order_RPay_enum1", 5000);
        payment.setStatus(PaymentStatus.CREATED);

        paymentRepository.save(payment);
        paymentRepository.flush();

        Optional<Payment> found = paymentRepository.findById(payment.getId());
        assertTrue(found.isPresent());
        assertEquals(PaymentStatus.CREATED, found.get().getStatus());
    }

    @Test
    void shouldPersistPaidStatus() {
        Payment payment = new Payment(UUID.randomUUID(), testOrder, "order_RPay_enum2", 5000);
        payment.setStatus(PaymentStatus.PAID);

        paymentRepository.save(payment);
        paymentRepository.flush();

        Optional<Payment> found = paymentRepository.findById(payment.getId());
        assertTrue(found.isPresent());
        assertEquals(PaymentStatus.PAID, found.get().getStatus());
    }

    @Test
    void shouldPersistFailedStatus() {
        Payment payment = new Payment(UUID.randomUUID(), testOrder, "order_RPay_enum3", 5000);
        payment.setStatus(PaymentStatus.FAILED);

        paymentRepository.save(payment);
        paymentRepository.flush();

        Optional<Payment> found = paymentRepository.findById(payment.getId());
        assertTrue(found.isPresent());
        assertEquals(PaymentStatus.FAILED, found.get().getStatus());
    }

    @Test
    void shouldPersistAllPaymentStatusValues() {
        for (int i = 0; i < PaymentStatus.values().length; i++) {
            PaymentStatus status = PaymentStatus.values()[i];

            Order order = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("10.00"), UUID.randomUUID().toString());
            orderRepository.save(order);

            Payment payment = new Payment(UUID.randomUUID(), order, "order_RPay_all_" + i, 1000);
            payment.setStatus(status);
            paymentRepository.save(payment);
        }
        paymentRepository.flush();

        long count = paymentRepository.count();
        assertEquals(PaymentStatus.values().length, count);
    }

    @Test
    void shouldRejectPaymentWithNonExistentOrder() {
        User tempUser = new User(UUID.randomUUID(), "temp-fk@example.com", "hashedPassword", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(tempUser);
        Customer tempCustomer = new Customer(UUID.randomUUID(), tempUser.getId(), "Temp", "0000000000", "Nowhere", null, null);
        customerRepository.save(tempCustomer);

        Order unsavedOrder = new Order(UUID.randomUUID(), tempCustomer, testVendor, new BigDecimal("50.00"), UUID.randomUUID().toString());

        Payment payment = new Payment(UUID.randomUUID(), unsavedOrder, "order_RPay_fk", 5000);

        assertThrows(Exception.class, () -> {
            paymentRepository.save(payment);
            paymentRepository.flush();
        });
    }
}
