package com.streetvendor.integration;

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
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for {@code GET /api/admin/dashboard}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Aggregation correctness (total vendors, pending count, user count, today orders)</li>
 *   <li>Pending vendor calculation only counts PENDING_REVIEW status</li>
 *   <li>Today's order calculation reflects seeded data</li>
 *   <li>Response structure matches the acceptance criteria</li>
 * </ul>
 *
 * <p>Uses the {@code admin-test} profile with an isolated H2 database.
 */
@ActiveProfiles("admin-test")
@Transactional
@DisplayName("AdminDashboard Integration Tests")
class AdminDashboardIntegrationTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        vendorRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = new User(UUID.randomUUID(), "admin@inttest.com",
                passwordEncoder.encode("Password1!"), Role.ADMIN, AccountStatus.ACTIVE);
        userRepository.save(adminUser);
    }

    private String adminToken() {
        return jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());
    }

    private Vendor createVendor(String email, VendorStatus status) {
        User vendorUser = new User(UUID.randomUUID(), email,
                "hashed", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);
        Vendor vendor = new Vendor(UUID.randomUUID(), vendorUser, "Business " + email);
        vendor.setStatus(status);
        return vendorRepository.save(vendor);
    }

    private Customer createCustomer(String email) {
        User customerUser = new User(UUID.randomUUID(), email,
                "hashed", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);
        Customer customer = new Customer(UUID.randomUUID(), customerUser.getId(),
                "Customer Name", "9999999999", "Some Address", null, null);
        return customerRepository.save(customer);
    }

    private Order createOrder(Customer customer, Vendor vendor) {
        Order order = new Order(UUID.randomUUID(), customer, vendor,
                new BigDecimal("100.00"), UUID.randomUUID().toString());
        return orderRepository.save(order);
    }

    // -------------------------------------------------------------------------
    // Response structure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("dashboard response should have correct structure")
    void shouldReturnCorrectResponseStructure() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Dashboard metrics retrieved successfully."))
                .andExpect(jsonPath("$.data.totalVendors").isNumber())
                .andExpect(jsonPath("$.data.pendingApprovals").isNumber())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalOrdersToday").isNumber());
    }

    // -------------------------------------------------------------------------
    // Aggregation correctness
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should return correct total vendor count")
    void shouldReturnCorrectTotalVendorCount() throws Exception {
        createVendor("v1@test.com", VendorStatus.APPROVED);
        createVendor("v2@test.com", VendorStatus.PENDING_REVIEW);
        createVendor("v3@test.com", VendorStatus.REJECTED);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalVendors").value(3));
    }

    @Test
    @DisplayName("should count only PENDING_REVIEW vendors as pending approvals")
    void shouldCountOnlyPendingVendors() throws Exception {
        createVendor("pending1@test.com", VendorStatus.PENDING_REVIEW);
        createVendor("pending2@test.com", VendorStatus.PENDING_REVIEW);
        createVendor("approved@test.com", VendorStatus.APPROVED);
        createVendor("rejected@test.com", VendorStatus.REJECTED);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingApprovals").value(2));
    }

    @Test
    @DisplayName("should return correct total user count including all roles")
    void shouldReturnCorrectTotalUserCount() throws Exception {
        // adminUser is already saved (1 user).
        // Add 2 more users of different roles.
        User u2 = new User(UUID.randomUUID(), "extra1@test.com", "hashed", Role.CUSTOMER, AccountStatus.ACTIVE);
        User u3 = new User(UUID.randomUUID(), "extra2@test.com", "hashed", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(u2);
        userRepository.save(u3);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(3));
    }

    @Test
    @DisplayName("should count orders created today")
    void shouldCountOrdersCreatedToday() throws Exception {
        Vendor vendor = createVendor("ordervendor@test.com", VendorStatus.APPROVED);
        Customer customer = createCustomer("ordercustomer@test.com");

        // Save two orders — both are newly created and fall within today
        createOrder(customer, vendor);
        createOrder(customer, vendor);

        orderRepository.flush();

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrdersToday").value(2));
    }

    @Test
    @DisplayName("should return zeros when platform is empty (only admin exists)")
    void shouldReturnZerosWhenPlatformIsEmpty() throws Exception {
        // Only adminUser exists — no vendors, no non-admin users, no orders
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalVendors").value(0))
                .andExpect(jsonPath("$.data.pendingApprovals").value(0))
                .andExpect(jsonPath("$.data.totalUsers").value(1)) // just the admin
                .andExpect(jsonPath("$.data.totalOrdersToday").value(0));
    }

    // -------------------------------------------------------------------------
    // Regression — existing vendor approval workflow unaffected
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("regression: existing admin vendor approval endpoint is unaffected")
    void regressionExistingAdminVendorEndpointUnaffected() throws Exception {
        // Verify the existing admin vendor endpoint still returns 404 for unknown IDs
        // (not 401/403 — security not degraded)
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/admin/vendors/{id}/approve", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Vendor not found → 404, security still intact (not 401/403 misrouted)
                    org.junit.jupiter.api.Assertions.assertTrue(
                            status == 404 || status == 409,
                            "Existing approve endpoint should still respond to admin (got " + status + ")");
                });
    }
}
