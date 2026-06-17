package com.streetvendor.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.order.dto.OrderItemRequest;
import com.streetvendor.order.dto.PlaceOrderRequest;
import com.streetvendor.order.dto.PlaceOrderResponse;
import com.streetvendor.order.dto.PlaceOrderResult;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.service.OrderService;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
class OrderControllerTest extends AbstractSecurityTest {

    @MockitoBean
    private OrderService orderService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String customerToken;
    private String vendorToken;
    private UUID customerUserId;

    @BeforeEach
    void setUp() {
        customerUserId = UUID.randomUUID();
        User customerUser = new User(customerUserId, "customer@example.com", "pass", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);

        UUID vendorUserId = UUID.randomUUID();
        User vendorUser = new User(vendorUserId, "vendor@example.com", "pass", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);

        customerToken = jwtService.generateAccessToken(customerUserId, "customer@example.com", "CUSTOMER");
        vendorToken = jwtService.generateAccessToken(vendorUserId, "vendor@example.com", "VENDOR");
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(UUID.randomUUID(), 2)),
                "Spicy"
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", "key-123")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenAuthenticatedAsVendor() throws Exception {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(UUID.randomUUID(), 2)),
                "Spicy"
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendorToken)
                        .header("X-Idempotency-Key", "key-123")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn201OnSuccessfulNewOrder() throws Exception {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(UUID.randomUUID(), 2)),
                "Spicy"
        );
        UUID orderId = UUID.randomUUID();
        PlaceOrderResponse response = new PlaceOrderResponse(
                orderId,
                OrderStatus.PLACED,
                new BigDecimal("100.00"),
                Instant.now()
        );
        PlaceOrderResult result = new PlaceOrderResult(response, false);

        when(orderService.placeOrder(eq(customerUserId), eq("key-123"), any())).thenReturn(result);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .header("X-Idempotency-Key", "key-123")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(100.00))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturn200OnDuplicateOrderIdempotencyHit() throws Exception {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(UUID.randomUUID(), 2)),
                "Spicy"
        );
        UUID orderId = UUID.randomUUID();
        PlaceOrderResponse response = new PlaceOrderResponse(
                orderId,
                OrderStatus.PLACED,
                new BigDecimal("100.00"),
                Instant.now()
        );
        PlaceOrderResult result = new PlaceOrderResult(response, true);

        when(orderService.placeOrder(eq(customerUserId), eq("key-123"), any())).thenReturn(result);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .header("X-Idempotency-Key", "key-123")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(100.00))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturn400OnValidationFailure_EmptyItems() throws Exception {
        PlaceOrderRequest request = new PlaceOrderRequest(
                Collections.emptyList(),
                "Spicy"
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .header("X-Idempotency-Key", "key-123")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400OnValidationFailure_InvalidQuantity() throws Exception {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(UUID.randomUUID(), 0)),
                "Spicy"
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .header("X-Idempotency-Key", "key-123")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenIdempotencyKeyHeaderIsMissing() throws Exception {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(UUID.randomUUID(), 2)),
                "Spicy"
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
