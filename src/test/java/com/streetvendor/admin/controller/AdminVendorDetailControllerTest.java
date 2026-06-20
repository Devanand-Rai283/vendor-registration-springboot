package com.streetvendor.admin.controller;

import com.streetvendor.admin.dto.AdminVendorDetailResponseDto;
import com.streetvendor.admin.service.AdminVendorManagementService;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.enums.VendorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@DisplayName("AdminVendorDetailController Security and Web Tests")
class AdminVendorDetailControllerTest extends AbstractSecurityTest {

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

        @Test
        @DisplayName("GET /api/admin/vendors/{id} - ADMIN should receive 200 OK and vendor details")
        void getVendorDetails_admin_returnsDetails() throws Exception {
                AdminVendorDetailResponseDto details = new AdminVendorDetailResponseDto(
                                testVendorId, "Business A", "Owner A", "vendor@mgt.com", "1234567890",
                                "Great food", "Mexican", VendorStatus.PENDING_REVIEW, AccountStatus.ACTIVE,
                                "123 Main St", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0,
                                Instant.now(), Instant.now(), null, List.of());

                when(adminVendorManagementService.getVendorDetails(eq(testVendorId))).thenReturn(details);

                mockMvc.perform(get("/api/admin/vendors/{id}", testVendorId)
                                .header("Authorization", "Bearer " + token(adminUser)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.businessName").value("Business A"))
                                .andExpect(jsonPath("$.data.email").value("vendor@mgt.com"));
        }

        @Test
        @DisplayName("GET /api/admin/vendors/{id} - ADMIN should receive 404 when vendor not found")
        void getVendorDetails_admin_returns404_whenNotFound() throws Exception {
                when(adminVendorManagementService.getVendorDetails(eq(testVendorId)))
                                .thenThrow(new ResourceNotFoundException("Vendor not found with id: " + testVendorId));

                mockMvc.perform(get("/api/admin/vendors/{id}", testVendorId)
                                .header("Authorization", "Bearer " + token(adminUser)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.message").value("Vendor not found with id: " + testVendorId));
        }

        @Test
        @DisplayName("GET /api/admin/vendors/{id} - VENDOR should receive 403 Forbidden")
        void getVendorDetails_vendor_forbidden() throws Exception {
                mockMvc.perform(get("/api/admin/vendors/{id}", testVendorId)
                                .header("Authorization", "Bearer " + token(vendorUser)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/admin/vendors/{id} - CUSTOMER should receive 403 Forbidden")
        void getVendorDetails_customer_forbidden() throws Exception {
                mockMvc.perform(get("/api/admin/vendors/{id}", testVendorId)
                                .header("Authorization", "Bearer " + token(customerUser)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/admin/vendors/{id} - Anonymous should receive 401 Unauthorized")
        void getVendorDetails_anonymous_unauthorized() throws Exception {
                mockMvc.perform(get("/api/admin/vendors/{id}", testVendorId))
                                .andExpect(status().isUnauthorized());
        }
}
