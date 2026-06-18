package com.streetvendor.payment;

import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.payment.dto.CreatePaymentOrderResponse;
import com.streetvendor.payment.dto.PaymentVerificationResponse;
import com.streetvendor.payment.entity.Payment;
import com.streetvendor.payment.enums.PaymentStatus;
import com.streetvendor.payment.repository.PaymentRepository;
import com.streetvendor.payment.service.PaymentService;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:paymentservicetestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

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

    private OrderClient mockOrderClient;

    private User customerUser;
    private User otherCustomerUser;
    private Customer testCustomer;
    private Customer otherCustomer;
    private Vendor testVendor;
    private Order testOrder;

    @BeforeEach
    void setUp() throws Exception {
        mockOrderClient = mock(OrderClient.class);
        razorpayClient.orders = mockOrderClient;

        // Create Customer User
        customerUser = new User(UUID.randomUUID(), "service-customer@example.com", "pass", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);

        testCustomer = new Customer(UUID.randomUUID(), customerUser.getId(), "Test Customer", "1234567890", "123 Test St", null, null);
        customerRepository.save(testCustomer);

        // Create Other Customer (for ownership check)
        otherCustomerUser = new User(UUID.randomUUID(), "other-customer@example.com", "pass", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(otherCustomerUser);

        otherCustomer = new Customer(UUID.randomUUID(), otherCustomerUser.getId(), "Other Customer", "0987654321", "456 Other St", null, null);
        customerRepository.save(otherCustomer);

        // Create Vendor User
        User vendorUser = new User(UUID.randomUUID(), "service-vendor@example.com", "pass", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);

        testVendor = new Vendor(UUID.randomUUID(), vendorUser, "Test Vendor");
        vendorRepository.save(testVendor);

        // Create Order (Rups 250.00 -> 25000 paise)
        testOrder = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("250.00"), UUID.randomUUID().toString());
        orderRepository.save(testOrder);
    }

    @Test
    void shouldCreateRazorpayOrderSuccessfully() throws Exception {
        com.razorpay.Order mockRazorpayOrder = mock(com.razorpay.Order.class);
        when(mockRazorpayOrder.get("id")).thenReturn("order_RPay12345");
        when(mockOrderClient.create(any(JSONObject.class))).thenReturn(mockRazorpayOrder);

        CreatePaymentOrderResponse response = paymentService.createPaymentOrder(testOrder.getId(), customerUser);

        assertNotNull(response);
        assertEquals("order_RPay12345", response.razorpayOrderId());
        assertEquals(25000, response.amount());
        assertEquals("INR", response.currency());
        assertEquals("CREATED", response.status());

        // Verify database state
        Payment updatedPayment = paymentRepository.findByOrderId(testOrder.getId()).orElseThrow();
        assertEquals("order_RPay12345", updatedPayment.getRazorpayOrderId());
        assertEquals(PaymentStatus.CREATED, updatedPayment.getStatus());
    }

    @Test
    void shouldThrowForbiddenExceptionWhenNonOwnerAttemptsToPay() {
        assertThrows(ForbiddenException.class, () -> 
            paymentService.createPaymentOrder(testOrder.getId(), otherCustomerUser)
        );
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenOrderDoesNotExistOnCreation() {
        assertThrows(ResourceNotFoundException.class, () -> 
            paymentService.createPaymentOrder(UUID.randomUUID(), customerUser)
        );
    }

    @Test
    void shouldThrowConflictExceptionWhenOrderIsCancelled() {
        testOrder.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(testOrder);

        assertThrows(ConflictException.class, () -> 
            paymentService.createPaymentOrder(testOrder.getId(), customerUser)
        );
    }

    @Test
    void shouldThrowConflictExceptionWhenPaymentIsAlreadyPaid() {
        Payment paidPayment = new Payment(UUID.randomUUID(), testOrder, "rpay_ord_paid", 25000);
        paidPayment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(paidPayment);

        assertThrows(ConflictException.class, () -> 
            paymentService.createPaymentOrder(testOrder.getId(), customerUser)
        );
    }

    @Test
    void shouldCreatePaymentRecordOnTheFlyIfNotExist() throws Exception {
        // Create an order without payment record
        Order orderWithoutPayment = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("100.00"), UUID.randomUUID().toString());
        orderRepository.save(orderWithoutPayment);

        com.razorpay.Order mockRazorpayOrder = mock(com.razorpay.Order.class);
        when(mockRazorpayOrder.get("id")).thenReturn("order_new_RPay");
        when(mockOrderClient.create(any(JSONObject.class))).thenReturn(mockRazorpayOrder);

        CreatePaymentOrderResponse response = paymentService.createPaymentOrder(orderWithoutPayment.getId(), customerUser);

        assertNotNull(response);
        assertEquals("order_new_RPay", response.razorpayOrderId());
        assertEquals(10000, response.amount());

        // Verify it was persisted in the repository
        Payment newPayment = paymentRepository.findByOrderId(orderWithoutPayment.getId()).orElse(null);
        assertNotNull(newPayment);
        assertEquals("order_new_RPay", newPayment.getRazorpayOrderId());
        assertEquals(PaymentStatus.CREATED, newPayment.getStatus());
    }

    @Test
    void shouldReturnExistingPaymentResponseForIdempotency() throws Exception {
        // Prepare payment state: CREATED with razorpayOrderId
        Payment existingPayment = new Payment(UUID.randomUUID(), testOrder, "order_existing_idempotent", 25000);
        existingPayment.setStatus(PaymentStatus.CREATED);
        paymentRepository.save(existingPayment);

        // Call service: should not interact with Razorpay client mock but return existing payment directly
        CreatePaymentOrderResponse response = paymentService.createPaymentOrder(testOrder.getId(), customerUser);

        assertNotNull(response);
        assertEquals("order_existing_idempotent", response.razorpayOrderId());
        assertEquals(25000, response.amount());
        assertEquals("CREATED", response.status());
    }

    @Test
    void shouldReturnPaymentStatusOnVerificationForOwner() {
        Payment existingPayment = new Payment(UUID.randomUUID(), testOrder, "order_verified_rpay", 25000);
        existingPayment.setStatus(PaymentStatus.PAID);
        existingPayment.setRazorpayPaymentId("pay_verified_123");
        paymentRepository.save(existingPayment);

        PaymentVerificationResponse response = paymentService.verifyPaymentStatus(testOrder.getId(), customerUser);

        assertNotNull(response);
        assertEquals(existingPayment.getId(), response.paymentId());
        assertEquals(testOrder.getId(), response.orderId());
        assertEquals("PAID", response.paymentStatus());
        assertEquals("PENDING", response.orderPaymentStatus());
        assertEquals("PLACED", response.orderStatus());
        assertEquals("order_verified_rpay", response.razorpayOrderId());
        assertEquals("pay_verified_123", response.razorpayPaymentId());
    }

    @Test
    void shouldReturnCreatedStatusDuringWebhookDelay() {
        Payment existingPayment = new Payment(UUID.randomUUID(), testOrder, "order_delay_rpay", 25000);
        existingPayment.setStatus(PaymentStatus.CREATED);
        paymentRepository.save(existingPayment);

        PaymentVerificationResponse response = paymentService.verifyPaymentStatus(testOrder.getId(), customerUser);

        assertNotNull(response);
        assertEquals("CREATED", response.paymentStatus());
        assertEquals("PENDING", response.orderPaymentStatus());
        assertEquals("PLACED", response.orderStatus());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenOrderDoesNotExistOnVerification() {
        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.verifyPaymentStatus(UUID.randomUUID(), customerUser)
        );
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenPaymentDoesNotExist() {
        Order orderWithoutPayment = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("100.00"), UUID.randomUUID().toString());
        orderRepository.save(orderWithoutPayment);

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.verifyPaymentStatus(orderWithoutPayment.getId(), customerUser)
        );
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenNonOwnerAttemptsVerification() {
        Payment existingPayment = new Payment(UUID.randomUUID(), testOrder, "order_other_rpay", 25000);
        paymentRepository.save(existingPayment);

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.verifyPaymentStatus(testOrder.getId(), otherCustomerUser)
        );
    }
}
