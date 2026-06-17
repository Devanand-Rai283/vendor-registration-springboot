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
import com.streetvendor.order.dto.UpdateOrderStatusRequest;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
class OrderProcessingIntegrationTest extends AbstractSecurityTest {

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
    private String vendorTokenA;
    private String vendorTokenB;
    private Vendor vendorA;
    private Vendor vendorB;
    private Order testOrder;
    private UUID vendorUserAId;

    @BeforeEach
    void setUpTestData() {
        auditLogRepository.deleteAll();
        orderRepository.deleteAll();
        vendorRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Vendor A
        vendorUserAId = UUID.randomUUID();
        User userA = new User(vendorUserAId, "vendorA@example.com", "pass", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(userA);
        vendorA = new Vendor(UUID.randomUUID(), userA, "Vendor A Business");
        vendorRepository.save(vendorA);
        vendorTokenA = jwtService.generateAccessToken(userA.getId(), "vendorA@example.com", "VENDOR");

        // 2. Create Vendor B
        UUID vendorUserBId = UUID.randomUUID();
        User userB = new User(vendorUserBId, "vendorB@example.com", "pass", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(userB);
        vendorB = new Vendor(UUID.randomUUID(), userB, "Vendor B Business");
        vendorRepository.save(vendorB);
        vendorTokenB = jwtService.generateAccessToken(userB.getId(), "vendorB@example.com", "VENDOR");

        // 3. Create Customer
        User customerUser = new User(UUID.randomUUID(), "cust@example.com", "pass", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);
        Customer testCustomer = new Customer(UUID.randomUUID(), customerUser.getId(), "Cust Name", "123", "Addr", null, null);
        customerRepository.save(testCustomer);

        // 4. Create Order belonging to Vendor A
        testOrder = new Order(UUID.randomUUID(), testCustomer, vendorA, new BigDecimal("250.00"), "idemp-1");
        orderRepository.save(testOrder);
        orderRepository.flush();
    }

    @Test
    void shouldSuccessfullyTransitionOrderStatusAndCreateAuditLog() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.ACCEPTED);

        mockMvc.perform(put("/api/orders/" + testOrder.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendorTokenA)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify database state
        Order updatedOrder = orderRepository.findById(testOrder.getId()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.ACCEPTED, updatedOrder.getStatus());
        assertEquals(PaymentStatus.PENDING, updatedOrder.getPaymentStatus());

        // Verify Audit Logs
        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());

        AuditLog log = logs.get(0);
        assertEquals(AuditEventType.ORDER_ACCEPTED, log.getEventType());
        assertEquals(vendorA.getId(), log.getVendorId());
        assertEquals(vendorUserAId, log.getAdminUserId());

        // Verify JSON Audit details structure
        JsonNode jsonNode = objectMapper.readTree(log.getDetails());
        assertEquals(testOrder.getId().toString(), jsonNode.get("orderId").asText());
        assertEquals(vendorA.getId().toString(), jsonNode.get("vendorId").asText());
        assertEquals("PLACED", jsonNode.get("fromStatus").asText());
        assertEquals("ACCEPTED", jsonNode.get("toStatus").asText());
    }

    @Test
    void shouldRejectTransitionWhenVendorDoesNotOwnOrder() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.ACCEPTED);

        // Vendor B attempts to update Vendor A's order -> 403 Forbidden
        mockMvc.perform(put("/api/orders/" + testOrder.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + vendorTokenB)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify state is unmodified
        Order unmodifiedOrder = orderRepository.findById(testOrder.getId()).orElse(null);
        assertNotNull(unmodifiedOrder);
        assertEquals(OrderStatus.PLACED, unmodifiedOrder.getStatus());
    }
}
