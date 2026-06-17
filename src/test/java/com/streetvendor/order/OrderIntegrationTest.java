package com.streetvendor.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.order.dto.OrderItemRequest;
import com.streetvendor.order.dto.PlaceOrderRequest;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.enums.PaymentStatus;
import com.streetvendor.order.repository.OrderItemRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
class OrderIntegrationTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String customerTokenA;
    private String customerTokenB;
    private Customer customerA;
    private Customer customerB;
    private MenuItem testMenuItem1;

    @BeforeEach
    void setUpTestData() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        menuItemRepository.deleteAll();
        menuCategoryRepository.deleteAll();
        vendorRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Customer A
        User userA = new User(UUID.randomUUID(), "custA@example.com", "pass", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(userA);
        customerA = new Customer(UUID.randomUUID(), userA.getId(), "Cust A", "123", "Addr", null, null);
        customerRepository.save(customerA);
        customerTokenA = jwtService.generateAccessToken(userA.getId(), "custA@example.com", "CUSTOMER");

        // 2. Create Customer B
        User userB = new User(UUID.randomUUID(), "custB@example.com", "pass", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(userB);
        customerB = new Customer(UUID.randomUUID(), userB.getId(), "Cust B", "456", "Addr", null, null);
        customerRepository.save(customerB);
        customerTokenB = jwtService.generateAccessToken(userB.getId(), "custB@example.com", "CUSTOMER");

        // 3. Create Vendor
        User vendorUser = new User(UUID.randomUUID(), "vend@example.com", "pass", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);
        Vendor testVendor = new Vendor(UUID.randomUUID(), vendorUser, "Vend Business");
        vendorRepository.save(testVendor);

        // 4. Create MenuItem
        MenuCategory category = new MenuCategory(UUID.randomUUID(), testVendor, "Mains", 1);
        menuCategoryRepository.save(category);
        testMenuItem1 = new MenuItem(UUID.randomUUID(), category, testVendor, "Rice", new BigDecimal("120.00"));
        testMenuItem1.setAvailable(true);
        menuItemRepository.save(testMenuItem1);
    }

    @Test
    void shouldSuccessfullyPlaceNewOrderAndHandleIdempotency() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        OrderItemRequest item = new OrderItemRequest(testMenuItem1.getId(), 2);
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(item), "Keep it hot");

        // 1. Customer A places a new order
        MvcResult firstResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerTokenA)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andReturn();

        String responseBody = firstResult.getResponse().getContentAsString();
        UUID orderIdA = UUID.fromString(objectMapper.readTree(responseBody).get("orderId").asText());

        // Verify database state for Customer A's order (Status and PaymentStatus)
        Order order = orderRepository.findById(orderIdA).orElse(null);
        assertNotNull(order);
        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(new BigDecimal("240.00"), order.getTotalAmount());

        // Prove ownership assignment: order references customerA, not customerB
        assertEquals(customerA.getId(), order.getCustomer().getId());
        assertNotEquals(customerB.getId(), order.getCustomer().getId());

        // 2. Customer A makes duplicate request (200 OK, returning same orderIdA)
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerTokenA)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderIdA.toString()))
                .andExpect(jsonPath("$.status").value("PLACED"));
    }

    @Test
    void shouldAllowDifferentCustomersToReuseSameIdempotencyKey() throws Exception {
        String sharedIdempotencyKey = "shared-key-123";
        OrderItemRequest item = new OrderItemRequest(testMenuItem1.getId(), 2);
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(item), null);

        // 1. Customer A places order with shared key (returns 201)
        MvcResult resultA = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerTokenA)
                        .header("X-Idempotency-Key", sharedIdempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID orderIdA = UUID.fromString(objectMapper.readTree(resultA.getResponse().getContentAsString()).get("orderId").asText());

        // 2. Customer B places order with same shared key (returns 201, NOT duplicate hit or conflict!)
        MvcResult resultB = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerTokenB)
                        .header("X-Idempotency-Key", sharedIdempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID orderIdB = UUID.fromString(objectMapper.readTree(resultB.getResponse().getContentAsString()).get("orderId").asText());

        // Verify two distinct orders were created in database
        assertNotEquals(orderIdA, orderIdB);

        Order orderA = orderRepository.findById(orderIdA).orElse(null);
        Order orderB = orderRepository.findById(orderIdB).orElse(null);

        assertNotNull(orderA);
        assertNotNull(orderB);

        // Verify distinct customer ownerships
        assertEquals(customerA.getId(), orderA.getCustomer().getId());
        assertEquals(customerB.getId(), orderB.getCustomer().getId());

        // Verify status and paymentStatus are correct for both
        assertEquals(OrderStatus.PLACED, orderA.getStatus());
        assertEquals(PaymentStatus.PENDING, orderA.getPaymentStatus());
        assertEquals(OrderStatus.PLACED, orderB.getStatus());
        assertEquals(PaymentStatus.PENDING, orderB.getPaymentStatus());
    }
}
