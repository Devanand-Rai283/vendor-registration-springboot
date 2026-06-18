package com.streetvendor.payment;

import com.razorpay.RazorpayClient;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.payment.entity.Payment;
import com.streetvendor.payment.enums.PaymentStatus;
import com.streetvendor.payment.repository.PaymentRepository;
import com.streetvendor.payment.service.PaymentWebhookService;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:webhookservicetestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
    "r2.endpoint=https://test.r2.cloudflarestorage.com",
    "razorpay.key-id=test-key-id",
    "razorpay.key-secret=test-key-secret",
    "razorpay.webhook-secret=test-webhook-secret"
})
@Transactional
class PaymentWebhookServiceTest {

    @Autowired
    private PaymentWebhookService paymentWebhookService;

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

    @MockitoBean
    private RazorpayClient razorpayClient;

    private Order testOrder;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        User customerUser = new User(UUID.randomUUID(), "webhook-customer@example.com", "pass", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);

        Customer customer = new Customer(UUID.randomUUID(), customerUser.getId(), "Webhook Customer", "9876543210", "10 Webhook St", null, null);
        customerRepository.save(customer);

        User vendorUser = new User(UUID.randomUUID(), "webhook-vendor@example.com", "pass", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);

        Vendor vendor = new Vendor(UUID.randomUUID(), vendorUser, "Webhook Vendor");
        vendorRepository.save(vendor);

        testOrder = new Order(UUID.randomUUID(), customer, vendor, new BigDecimal("250.00"), UUID.randomUUID().toString());
        orderRepository.save(testOrder);

        // razorpay_order_id is set during PAYMENT-001 checkout
        testPayment = new Payment(UUID.randomUUID(), testOrder, "order_RPay_Webhook", 25000);
        paymentRepository.save(testPayment);
    }

    // ------------------------------------------------------------------ //
    // payment.captured — happy path                                       //
    // ------------------------------------------------------------------ //

    @Test
    void shouldTransitionPaymentToPaidOnCapture() {
        paymentWebhookService.processEvent(
                "payment.captured",
                "order_RPay_Webhook",
                "pay_RPay_Webhook_001",
                25000L,
                "INR");

        Payment updated = paymentRepository.findByOrderId(testOrder.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updated.getRazorpayPaymentId()).isEqualTo("pay_RPay_Webhook_001");
        assertThat(updated.getPaidAt()).isNotNull();
        assertThat(updated.getPaidAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void shouldUpdateOrderPaymentStatusToPaidOnCapture() {
        paymentWebhookService.processEvent(
                "payment.captured",
                "order_RPay_Webhook",
                "pay_RPay_Webhook_002",
                25000L,
                "INR");

        Order updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertThat(updatedOrder.getPaymentStatus())
                .isEqualTo(com.streetvendor.order.enums.PaymentStatus.PAID);
    }

    // ------------------------------------------------------------------ //
    // payment.captured — idempotency                                     //
    // ------------------------------------------------------------------ //

    @Test
    void shouldIgnoreDuplicateCaptureEventWhenAlreadyPaid() {
        // First delivery
        testPayment.setStatus(PaymentStatus.PAID);
        testPayment.setRazorpayPaymentId("pay_RPay_Existing");
        testPayment.setPaidAt(Instant.now());
        paymentRepository.save(testPayment);

        // Duplicate delivery — must not throw and must not mutate state
        paymentWebhookService.processEvent(
                "payment.captured",
                "order_RPay_Webhook",
                "pay_RPay_Existing",
                25000L,
                "INR");

        Payment unchanged = paymentRepository.findByOrderId(testOrder.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(unchanged.getRazorpayPaymentId()).isEqualTo("pay_RPay_Existing");
    }

    @Test
    void shouldIgnoreCaptureEventWhenPaymentIsAlreadyFailed() {
        testPayment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(testPayment);

        // Must not throw or transition state
        paymentWebhookService.processEvent(
                "payment.captured",
                "order_RPay_Webhook",
                "pay_RPay_Late",
                25000L,
                "INR");

        Payment unchanged = paymentRepository.findByOrderId(testOrder.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    // ------------------------------------------------------------------ //
    // payment.captured — amount mismatch                                 //
    // ------------------------------------------------------------------ //

    @Test
    void shouldNotUpdatePaymentOnAmountMismatch() {
        // Send different amount than what is stored (25000 vs 10000)
        paymentWebhookService.processEvent(
                "payment.captured",
                "order_RPay_Webhook",
                "pay_RPay_Tampered",
                10000L,
                "INR");

        Payment unchanged = paymentRepository.findByOrderId(testOrder.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(unchanged.getRazorpayPaymentId()).isNull();
    }

    // ------------------------------------------------------------------ //
    // payment.captured — currency mismatch                               //
    // ------------------------------------------------------------------ //

    @Test
    void shouldThrowIllegalArgumentExceptionOnCurrencyMismatch() {
        assertThatThrownBy(() ->
            paymentWebhookService.processEvent(
                    "payment.captured",
                    "order_RPay_Webhook",
                    "pay_RPay_Webhook_001",
                    25000L,
                    "USD")
        ).isInstanceOf(IllegalArgumentException.class);

        Payment unchanged = paymentRepository.findByOrderId(testOrder.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(unchanged.getRazorpayPaymentId()).isNull();
    }

    // ------------------------------------------------------------------ //
    // payment.failed — happy path                                        //
    // ------------------------------------------------------------------ //

    @Test
    void shouldTransitionPaymentToFailedOnFailureEvent() {
        paymentWebhookService.processEvent(
                "payment.failed",
                "order_RPay_Webhook",
                null,
                0L,
                "INR");

        Payment updated = paymentRepository.findByOrderId(testOrder.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void shouldUpdateOrderPaymentStatusToFailedOnFailureEvent() {
        paymentWebhookService.processEvent(
                "payment.failed",
                "order_RPay_Webhook",
                null,
                0L,
                "INR");

        Order updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertThat(updatedOrder.getPaymentStatus())
                .isEqualTo(com.streetvendor.order.enums.PaymentStatus.FAILED);
    }

    @Test
    void shouldNotChangeOrderStatusOnPaymentFailed() {
        com.streetvendor.order.enums.OrderStatus originalStatus = testOrder.getStatus();

        paymentWebhookService.processEvent(
                "payment.failed",
                "order_RPay_Webhook",
                null,
                0L,
                "INR");

        Order updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
        // Order.status must remain unchanged — only paymentStatus changes (Q3)
        assertThat(updatedOrder.getStatus()).isEqualTo(originalStatus);
    }

    // ------------------------------------------------------------------ //
    // payment.failed — idempotency                                       //
    // ------------------------------------------------------------------ //

    @Test
    void shouldIgnoreDuplicateFailedEventWhenAlreadyFailed() {
        testPayment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(testPayment);

        // Duplicate — must not throw
        paymentWebhookService.processEvent(
                "payment.failed",
                "order_RPay_Webhook",
                null,
                0L,
                "INR");

        Payment unchanged = paymentRepository.findByOrderId(testOrder.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    // ------------------------------------------------------------------ //
    // Unknown event types                                                 //
    // ------------------------------------------------------------------ //

    @Test
    void shouldIgnoreUnknownEventType() {
        // Must not throw or mutate state
        paymentWebhookService.processEvent(
                "refund.created",
                "order_RPay_Webhook",
                null,
                0L,
                "INR");

        Payment unchanged = paymentRepository.findByOrderId(testOrder.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(PaymentStatus.CREATED);
    }

    // ------------------------------------------------------------------ //
    // Payment record not found                                           //
    // ------------------------------------------------------------------ //

    @Test
    void shouldThrowResourceNotFoundExceptionForUnknownRazorpayOrderId() {
        assertThatThrownBy(() ->
            paymentWebhookService.processEvent(
                    "payment.captured",
                    "order_nonexistent",
                    "pay_xyz",
                    25000L,
                    "INR")
        ).isInstanceOf(ResourceNotFoundException.class);
    }
}
