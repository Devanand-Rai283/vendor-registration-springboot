package com.streetvendor.integration.security;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.repository.MenuItemRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
@Transactional
class VendorMenuSecurityIntegrationTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID approvedVendorId;

    @BeforeEach
    void setUp() {
        User vendorUser = userRepository.save(
                new User(UUID.randomUUID(), "vendor-menu-security@test.com",
                        passwordEncoder.encode("Password1!"), Role.VENDOR, AccountStatus.ACTIVE));

        Vendor vendor = new Vendor(UUID.randomUUID(), vendorUser, "Security Test Vendor");
        vendor.setStatus(VendorStatus.APPROVED);
        vendor.setFoodType("Test");
        vendor.setAddress("123 Test St");
        vendor.setLatitude(BigDecimal.valueOf(12.9716));
        vendor.setLongitude(BigDecimal.valueOf(77.5946));
        vendor.setAverageRating(BigDecimal.valueOf(4.0));
        vendor = vendorRepository.save(vendor);
        approvedVendorId = vendor.getId();

        MenuCategory category = menuCategoryRepository.save(
                new MenuCategory(UUID.randomUUID(), vendor, "Drinks", 1));

        menuItemRepository.save(
                new MenuItem(UUID.randomUUID(), category, vendor, "Tea", BigDecimal.valueOf(10.00)));
    }

    @Test
    void anonymousGetMenu_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/vendors/{vendorId}/menu", approvedVendorId))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_shouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(post("/api/vendors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMenuWithInvalidJwt_shouldStillReturnOk() throws Exception {
        mockMvc.perform(get("/api/vendors/{vendorId}/menu", approvedVendorId)
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk());
    }

    @Test
    void nonGetMenuAccess_shouldRemainProtected() throws Exception {
        mockMvc.perform(post("/api/vendors/{vendorId}/menu", approvedVendorId))
                .andExpect(status().isUnauthorized());
    }
}
