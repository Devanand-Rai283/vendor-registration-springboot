package com.streetvendor.vendor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.dto.CreateVendorRequest;
import com.streetvendor.vendor.dto.VendorResponse;
import com.streetvendor.vendor.service.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
class VendorSecurityTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private VendorService vendorService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User vendorUser;
    private User customerUser;
    private User adminUser;
    private CreateVendorRequest validRequest;

    @BeforeEach
    void setUpTestData() {
        userRepository.deleteAll();
        vendorUser = new User(UUID.randomUUID(), "vendor@example.com", passwordEncoder.encode("Password1!"), Role.VENDOR, AccountStatus.ACTIVE);
        customerUser = new User(UUID.randomUUID(), "customer@example.com", passwordEncoder.encode("Password1!"), Role.CUSTOMER, AccountStatus.ACTIVE);
        adminUser = new User(UUID.randomUUID(), "admin@example.com", passwordEncoder.encode("Password1!"), Role.ADMIN, AccountStatus.ACTIVE);

        userRepository.save(vendorUser);
        userRepository.save(customerUser);
        userRepository.save(adminUser);

        validRequest = new CreateVendorRequest(
                "Test Business",
                "Owner",
                "1234567890",
                "Indian",
                "Delicious food",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                "123 Main St"
        );
    }

    private String generateToken(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenCustomerTriesToCreateVendor() throws Exception {
        String token = generateToken(customerUser);

        mockMvc.perform(post("/api/vendors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenAdminTriesToCreateVendor() throws Exception {
        String token = generateToken(adminUser);

        mockMvc.perform(post("/api/vendors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn201WhenVendorCreatesProfile() throws Exception {
        String token = generateToken(vendorUser);
        VendorResponse response = new VendorResponse(UUID.randomUUID(), com.streetvendor.vendor.enums.VendorStatus.PENDING_REVIEW, "Vendor profile created successfully.");
        when(vendorService.createVendor(any(CreateVendorRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/vendors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
    }
}