package com.streetvendor.vendor;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.service.VendorDashboardService;
import com.streetvendor.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
@Transactional
public class VendorDashboardSecurityTest extends AbstractSecurityTest {

    @MockitoBean
    private VendorService vendorService;

    @MockitoBean
    private VendorDashboardService vendorDashboardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String createToken(String email, Role role) {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, email, passwordEncoder.encode("Password1!"), role, AccountStatus.ACTIVE);
        userRepository.save(user);
        return jwtService.generateAccessToken(userId, email, role.name());
    }

    @Test
    void anonymousCannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/vendors/dashboard/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotAccessDashboard() throws Exception {
        String token = createToken("cust@test.com", Role.CUSTOMER);
        mockMvc.perform(get("/api/vendors/dashboard/metrics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotAccessDashboard() throws Exception {
        String token = createToken("admin@test.com", Role.ADMIN);
        mockMvc.perform(get("/api/vendors/dashboard/metrics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotAccessOrders() throws Exception {
        mockMvc.perform(get("/api/vendors/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotAccessOrders() throws Exception {
        String token = createToken("cust@test.com", Role.CUSTOMER);
        mockMvc.perform(get("/api/vendors/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotAccessDocuments() throws Exception {
        mockMvc.perform(get("/api/vendors/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCannotAccessProfile() throws Exception {
        mockMvc.perform(get("/api/vendors/me/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCannotUpdateProfile() throws Exception {
        mockMvc.perform(put("/api/vendors/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotUpdateProfile() throws Exception {
        String token = createToken("cust@test.com", Role.CUSTOMER);
        mockMvc.perform(put("/api/vendors/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
