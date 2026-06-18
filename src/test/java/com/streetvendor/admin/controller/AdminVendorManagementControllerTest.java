package com.streetvendor.admin.controller;

import com.streetvendor.admin.dto.AdminVendorSummaryDto;
import com.streetvendor.admin.service.AdminVendorManagementService;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.enums.VendorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@DisplayName("AdminVendorManagementController Security and Web Tests")
class AdminVendorManagementControllerTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AdminVendorManagementService adminVendorManagementService;

    private User adminUser;
    private User vendorUser;
    private User customerUser;
    private UUID testVendorId;

    @BeforeEach
    void setUpTestData() {
        userRepository.deleteAll();
        adminUser = new User(UUID.randomUUID(), "admin@mgt.com",
                passwordEncoder.encode("Password1!"), Role.ADMIN, AccountStatus.ACTIVE);
        vendorUser = new User(UUID.randomUUID(), "vendor@mgt.com",
                passwordEncoder.encode("Password1!"), Role.VENDOR, AccountStatus.ACTIVE);
        customerUser = new User(UUID.randomUUID(), "customer@mgt.com",
                passwordEncoder.encode("Password1!"), Role.CUSTOMER, AccountStatus.ACTIVE);

        userRepository.save(adminUser);
        userRepository.save(vendorUser);
        userRepository.save(customerUser);

        testVendorId = UUID.randomUUID();
    }

    private String token(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    // =========================================================================
    // Endpoint: GET /api/admin/vendors (Security & Functionality)
    // =========================================================================

    @Test
    @DisplayName("GET /api/admin/vendors - ADMIN should receive 200 OK and list of vendors")
    void getVendors_admin_returnsList() throws Exception {
        AdminVendorSummaryDto summary = new AdminVendorSummaryDto(
                testVendorId, "Business A", "Owner A", VendorStatus.APPROVED, "vendor@mgt.com", AccountStatus.ACTIVE
        );
        when(adminVendorManagementService.getVendors(eq(VendorStatus.APPROVED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/vendors")
                        .header("Authorization", "Bearer " + token(adminUser))
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].businessName").value("Business A"))
                .andExpect(jsonPath("$.data.content[0].status").value("APPROVED"));
    }

    @Test
    @DisplayName("GET /api/admin/vendors - VENDOR should receive 403 Forbidden")
    void getVendors_vendor_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/vendors")
                        .header("Authorization", "Bearer " + token(vendorUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/vendors - CUSTOMER should receive 403 Forbidden")
    void getVendors_customer_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/vendors")
                        .header("Authorization", "Bearer " + token(customerUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/vendors - Anonymous should receive 401 Unauthorized")
    void getVendors_anonymous_unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/vendors"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Endpoint: POST /api/admin/vendors/{id}/suspend (Security & Functionality)
    // =========================================================================

    @Test
    @DisplayName("POST /api/admin/vendors/{id}/suspend - ADMIN should receive 200 OK")
    void suspendVendor_admin_success() throws Exception {
        mockMvc.perform(post("/api/admin/vendors/{id}/suspend", testVendorId)
                        .header("Authorization", "Bearer " + token(adminUser))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Vendor account suspended successfully."));

        verify(adminVendorManagementService).suspendVendor(eq(testVendorId), eq(adminUser.getId()));
    }

    @Test
    @DisplayName("POST /api/admin/vendors/{id}/suspend - VENDOR should receive 403 Forbidden")
    void suspendVendor_vendor_forbidden() throws Exception {
        mockMvc.perform(post("/api/admin/vendors/{id}/suspend", testVendorId)
                        .header("Authorization", "Bearer " + token(vendorUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/vendors/{id}/suspend - CUSTOMER should receive 403 Forbidden")
    void suspendVendor_customer_forbidden() throws Exception {
        mockMvc.perform(post("/api/admin/vendors/{id}/suspend", testVendorId)
                        .header("Authorization", "Bearer " + token(customerUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/vendors/{id}/suspend - Anonymous should receive 401 Unauthorized")
    void suspendVendor_anonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/vendors/{id}/suspend", testVendorId))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Endpoint: POST /api/admin/vendors/{id}/reactivate (Security & Functionality)
    // =========================================================================

    @Test
    @DisplayName("POST /api/admin/vendors/{id}/reactivate - ADMIN should receive 200 OK")
    void reactivateVendor_admin_success() throws Exception {
        mockMvc.perform(post("/api/admin/vendors/{id}/reactivate", testVendorId)
                        .header("Authorization", "Bearer " + token(adminUser))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Vendor account reactivated successfully."));

        verify(adminVendorManagementService).reactivateVendor(eq(testVendorId), eq(adminUser.getId()));
    }

    @Test
    @DisplayName("POST /api/admin/vendors/{id}/reactivate - VENDOR should receive 403 Forbidden")
    void reactivateVendor_vendor_forbidden() throws Exception {
        mockMvc.perform(post("/api/admin/vendors/{id}/reactivate", testVendorId)
                        .header("Authorization", "Bearer " + token(vendorUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/vendors/{id}/reactivate - CUSTOMER should receive 403 Forbidden")
    void reactivateVendor_customer_forbidden() throws Exception {
        mockMvc.perform(post("/api/admin/vendors/{id}/reactivate", testVendorId)
                        .header("Authorization", "Bearer " + token(customerUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/vendors/{id}/reactivate - Anonymous should receive 401 Unauthorized")
    void reactivateVendor_anonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/vendors/{id}/reactivate", testVendorId))
                .andExpect(status().isUnauthorized());
    }
}
