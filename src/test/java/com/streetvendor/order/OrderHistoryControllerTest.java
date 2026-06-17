package com.streetvendor.order;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.order.dto.CustomerOrderHistoryResponse;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.enums.PaymentStatus;
import com.streetvendor.order.service.OrderHistoryService;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
class OrderHistoryControllerTest extends AbstractSecurityTest {

    @MockitoBean
    private OrderHistoryService orderHistoryService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

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
        mockMvc.perform(get("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenAuthenticatedAsVendor() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200AndPageContentWhenAuthenticatedAsCustomer() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        CustomerOrderHistoryResponse responseDto = new CustomerOrderHistoryResponse(
                orderId,
                vendorId,
                "Aroma Cafe",
                OrderStatus.PLACED,
                PaymentStatus.PENDING,
                new BigDecimal("45.50"),
                Instant.now()
        );

        Page<CustomerOrderHistoryResponse> mockPage = new PageImpl<>(List.of(responseDto));
        when(orderHistoryService.getOrderHistory(eq(customerUserId), eq(0), eq(20)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.content[0].vendorId").value(vendorId.toString()))
                .andExpect(jsonPath("$.content[0].vendorBusinessName").value("Aroma Cafe"))
                .andExpect(jsonPath("$.content[0].status").value("PLACED"))
                .andExpect(jsonPath("$.content[0].paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.content[0].totalAmount").value(45.50))
                .andExpect(jsonPath("$.content[0].createdAt").exists());
    }

    @Test
    void shouldReturn400WhenServiceThrowsIllegalArgumentException() throws Exception {
        when(orderHistoryService.getOrderHistory(eq(customerUserId), eq(-1), anyInt()))
                .thenThrow(new IllegalArgumentException("Page index must not be less than zero"));

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page index must not be less than zero"));
    }
}
