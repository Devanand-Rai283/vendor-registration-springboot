package com.streetvendor.vendor;

import com.streetvendor.admin.dto.AdminDashboardResponseDto;
import com.streetvendor.admin.service.AdminDashboardService;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller security tests for {@code GET /api/admin/dashboard}.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>ADMIN receives 200 OK with the dashboard data</li>
 *   <li>CUSTOMER receives 403 Forbidden</li>
 *   <li>VENDOR receives 403 Forbidden</li>
 *   <li>Anonymous (unauthenticated) receives 401 Unauthorized</li>
 * </ul>
 */
@ActiveProfiles("security-test")
@DisplayName("AdminDashboardController Security Tests")
class AdminDashboardControllerTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    private User adminUser;
    private User vendorUser;
    private User customerUser;

    @BeforeEach
    void setUpTestData() {
        userRepository.deleteAll();
        adminUser = new User(UUID.randomUUID(), "admin@dashboard.com",
                passwordEncoder.encode("Password1!"), Role.ADMIN, AccountStatus.ACTIVE);
        vendorUser = new User(UUID.randomUUID(), "vendor@dashboard.com",
                passwordEncoder.encode("Password1!"), Role.VENDOR, AccountStatus.ACTIVE);
        customerUser = new User(UUID.randomUUID(), "customer@dashboard.com",
                passwordEncoder.encode("Password1!"), Role.CUSTOMER, AccountStatus.ACTIVE);

        userRepository.save(adminUser);
        userRepository.save(vendorUser);
        userRepository.save(customerUser);
    }

    private String token(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    @Test
    @DisplayName("ADMIN should receive 200 OK with dashboard data")
    void shouldReturn200ForAdmin() throws Exception {
        AdminDashboardResponseDto dto = new AdminDashboardResponseDto(120L, 14L, 850L, 42L);
        when(adminDashboardService.getDashboardMetrics()).thenReturn(dto);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalVendors").value(120))
                .andExpect(jsonPath("$.data.pendingApprovals").value(14))
                .andExpect(jsonPath("$.data.totalUsers").value(850))
                .andExpect(jsonPath("$.data.totalOrdersToday").value(42));
    }

    @Test
    @DisplayName("CUSTOMER should receive 403 Forbidden")
    void shouldReturn403ForCustomer() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token(customerUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("VENDOR should receive 403 Forbidden")
    void shouldReturn403ForVendor() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token(vendorUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request should receive 401 Unauthorized")
    void shouldReturn401ForAnonymous() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }
}
