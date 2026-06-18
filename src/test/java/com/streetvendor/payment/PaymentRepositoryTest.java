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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:paymentrepotestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class PaymentRepositoryTest {

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

    @BeforeEach
    void setUp() {
        User customerUser = new User(UUID.randomUUID(), "repo-customer@example.com", "hashedPassword", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);

        Customer testCustomer = new Customer(UUID.randomUUID(), customerUser.getId(), "Repo Test Customer", "1112223333", "789 Repo St", null, null);
        customerRepository.save(testCustomer);

        User vendorUser = new User(UUID.randomUUID(), "repo-vendor@example.com", "hashedPassword", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);

        Vendor testVendor = new Vendor(UUID.randomUUID(), vendorUser, "Repo Test Vendor");
        vendorRepository.save(testVendor);

        testOrder = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("300.00"), UUID.randomUUID().toString());
        orderRepository.save(testOrder);
        orderRepository.flush();
    }

    // --- findByRazorpayOrderId ---

    @Test
    void findByRazorpayOrderId_existingValue_returnsPayment() {
        String razorpayOrderId = "order_RepoTest123";
        Payment payment = new Payment(UUID.randomUUID(), testOrder, razorpayOrderId, 30000);
        paymentRepository.save(payment);
        paymentRepository.flush();

        Optional<Payment> found = paymentRepository.findByRazorpayOrderId(razorpayOrderId);

        assertTrue(found.isPresent());
        assertEquals(razorpayOrderId, found.get().getRazorpayOrderId());
        assertEquals(testOrder.getId(), found.get().getOrder().getId());
    }

    @Test
    void findByRazorpayOrderId_missingValue_returnsEmpty() {
        Optional<Payment> found = paymentRepository.findByRazorpayOrderId("order_NonExistent");

        assertFalse(found.isPresent());
    }

    // --- findByRazorpayPaymentId ---

    @Test
    void findByRazorpayPaymentId_existingValue_returnsPayment() {
        String razorpayPaymentId = "pay_RepoTest456";
        Payment payment = new Payment(UUID.randomUUID(), testOrder, "order_RepoRPID1", 20000);
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);
        paymentRepository.flush();

        Optional<Payment> found = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId);

        assertTrue(found.isPresent());
        assertEquals(razorpayPaymentId, found.get().getRazorpayPaymentId());
        assertEquals(PaymentStatus.PAID, found.get().getStatus());
    }

    @Test
    void findByRazorpayPaymentId_missingValue_returnsEmpty() {
        Optional<Payment> found = paymentRepository.findByRazorpayPaymentId("pay_NonExistent");

        assertFalse(found.isPresent());
    }

    // --- findByOrderId ---

    @Test
    void findByOrderId_existingOrder_returnsPayment() {
        Payment payment = new Payment(UUID.randomUUID(), testOrder, "order_RepoOrdId1", 15000);
        paymentRepository.save(payment);
        paymentRepository.flush();

        Optional<Payment> found = paymentRepository.findByOrderId(testOrder.getId());

        assertTrue(found.isPresent());
        assertEquals(testOrder.getId(), found.get().getOrder().getId());
        assertEquals("order_RepoOrdId1", found.get().getRazorpayOrderId());
    }

    @Test
    void findByOrderId_missingOrder_returnsEmpty() {
        Optional<Payment> found = paymentRepository.findByOrderId(UUID.randomUUID());

        assertFalse(found.isPresent());
    }
}
