package com.streetvendor.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.payment.dto.CreatePaymentOrderRequest;
import com.streetvendor.payment.dto.CreatePaymentOrderResponse;
import com.streetvendor.payment.service.PaymentService;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
class PaymentControllerTest extends AbstractSecurityTest {

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String customerToken;
    private String vendorToken;
    private User customerUser;

    @BeforeEach
    void setUp() {
        UUID customerUserId = UUID.randomUUID();
        customerUser = new User(customerUserId, "pay-customer@example.com", "pass", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);

        UUID vendorUserId = UUID.randomUUID();
        User vendorUser = new User(vendorUserId, "pay-vendor@example.com", "pass", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);

        customerToken = jwtService.generateAccessToken(customerUserId, "pay-customer@example.com", "CUSTOMER");
        vendorToken = jwtService.generateAccessToken(vendorUserId, "pay-vendor@example.com", "VENDOR");
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(UUID.randomUUID());

        mockMvc.perform(post("/api/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenAuthenticatedAsVendor() throws Exception {
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(UUID.randomUUID());

        mockMvc.perform(post("/api/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendorToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200OnSuccessfulOrderCreation() throws Exception {
        UUID orderId = UUID.randomUUID();
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(orderId);
        CreatePaymentOrderResponse response = new CreatePaymentOrderResponse(
                UUID.randomUUID(),
                "order_RPay123",
                25000,
                "INR",
                "CREATED"
        );

        when(paymentService.createPaymentOrder(eq(orderId), any(User.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(response.paymentId().toString()))
                .andExpect(jsonPath("$.razorpayOrderId").value("order_RPay123"))
                .andExpect(jsonPath("$.amount").value(25000))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void shouldReturn400OnValidationFailure_MissingOrderId() throws Exception {
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(null);

        mockMvc.perform(post("/api/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401OnVerifyWhenNotAuthenticated() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/payments/orders/" + UUID.randomUUID() + "/verify"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403OnVerifyWhenAuthenticatedAsVendor() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/payments/orders/" + UUID.randomUUID() + "/verify")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200OnSuccessfulVerify() throws Exception {
        UUID orderId = UUID.randomUUID();
        com.streetvendor.payment.dto.PaymentVerificationResponse response = new com.streetvendor.payment.dto.PaymentVerificationResponse(
                UUID.randomUUID(),
                orderId,
                "PAID",
                "PAID",
                "CONFIRMED",
                "order_RPay123",
                "pay_RPay123"
        );

        when(paymentService.verifyPaymentStatus(eq(orderId), any(User.class))).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/payments/orders/" + orderId + "/verify")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(response.paymentId().toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.orderPaymentStatus").value("PAID"))
                .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.razorpayOrderId").value("order_RPay123"))
                .andExpect(jsonPath("$.razorpayPaymentId").value("pay_RPay123"));
    }

    @Test
    void shouldReturn200WithCreatedStatusOnVerify() throws Exception {
        UUID orderId = UUID.randomUUID();
        com.streetvendor.payment.dto.PaymentVerificationResponse response = new com.streetvendor.payment.dto.PaymentVerificationResponse(
                UUID.randomUUID(),
                orderId,
                "CREATED",
                "PENDING",
                "PLACED",
                "order_RPay123",
                null
        );

        when(paymentService.verifyPaymentStatus(eq(orderId), any(User.class))).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/payments/orders/" + orderId + "/verify")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("CREATED"))
                .andExpect(jsonPath("$.orderPaymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.orderStatus").value("PLACED"))
                .andExpect(jsonPath("$.razorpayPaymentId").isEmpty());
    }

    @Test
    void shouldReturn404OnVerifyWhenOrderDoesNotExist() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(paymentService.verifyPaymentStatus(eq(orderId), any(User.class)))
                .thenThrow(new com.streetvendor.common.exception.ResourceNotFoundException("Order not found"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/payments/orders/" + orderId + "/verify")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound());
    }
}
