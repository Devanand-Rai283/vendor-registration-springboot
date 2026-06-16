package com.streetvendor.integration;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
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

@ActiveProfiles("vendor-test")
@Transactional
class DiscoveryVisibilityIntegrationTest extends AbstractSecurityTest {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final double TEST_LAT = 12.9716;
    private static final double TEST_LNG = 77.5946;

    private User vendorUser;
    private User customerUser;
    private User adminUser;

    private String vendorToken;
    private String customerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        vendorUser = createUser("vendor-vis@test.com", Role.VENDOR);
        customerUser = createUser("customer-vis@test.com", Role.CUSTOMER);
        adminUser = createUser("admin-vis@test.com", Role.ADMIN);

        vendorToken = jwtService.generateAccessToken(vendorUser.getId(), vendorUser.getEmail(), vendorUser.getRole().name());
        customerToken = jwtService.generateAccessToken(customerUser.getId(), customerUser.getEmail(), customerUser.getRole().name());
        adminToken = jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());
    }

    private User createUser(String email, Role role) {
        User user = new User(UUID.randomUUID(), email, passwordEncoder.encode("Password1!"), role, AccountStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Vendor createVendor(UUID id, User user, String businessName, VendorStatus status) {
        Vendor vendor = new Vendor(id, user, businessName);
        vendor.setLatitude(BigDecimal.valueOf(TEST_LAT));
        vendor.setLongitude(BigDecimal.valueOf(TEST_LNG));
        vendor.setStatus(status);
        vendor.setFoodType("Test Food");
        vendor.setAddress("123 Test St");
        vendor.setAverageRating(BigDecimal.valueOf(4.0));
        return vendorRepository.save(vendor);
    }

    @Test
    void approvedVendorAppearsInResults() throws Exception {
        createVendor(UUID.randomUUID(), vendorUser, "Approved Vendor", VendorStatus.APPROVED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(TEST_LAT))
                        .param("lng", String.valueOf(TEST_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors[0].businessName").value("Approved Vendor"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void pendingReviewVendorIsAbsent() throws Exception {
        createVendor(UUID.randomUUID(), vendorUser, "Pending Vendor", VendorStatus.PENDING_REVIEW);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(TEST_LAT))
                        .param("lng", String.valueOf(TEST_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectedVendorIsAbsent() throws Exception {
        createVendor(UUID.randomUUID(), vendorUser, "Rejected Vendor", VendorStatus.REJECTED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(TEST_LAT))
                        .param("lng", String.valueOf(TEST_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void onlyApprovedVendorsAreVisible() throws Exception {
        createVendor(UUID.randomUUID(), createUser("approved@test.com", Role.VENDOR), "Approved Business", VendorStatus.APPROVED);
        createVendor(UUID.randomUUID(), createUser("pending@test.com", Role.VENDOR), "Pending Business", VendorStatus.PENDING_REVIEW);
        createVendor(UUID.randomUUID(), createUser("rejected@test.com", Role.VENDOR), "Rejected Business", VendorStatus.REJECTED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(TEST_LAT))
                        .param("lng", String.valueOf(TEST_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.vendors[0].businessName").value("Approved Business"));
    }

    @Test
    void anonymousUserCanViewApprovedVendors() throws Exception {
        createVendor(UUID.randomUUID(), vendorUser, "Public Vendor", VendorStatus.APPROVED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(TEST_LAT))
                        .param("lng", String.valueOf(TEST_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void authenticatedVendorCanViewOtherApprovedVendors() throws Exception {
        User otherVendorUser = createUser("other-vendor@test.com", Role.VENDOR);
        createVendor(UUID.randomUUID(), otherVendorUser, "Other's Vendor", VendorStatus.APPROVED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .header("Authorization", "Bearer " + vendorToken)
                        .param("lat", String.valueOf(TEST_LAT))
                        .param("lng", String.valueOf(TEST_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.vendors[0].businessName").value("Other's Vendor"));
    }

    @Test
    void authenticatedCustomerCanViewApprovedVendors() throws Exception {
        createVendor(UUID.randomUUID(), vendorUser, "Customer Visible", VendorStatus.APPROVED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("lat", String.valueOf(TEST_LAT))
                        .param("lng", String.valueOf(TEST_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void authenticatedAdminCanViewApprovedVendors() throws Exception {
        createVendor(UUID.randomUUID(), vendorUser, "Admin Visible", VendorStatus.APPROVED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("lat", String.valueOf(TEST_LAT))
                        .param("lng", String.valueOf(TEST_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void approvedToRejectedRemovesFromDiscoverability() throws Exception {
        UUID vendorId = UUID.randomUUID();
        createVendor(vendorId, vendorUser, "Status Change Vendor", VendorStatus.APPROVED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(TEST_LAT))
                        .param("lng", String.valueOf(TEST_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        Vendor vendor = vendorRepository.findById(vendorId).orElseThrow();
        vendor.setStatus(VendorStatus.REJECTED);
        vendorRepository.save(vendor);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(TEST_LAT))
                        .param("lng", String.valueOf(TEST_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectedToApprovedRestoresDiscoverability() throws Exception {
        UUID vendorId = UUID.randomUUID();
        createVendor(vendorId, vendorUser, "Restored Vendor", VendorStatus.REJECTED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(TEST_LAT))
                        .param("lng", String.valueOf(TEST_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        Vendor vendor = vendorRepository.findById(vendorId).orElseThrow();
        vendor.setStatus(VendorStatus.APPROVED);
        vendorRepository.save(vendor);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(TEST_LAT))
                        .param("lng", String.valueOf(TEST_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.vendors[0].businessName").value("Restored Vendor"));
    }
}
