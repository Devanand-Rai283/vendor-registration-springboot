package com.streetvendor.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.order.dto.OrderResponse;
import com.streetvendor.order.dto.UpdateOrderStatusRequest;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.enums.PaymentStatus;
import com.streetvendor.order.exception.InvalidOrderStatusTransitionException;
import com.streetvendor.order.exception.OrderAlreadyFinalizedException;
import com.streetvendor.order.service.OrderProcessingService;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
class OrderProcessingControllerTest extends AbstractSecurityTest {

    @MockitoBean
    private OrderProcessingService orderProcessingService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String vendorToken;
    private String customerToken;
    private UUID vendorUserId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        vendorUserId = UUID.randomUUID();
        User vendorUser = new User(vendorUserId, "vendor@example.com", "pass", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);

        UUID customerUserId = UUID.randomUUID();
        User customerUser = new User(customerUserId, "customer@example.com", "pass", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);

        vendorToken = jwtService.generateAccessToken(vendorUserId, "vendor@example.com", "VENDOR");
        customerToken = jwtService.generateAccessToken(customerUserId, "customer@example.com", "CUSTOMER");
        orderId = UUID.randomUUID();
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.ACCEPTED);

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenAuthenticatedAsCustomer() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.ACCEPTED);

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200OnSuccessfulTransition() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.ACCEPTED);
        OrderResponse response = new OrderResponse(
                orderId,
                OrderStatus.ACCEPTED,
                PaymentStatus.PENDING,
                new BigDecimal("100.00"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now()
        );

        when(orderProcessingService.updateStatus(eq(orderId), eq(OrderStatus.ACCEPTED), eq(vendorUserId))).thenReturn(response);

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendorToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void shouldReturn400WhenTransitionIsInvalid() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.PREPARING);

        when(orderProcessingService.updateStatus(eq(orderId), eq(OrderStatus.PREPARING), eq(vendorUserId)))
                .thenThrow(new InvalidOrderStatusTransitionException("Invalid transition from PLACED to PREPARING"));

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendorToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid transition from PLACED to PREPARING"));
    }

    @Test
    void shouldReturn400WhenOrderIsAlreadyFinalized() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.ACCEPTED);

        when(orderProcessingService.updateStatus(eq(orderId), eq(OrderStatus.ACCEPTED), eq(vendorUserId)))
                .thenThrow(new OrderAlreadyFinalizedException("Cannot transition from finalized status: COMPLETED"));

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendorToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot transition from finalized status: COMPLETED"));
    }

    @Test
    void shouldReturn400WhenStatusIsNull() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(null);

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendorToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
