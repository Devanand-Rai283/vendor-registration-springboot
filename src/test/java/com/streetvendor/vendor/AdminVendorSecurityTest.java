package com.streetvendor.vendor;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.dto.VendorResponse;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.service.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
class AdminVendorSecurityTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private VendorService vendorService;

    private User adminUser;
    private User vendorUser;
    private User customerUser;

    @BeforeEach
    void setUpTestData() {
        userRepository.deleteAll();
        adminUser = new User(UUID.randomUUID(), "admin@example.com", passwordEncoder.encode("Password1!"), Role.ADMIN, AccountStatus.ACTIVE);
        vendorUser = new User(UUID.randomUUID(), "vendor@example.com", passwordEncoder.encode("Password1!"), Role.VENDOR, AccountStatus.ACTIVE);
        customerUser = new User(UUID.randomUUID(), "customer@example.com", passwordEncoder.encode("Password1!"), Role.CUSTOMER, AccountStatus.ACTIVE);

        userRepository.save(adminUser);
        userRepository.save(vendorUser);
        userRepository.save(customerUser);
    }

    private String generateToken(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/admin/vendors/{id}/approve", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenCustomerTriesToAccess() throws Exception {
        String token = generateToken(customerUser);

        mockMvc.perform(post("/api/admin/vendors/{id}/approve", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenVendorTriesToAccess() throws Exception {
        String token = generateToken(vendorUser);

        mockMvc.perform(post("/api/admin/vendors/{id}/approve", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200WhenAdminApprovesVendor() throws Exception {
        String token = generateToken(adminUser);
        VendorResponse response = new VendorResponse(UUID.randomUUID(), VendorStatus.APPROVED, "Vendor approved successfully.", null);
        when(vendorService.approveVendor(any(UUID.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/vendors/{id}/approve", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
