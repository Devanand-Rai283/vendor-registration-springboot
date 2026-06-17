package com.streetvendor.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.order.entity.Order;
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
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
class OrderHistoryIntegrationTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenCustomerA;
    private String tokenCustomerB;
    private String tokenVendor;
    private String tokenAdmin;

    private Customer customerA;
    private Customer customerB;
    private Vendor vendor;

    private UUID orderIdA1;
    private UUID orderIdA2;
    private UUID orderIdB1;

    @BeforeEach
    void setUpTestData() throws Exception {
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

        User userAdmin = new User(UUID.randomUUID(), "admin@example.com", "pass", Role.ADMIN, AccountStatus.ACTIVE);
        userRepository.save(userAdmin);
        tokenAdmin = jwtService.generateAccessToken(userAdmin.getId(), "admin@example.com", "ADMIN");

        // 2. Create Profiles
        customerA = new Customer(UUID.randomUUID(), userCustA.getId(), "Customer A", "123", "Addr A", null, null);
        customerRepository.save(customerA);

        customerB = new Customer(UUID.randomUUID(), userCustB.getId(), "Customer B", "456", "Addr B", null, null);
        customerRepository.save(customerB);

        vendor = new Vendor(UUID.randomUUID(), userVendor, "Gourmet Street");
        vendorRepository.save(vendor);

        // 3. Create Orders
        // Order A1 (earlier)
        Order orderA1 = new Order(UUID.randomUUID(), customerA, vendor, new BigDecimal("100.00"), "key-a1");
        orderRepository.save(orderA1);
        orderRepository.flush();
        orderIdA1 = orderA1.getId();

        // Pause briefly to ensure distinct timestamps
        Thread.sleep(50);

        // Order A2 (later)
        Order orderA2 = new Order(UUID.randomUUID(), customerA, vendor, new BigDecimal("200.00"), "key-a2");
        orderRepository.save(orderA2);
        orderRepository.flush();
        orderIdA2 = orderA2.getId();

        // Order B1
        Order orderB1 = new Order(UUID.randomUUID(), customerB, vendor, new BigDecimal("300.00"), "key-b1");
        orderRepository.save(orderB1);
        orderRepository.flush();
        orderIdB1 = orderB1.getId();
    }

    @Test
    void getOrderHistory_shouldEnforceRoleRestrictions() throws Exception {
        // 1. Anonymous access -> 401
        mockMvc.perform(get("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // 2. Vendor access -> 403
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + tokenVendor)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 3. Admin access -> 403
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrderHistory_shouldEnforceOwnershipAndOnlyShowOwnOrders() throws Exception {
        // Customer A queries
        MvcResult result = mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + tokenCustomerA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode content = root.get("content");

        // Should return exactly 2 orders (orderIdA2 and orderIdA1), and NOT orderIdB1
        assertEquals(2, content.size());
        
        // Assert correct fields and values
        assertEquals(orderIdA2.toString(), content.get(0).get("orderId").asText());
        assertEquals(orderIdA1.toString(), content.get(1).get("orderId").asText());
        
        // Assert vendor business name is correct
        assertEquals("Gourmet Street", content.get(0).get("vendorBusinessName").asText());
        assertEquals(vendor.getId().toString(), content.get(0).get("vendorId").asText());
    }

    @Test
    void getOrderHistory_shouldSortByCreatedAtDesc() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + tokenCustomerA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode content = root.get("content");

        // The first element in the page content must be the most recently created order (A2)
        assertEquals(orderIdA2.toString(), content.get(0).get("orderId").asText());
        assertEquals(orderIdA1.toString(), content.get(1).get("orderId").asText());
    }

    @Test
    void getOrderHistory_shouldSupportPagination() throws Exception {
        // Request page index 0, page size 1
        MvcResult result = mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + tokenCustomerA)
                        .param("page", "0")
                        .param("size", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(orderIdA2.toString(), root.get("content").get(0).get("orderId").asText());

        // Request page index 1, page size 1
        result = mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + tokenCustomerA)
                        .param("page", "1")
                        .param("size", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andReturn();

        root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(orderIdA1.toString(), root.get("content").get(0).get("orderId").asText());
    }

    @Test
    void getOrderHistory_shouldRejectInvalidPaginationInputs() throws Exception {
        // 1. Negative page -> 400
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + tokenCustomerA)
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 2. Zero size -> 400
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + tokenCustomerA)
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 3. Exceeded size limit -> 400
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + tokenCustomerA)
                        .param("size", "101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
