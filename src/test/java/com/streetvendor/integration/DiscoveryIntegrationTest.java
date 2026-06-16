package com.streetvendor.integration;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
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
class DiscoveryIntegrationTest extends AbstractSecurityTest {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final double CENTER_LAT = 12.9716;
    private static final double CENTER_LNG = 77.5946;

    private User user1;
    private User user2;
    private User user3;
    private User user4;

    @BeforeEach
    void setUp() {
        user1 = createUser("vend1@test.com");
        user2 = createUser("vend2@test.com");
        user3 = createUser("vend3@test.com");
        user4 = createUser("vend4@test.com");
    }

    private User createUser(String email) {
        User user = new User(UUID.randomUUID(), email, passwordEncoder.encode("Password1!"), Role.VENDOR, AccountStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Vendor createVendor(User user, String name, double lat, double lng, VendorStatus status) {
        Vendor vendor = new Vendor(UUID.randomUUID(), user, name);
        vendor.setLatitude(BigDecimal.valueOf(lat));
        vendor.setLongitude(BigDecimal.valueOf(lng));
        vendor.setStatus(status);
        vendor.setFoodType("Test Food");
        vendor.setAddress("123 Test St");
        vendor.setAverageRating(BigDecimal.valueOf(4.0));
        return vendorRepository.save(vendor);
    }

    @Test
    void approvedVendorWithinRadiusAppearsInResults() throws Exception {
        createVendor(user1, "Central Cafe", CENTER_LAT, CENTER_LNG, VendorStatus.APPROVED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(CENTER_LAT))
                        .param("lng", String.valueOf(CENTER_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors[0].businessName").value("Central Cafe"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void vendorOutsideRadiusIsExcluded() throws Exception {
        createVendor(user1, "Far Diner", 13.5000, CENTER_LNG, VendorStatus.APPROVED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(CENTER_LAT))
                        .param("lng", String.valueOf(CENTER_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void multipleVendorsSortedByDistance() throws Exception {
        createVendor(user1, "Far Vendor", CENTER_LAT, CENTER_LNG + 0.01, VendorStatus.APPROVED);
        createVendor(user2, "Near Vendor", CENTER_LAT, CENTER_LNG, VendorStatus.APPROVED);
        createVendor(user3, "Middle Vendor", CENTER_LAT, CENTER_LNG + 0.005, VendorStatus.APPROVED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(CENTER_LAT))
                        .param("lng", String.valueOf(CENTER_LNG))
                        .param("radius", "10.0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors[0].businessName").value("Near Vendor"))
                .andExpect(jsonPath("$.vendors[1].businessName").value("Middle Vendor"))
                .andExpect(jsonPath("$.vendors[2].businessName").value("Far Vendor"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void paginationMetadataIsCorrect() throws Exception {
        createVendor(user1, "V1", CENTER_LAT, CENTER_LNG, VendorStatus.APPROVED);
        createVendor(user2, "V2", CENTER_LAT, CENTER_LNG + 0.001, VendorStatus.APPROVED);
        createVendor(user3, "V3", CENTER_LAT, CENTER_LNG + 0.002, VendorStatus.APPROVED);
        createVendor(user4, "V4", CENTER_LAT, CENTER_LNG + 0.003, VendorStatus.APPROVED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(CENTER_LAT))
                        .param("lng", String.valueOf(CENTER_LNG))
                        .param("radius", "5.0")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.vendors.length()").value(2));
    }

    @Test
    void emptyResultWhenNoApprovedVendors() throws Exception {
        createVendor(user1, "Pending Vendor", CENTER_LAT, CENTER_LNG, VendorStatus.PENDING_REVIEW);
        createVendor(user2, "Rejected Vendor", CENTER_LAT, CENTER_LNG, VendorStatus.REJECTED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(CENTER_LAT))
                        .param("lng", String.valueOf(CENTER_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidLatitudeReturns400() throws Exception {
        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", "91.0")
                        .param("lng", "0.0")
                        .param("radius", "5.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/vendors/nearby"));
    }

    @Test
    void anonymousRequestReturns200() throws Exception {
        createVendor(user1, "Public Vendor", CENTER_LAT, CENTER_LNG, VendorStatus.APPROVED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(CENTER_LAT))
                        .param("lng", String.valueOf(CENTER_LNG)))
                .andExpect(status().isOk());
    }

    @Test
    void dtoSerializationContainsAllFields() throws Exception {
        createVendor(user1, "DTO Test Vendor", CENTER_LAT, CENTER_LNG, VendorStatus.APPROVED);

        mockMvc.perform(get("/api/vendors/nearby")
                        .param("lat", String.valueOf(CENTER_LAT))
                        .param("lng", String.valueOf(CENTER_LNG))
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors[0].id").isString())
                .andExpect(jsonPath("$.vendors[0].businessName").value("DTO Test Vendor"))
                .andExpect(jsonPath("$.vendors[0].foodType").value("Test Food"))
                .andExpect(jsonPath("$.vendors[0].address").value("123 Test St"))
                .andExpect(jsonPath("$.vendors[0].averageRating").isNumber())
                .andExpect(jsonPath("$.vendors[0].latitude").isNumber())
                .andExpect(jsonPath("$.vendors[0].longitude").isNumber())
                .andExpect(jsonPath("$.vendors[0].distanceKm").isNumber());
    }
}
