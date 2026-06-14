package com.streetvendor.vendor;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.dto.VendorStatusResponse;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.service.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
class VendorMeSecurityTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private VendorService vendorService;

    private User vendorUser;
    private User customerUser;
    private User adminUser;

    @BeforeEach
    void setUpTestData() {
        userRepository.deleteAll();
        vendorUser = new User(UUID.randomUUID(), "vendor@example.com", passwordEncoder.encode("Password1!"), Role.VENDOR, AccountStatus.ACTIVE);
        customerUser = new User(UUID.randomUUID(), "customer@example.com", passwordEncoder.encode("Password1!"), Role.CUSTOMER, AccountStatus.ACTIVE);
        adminUser = new User(UUID.randomUUID(), "admin@example.com", passwordEncoder.encode("Password1!"), Role.ADMIN, AccountStatus.ACTIVE);

        userRepository.save(vendorUser);
        userRepository.save(customerUser);
        userRepository.save(adminUser);
    }

    private String generateToken(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/vendors/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenCustomerTriesToAccess() throws Exception {
        String token = generateToken(customerUser);

        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenAdminTriesToAccess() throws Exception {
        String token = generateToken(adminUser);

        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200WhenVendorAccessesOwnProfile() throws Exception {
        String token = generateToken(vendorUser);
        VendorStatusResponse response = new VendorStatusResponse(
                UUID.randomUUID(), "Test Business", VendorStatus.PENDING_REVIEW, BigDecimal.ZERO);
        when(vendorService.getMyVendorStatus()).thenReturn(response);

        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
