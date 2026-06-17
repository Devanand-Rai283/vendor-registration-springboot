package com.streetvendor.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.common.audit.AuditLog;
import com.streetvendor.common.audit.AuditLogRepository;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.enums.PaymentStatus;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
class OrderCancellationIntegrationTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenCustomerA;
    private String tokenCustomerB;
    private String tokenVendor;

    private Customer customerA;
    private Customer customerB;
    private Vendor vendor;

    private Order orderA;
    private Order orderB;

    @BeforeEach
    void setUpTestData() {
        auditLogRepository.deleteAll();
        orderRepository.deleteAll();
        vendorRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Users
        User userCustA = new User(UUID.randomUUID(), "custA@example.com", "pass", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(userCustA);
        tokenCustomerA = jwtService.generateAccessToken(userCustA.getId(), "custA@example.com", "CUSTOMER");

        User userCustB = new User(UUID.randomUUID(), "custB@example.com", "pass", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(userCustB);
        tokenCustomerB = jwtService.generateAccessToken(userCustB.getId(), "custB@example.com", "CUSTOMER");

        User userVendor = new User(UUID.randomUUID(), "vendor@example.com", "pass", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(userVendor);
        tokenVendor = jwtService.generateAccessToken(userVendor.getId(), "vendor@example.com", "VENDOR");

        // 2. Create Profiles
        customerA = new Customer(UUID.randomUUID(), userCustA.getId(), "Customer A", "123", "Addr A", null, null);
        customerRepository.save(customerA);

        customerB = new Customer(UUID.randomUUID(), userCustB.getId(), "Customer B", "456", "Addr B", null, null);
        customerRepository.save(customerB);

        vendor = new Vendor(UUID.randomUUID(), userVendor, "Gourmet Street");
        vendorRepository.save(vendor);

        // 3. Create Orders
        orderA = new Order(UUID.randomUUID(), customerA, vendor, new BigDecimal("150.00"), "key-a");
        orderRepository.save(orderA);

        orderB = new Order(UUID.randomUUID(), customerB, vendor, new BigDecimal("250.00"), "key-b");
        orderRepository.save(orderB);

        orderRepository.flush();
    }

    @Test
    void cancelOrder_shouldSuccessfullyCancelPlacedOrderAndCreateAuditLog() throws Exception {
        mockMvc.perform(put("/api/orders/" + orderA.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenCustomerA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderA.getId().toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(150.00))
                .andExpect(jsonPath("$.cancelledAt").exists());

        // Verify database state
        Order updatedOrder = orderRepository.findById(orderA.getId()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.CANCELLED, updatedOrder.getStatus());

        // Verify Audit Logs in Database
        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());

        AuditLog log = logs.get(0);
        assertEquals(AuditEventType.ORDER_CANCELLED_BY_CUSTOMER, log.getEventType());
        assertEquals(vendor.getId(), log.getVendorId());

        // Verify serialized JSON format in details
        JsonNode jsonNode = objectMapper.readTree(log.getDetails());
        assertEquals(orderA.getId().toString(), jsonNode.get("orderId").asText());
        assertEquals(customerA.getId().toString(), jsonNode.get("customerId").asText());
        assertEquals("PLACED", jsonNode.get("fromStatus").asText());
        assertEquals("CANCELLED", jsonNode.get("toStatus").asText());
    }

    @Test
    void cancelOrder_shouldRejectWhenCustomerDoesNotOwnOrder() throws Exception {
        // Customer B attempts to cancel Customer A's order -> 403 Forbidden
        mockMvc.perform(put("/api/orders/" + orderA.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenCustomerB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Verify state is unmodified
        Order unmodifiedOrder = orderRepository.findById(orderA.getId()).orElse(null);
        assertNotNull(unmodifiedOrder);
        assertEquals(OrderStatus.PLACED, unmodifiedOrder.getStatus());

        // Verify no audit log is created
        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(0, logs.size());
    }

    @Test
    void cancelOrder_shouldRejectWhenOrderStatusIsNotPlaced() throws Exception {
        // Update order status to ACCEPTED
        orderA.setStatus(OrderStatus.ACCEPTED);
        orderRepository.saveAndFlush(orderA);

        // Attempt to cancel -> 400 Bad Request
        mockMvc.perform(put("/api/orders/" + orderA.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenCustomerA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot cancel order in status: ACCEPTED"));

        // State remains unmodified
        Order unmodifiedOrder = orderRepository.findById(orderA.getId()).orElse(null);
        assertNotNull(unmodifiedOrder);
        assertEquals(OrderStatus.ACCEPTED, unmodifiedOrder.getStatus());
    }

    @Test
    void cancelOrder_shouldEnforceDoubleCancellationProtection() throws Exception {
        // Call 1 - Cancel PLACED order -> 200 OK
        mockMvc.perform(put("/api/orders/" + orderA.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenCustomerA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Call 2 - Repeat cancellation -> 400 Bad Request
        mockMvc.perform(put("/api/orders/" + orderA.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenCustomerA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot cancel order in status: CANCELLED"));

        // State is CANCELLED
        Order updatedOrder = orderRepository.findById(orderA.getId()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.CANCELLED, updatedOrder.getStatus());

        // Verify only 1 audit log exists in the database (failed attempt doesn't write audit log)
        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());
    }

    @Test
    void cancelOrder_shouldEnforceSecurityAccessRestrictions() throws Exception {
        // Anonymous -> 401
        mockMvc.perform(put("/api/orders/" + orderA.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // Vendor -> 403
        mockMvc.perform(put("/api/orders/" + orderA.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenVendor)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
