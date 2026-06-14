package com.streetvendor.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.common.audit.AuditLog;
import com.streetvendor.common.audit.AuditLogRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.dto.RejectVendorRequest;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("vendor-test")
@Transactional
class VendorApprovalIntegrationTest extends AbstractSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User vendorUser;
    private User adminUser;
    private Vendor pendingVendor;

    @BeforeEach
    void setUp() {
        vendorUser = new User(UUID.randomUUID(), "vendor@test.com", passwordEncoder.encode("Password1!"), Role.VENDOR, AccountStatus.ACTIVE);
        adminUser = new User(UUID.randomUUID(), "admin@test.com", passwordEncoder.encode("Password1!"), Role.ADMIN, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);
        userRepository.save(adminUser);

        pendingVendor = new Vendor(UUID.randomUUID(), vendorUser, "Test Business");
        pendingVendor.setOwnerName("Owner");
        pendingVendor.setPhone("1234567890");
        pendingVendor.setFoodType("Indian");
        pendingVendor.setDescription("Delicious food");
        pendingVendor.setLatitude(new BigDecimal("12.9716"));
        pendingVendor.setLongitude(new BigDecimal("77.5946"));
        pendingVendor.setAddress("123 Main St");
        pendingVendor.setStatus(VendorStatus.PENDING_REVIEW);
        vendorRepository.save(pendingVendor);
    }

    private String vendorToken() {
        return jwtService.generateAccessToken(vendorUser.getId(), vendorUser.getEmail(), vendorUser.getRole().name());
    }

    private String adminToken() {
        return jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());
    }

    @Test
    void shouldApproveVendorEndToEnd() throws Exception {
        UUID vendorId = pendingVendor.getId();

        mockMvc.perform(post("/api/admin/vendors/{id}/approve", vendorId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Vendor approved successfully."))
                .andExpect(jsonPath("$.data.vendorId").value(vendorId.toString()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        Vendor storedVendor = vendorRepository.findById(vendorId).orElseThrow();
        assertEquals(VendorStatus.APPROVED, storedVendor.getStatus());

        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertEquals(1, auditLogs.size());
        assertEquals(AuditEventType.VENDOR_APPROVED, auditLogs.get(0).getEventType());
        assertEquals(vendorId, auditLogs.get(0).getVendorId());

        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + vendorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void shouldRejectVendorEndToEnd() throws Exception {
        UUID vendorId = pendingVendor.getId();
        String reason = "Expired FSSAI certificate";

        RejectVendorRequest rejectRequest = new RejectVendorRequest(reason);

        mockMvc.perform(post("/api/admin/vendors/{id}/reject", vendorId)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Vendor rejected successfully."))
                .andExpect(jsonPath("$.data.vendorId").value(vendorId.toString()))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value(reason));

        Vendor storedVendor = vendorRepository.findById(vendorId).orElseThrow();
        assertEquals(VendorStatus.REJECTED, storedVendor.getStatus());
        assertEquals(reason, storedVendor.getRejectionReason());

        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertEquals(1, auditLogs.size());
        assertEquals(AuditEventType.VENDOR_REJECTED, auditLogs.get(0).getEventType());
        assertEquals(vendorId, auditLogs.get(0).getVendorId());
        assertEquals(reason, auditLogs.get(0).getDetails());

        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + vendorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void shouldReflectLatestStatusOnGetMe() throws Exception {
        UUID vendorId = pendingVendor.getId();

        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + vendorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.businessName").value("Test Business"))
                .andExpect(jsonPath("$.data.averageRating").isNumber());

        mockMvc.perform(post("/api/admin/vendors/{id}/approve", vendorId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + vendorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void shouldPersistApprovalAtomically() throws Exception {
        UUID vendorId = pendingVendor.getId();

        assertEquals("PENDING_REVIEW", vendorRepository.findById(vendorId).orElseThrow().getStatus().name());
        assertEquals(0, auditLogRepository.count());

        mockMvc.perform(post("/api/admin/vendors/{id}/approve", vendorId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());

        assertEquals("APPROVED", vendorRepository.findById(vendorId).orElseThrow().getStatus().name());
        assertEquals(1, auditLogRepository.count());
    }

    @Test
    void shouldPersistRejectionAtomically() throws Exception {
        UUID vendorId = pendingVendor.getId();
        String reason = "Incomplete documentation";

        assertEquals("PENDING_REVIEW", vendorRepository.findById(vendorId).orElseThrow().getStatus().name());
        assertEquals(0, auditLogRepository.count());

        RejectVendorRequest request = new RejectVendorRequest(reason);
        mockMvc.perform(post("/api/admin/vendors/{id}/reject", vendorId)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Vendor stored = vendorRepository.findById(vendorId).orElseThrow();
        assertEquals("REJECTED", stored.getStatus().name());
        assertEquals(reason, stored.getRejectionReason());
        assertEquals(1, auditLogRepository.count());
    }

    @Test
    void shouldNotAllowCustomerToApprove() throws Exception {
        User customerUser = new User(UUID.randomUUID(), "customer@test.com", passwordEncoder.encode("Password1!"), Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);
        String customerToken = jwtService.generateAccessToken(customerUser.getId(), customerUser.getEmail(), customerUser.getRole().name());

        mockMvc.perform(post("/api/admin/vendors/{id}/approve", pendingVendor.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotAllowVendorToApprove() throws Exception {
        mockMvc.perform(post("/api/admin/vendors/{id}/approve", pendingVendor.getId())
                        .header("Authorization", "Bearer " + vendorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotAllowAnonymousToApprove() throws Exception {
        mockMvc.perform(post("/api/admin/vendors/{id}/approve", pendingVendor.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotAllowCustomerToReject() throws Exception {
        User customerUser = new User(UUID.randomUUID(), "customer@test.com", passwordEncoder.encode("Password1!"), Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);
        String customerToken = jwtService.generateAccessToken(customerUser.getId(), customerUser.getEmail(), customerUser.getRole().name());
        RejectVendorRequest request = new RejectVendorRequest("Some reason");

        mockMvc.perform(post("/api/admin/vendors/{id}/reject", pendingVendor.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotAllowVendorToReject() throws Exception {
        RejectVendorRequest request = new RejectVendorRequest("Some reason");

        mockMvc.perform(post("/api/admin/vendors/{id}/reject", pendingVendor.getId())
                        .header("Authorization", "Bearer " + vendorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotAllowAnonymousToReject() throws Exception {
        RejectVendorRequest request = new RejectVendorRequest("Some reason");

        mockMvc.perform(post("/api/admin/vendors/{id}/reject", pendingVendor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotAllowAnonymousToAccessGetMe() throws Exception {
        mockMvc.perform(get("/api/vendors/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotAllowCustomerToAccessGetMe() throws Exception {
        User customerUser = new User(UUID.randomUUID(), "customer@test.com", passwordEncoder.encode("Password1!"), Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);
        String customerToken = jwtService.generateAccessToken(customerUser.getId(), customerUser.getEmail(), customerUser.getRole().name());

        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotAllowAdminToAccessGetMe() throws Exception {
        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnOwnProfileForVendor() throws Exception {
        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + vendorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(pendingVendor.getId().toString()))
                .andExpect(jsonPath("$.data.businessName").value("Test Business"))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.averageRating").isNumber());
    }
}