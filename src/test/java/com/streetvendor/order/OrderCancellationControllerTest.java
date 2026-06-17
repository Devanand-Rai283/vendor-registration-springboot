package com.streetvendor.order;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.order.dto.CancelOrderResponse;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.enums.PaymentStatus;
import com.streetvendor.order.exception.OrderCancellationNotAllowedException;
import com.streetvendor.order.service.OrderCancellationService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
class OrderCancellationControllerTest extends AbstractSecurityTest {

    @MockitoBean
    private OrderCancellationService orderCancellationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private String customerToken;
    private String vendorToken;
    private UUID customerUserId;
    private UUID orderId;

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
        orderId = UUID.randomUUID();
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/orders/" + orderId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenAuthenticatedAsVendor() throws Exception {
        mockMvc.perform(put("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200OnSuccessfulCancellation() throws Exception {
        CancelOrderResponse response = new CancelOrderResponse(
                orderId,
                OrderStatus.CANCELLED,
                PaymentStatus.PENDING,
                new BigDecimal("100.00"),
                Instant.now()
        );

        when(orderCancellationService.cancelOrder(eq(orderId), eq(customerUserId)))
                .thenReturn(response);

        mockMvc.perform(put("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturn400WhenCancellationNotAllowed() throws Exception {
        when(orderCancellationService.cancelOrder(eq(orderId), eq(customerUserId)))
                .thenThrow(new OrderCancellationNotAllowedException("Cannot cancel order in status: ACCEPTED"));

        mockMvc.perform(put("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot cancel order in status: ACCEPTED"));
    }
}
