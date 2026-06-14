package com.streetvendor.vendor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.dto.GeneratePresignedUrlRequest;
import com.streetvendor.vendor.dto.GeneratePresignedUrlResponse;
import com.streetvendor.vendor.enums.DocumentType;
import com.streetvendor.vendor.service.DocumentUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("security-test")
class UploadSecurityTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private DocumentUploadService documentUploadService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User vendorUser;
    private User customerUser;
    private User adminUser;
    private GeneratePresignedUrlRequest validRequest;

    @BeforeEach
    void setUpTestData() {
        reset(documentUploadService);

        userRepository.deleteAll();
        vendorUser = new User(UUID.randomUUID(), "vendor@example.com", passwordEncoder.encode("Password1!"), Role.VENDOR, AccountStatus.ACTIVE);
        customerUser = new User(UUID.randomUUID(), "customer@example.com", passwordEncoder.encode("Password1!"), Role.CUSTOMER, AccountStatus.ACTIVE);
        adminUser = new User(UUID.randomUUID(), "admin@example.com", passwordEncoder.encode("Password1!"), Role.ADMIN, AccountStatus.ACTIVE);

        userRepository.save(vendorUser);
        userRepository.save(customerUser);
        userRepository.save(adminUser);

        validRequest = new GeneratePresignedUrlRequest(
                DocumentType.FSSAI_CERTIFICATE,
                "application/pdf",
                1024L
        );
    }

    private String generateToken(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/uploads/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenCustomer() throws Exception {
        String token = generateToken(customerUser);

        mockMvc.perform(post("/api/uploads/presigned-url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenAdmin() throws Exception {
        String token = generateToken(adminUser);

        mockMvc.perform(post("/api/uploads/presigned-url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenVendorWithoutProfile() throws Exception {
        String token = generateToken(vendorUser);
        when(documentUploadService.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenThrow(new ForbiddenException("Vendor profile not found"));

        mockMvc.perform(post("/api/uploads/presigned-url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200WhenValidVendor() throws Exception {
        String token = generateToken(vendorUser);
        GeneratePresignedUrlResponse response = new GeneratePresignedUrlResponse(
                "https://upload.example.com/url",
                "vendors/" + vendorUser.getId() + "/documents/fssai_certificate/uuid"
        );
        when(documentUploadService.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/uploads/presigned-url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }
}
