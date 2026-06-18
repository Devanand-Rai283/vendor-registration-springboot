package com.streetvendor.admin.integration;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.RefreshToken;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.RefreshTokenRepository;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.common.audit.AuditLog;
import com.streetvendor.common.audit.AuditLogRepository;
import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("admin-test")
@Testcontainers
@Transactional
@DisplayName("Admin Vendor Management Integration Tests")
class AdminVendorManagementIntegrationTest extends AbstractSecurityTest {

    @Container
    public static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // Clean redis between runs
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        auditLogRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        vendorRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = new User(UUID.randomUUID(), "admin@inttest.com",
                passwordEncoder.encode("Password1!"), Role.ADMIN, AccountStatus.ACTIVE);
        userRepository.save(adminUser);

        adminToken = jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("GET /api/admin/vendors - returns paginated list sorted by createdAt DESC")
    void shouldReturnPaginatedVendorsSortedByCreatedAtDesc() throws Exception {
        User vUser1 = new User(UUID.randomUUID(), "v1@inttest.com", "hashed", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vUser1);
        Vendor vendor1 = new Vendor(UUID.randomUUID(), vUser1, "Business One");
        vendor1.setStatus(VendorStatus.APPROVED);
        vendorRepository.save(vendor1);

        // Perform a small wait to ensure distinct creation timestamps
        Thread.sleep(50);

        User vUser2 = new User(UUID.randomUUID(), "v2@inttest.com", "hashed", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vUser2);
        Vendor vendor2 = new Vendor(UUID.randomUUID(), vUser2, "Business Two");
        vendor2.setStatus(VendorStatus.APPROVED);
        vendorRepository.save(vendor2);

        mockMvc.perform(get("/api/admin/vendors")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].businessName").value("Business Two")) // Sorted DESC
                .andExpect(jsonPath("$.data.content[1].businessName").value("Business One"));
    }

    @Test
    @DisplayName("GET /api/admin/vendors - filtering by status works correctly")
    void shouldFilterVendorsByStatus() throws Exception {
        User vUser1 = new User(UUID.randomUUID(), "v1@inttest.com", "hashed", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vUser1);
        Vendor vendor1 = new Vendor(UUID.randomUUID(), vUser1, "Pending Vendor");
        vendor1.setStatus(VendorStatus.PENDING_REVIEW);
        vendorRepository.save(vendor1);

        User vUser2 = new User(UUID.randomUUID(), "v2@inttest.com", "hashed", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vUser2);
        Vendor vendor2 = new Vendor(UUID.randomUUID(), vUser2, "Approved Vendor");
        vendor2.setStatus(VendorStatus.APPROVED);
        vendorRepository.save(vendor2);

        mockMvc.perform(get("/api/admin/vendors")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].businessName").value("Approved Vendor"));
    }

    @Test
    @DisplayName("POST /api/admin/vendors/{id}/suspend - suspends user, revokes refresh tokens, sets Redis blacklist with TTL, writes audit event")
    void shouldSuspendVendorAccountAndInvalidateSessions() throws Exception {
        User vUser = new User(UUID.randomUUID(), "vendor@inttest.com", "hashed", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vUser);
        Vendor vendor = new Vendor(UUID.randomUUID(), vUser, "Suspended Business");
        vendor.setStatus(VendorStatus.APPROVED);
        vendorRepository.save(vendor);

        RefreshToken token = new RefreshToken(UUID.randomUUID(), vUser.getId(), "tokenhash", Instant.now().plusSeconds(3600));
        refreshTokenRepository.save(token);

        String vendorAccessToken = jwtService.generateAccessToken(vUser.getId(), vUser.getEmail(), vUser.getRole().name());

        // 1. Initial access is allowed (before suspension)
        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + vendorAccessToken))
                .andExpect(status().isOk());

        // 2. Perform suspension
        mockMvc.perform(post("/api/admin/vendors/{id}/suspend", vendor.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Verify database status
        User updatedUser = userRepository.findById(vUser.getId()).orElseThrow();
        assertThat(updatedUser.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);

        // 4. Verify refresh token is revoked
        RefreshToken updatedToken = refreshTokenRepository.findById(token.getId()).orElseThrow();
        assertThat(updatedToken.isRevoked()).isTrue();

        // 5. Verify Redis entry and TTL (approx 15 minutes)
        String redisKey = "suspended_users:" + vUser.getId();
        assertThat(redisTemplate.hasKey(redisKey)).isTrue();
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        assertThat(ttl).isNotNull().isGreaterThan(0).isLessThanOrEqualTo(900); // 15 mins is 900s

        // 6. Verify Audit logs
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.get(0).getEventType()).isEqualTo(AuditEventType.ACCOUNT_SUSPENDED);
        assertThat(auditLogs.get(0).getVendorId()).isEqualTo(vendor.getId());

        // 7. Verify subsequent request is rejected by JwtAuthenticationFilter immediately with 403
        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + vendorAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("User account is suspended."));
    }

    @Test
    @DisplayName("POST /api/admin/vendors/{id}/reactivate - reactivates user, deletes Redis blacklist, writes audit event")
    void shouldReactivateSuspendedVendor() throws Exception {
        User vUser = new User(UUID.randomUUID(), "vendor@inttest.com", "hashed", Role.VENDOR, AccountStatus.SUSPENDED);
        userRepository.save(vUser);
        Vendor vendor = new Vendor(UUID.randomUUID(), vUser, "Reactivated Business");
        vendor.setStatus(VendorStatus.APPROVED);
        vendorRepository.save(vendor);

        String redisKey = "suspended_users:" + vUser.getId();
        redisTemplate.opsForValue().set(redisKey, "true");

        String vendorAccessToken = jwtService.generateAccessToken(vUser.getId(), vUser.getEmail(), vUser.getRole().name());

        // 1. Authenticated request is blocked
        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + vendorAccessToken))
                .andExpect(status().isForbidden());

        // 2. Perform reactivation
        mockMvc.perform(post("/api/admin/vendors/{id}/reactivate", vendor.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Verify database status
        User updatedUser = userRepository.findById(vUser.getId()).orElseThrow();
        assertThat(updatedUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);

        // 4. Verify Redis entry is deleted
        assertThat(redisTemplate.hasKey(redisKey)).isFalse();

        // 5. Verify Audit logs
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.get(0).getEventType()).isEqualTo(AuditEventType.ACCOUNT_REACTIVATED);

        // 6. Verify subsequent request succeeds
        mockMvc.perform(get("/api/vendors/me")
                        .header("Authorization", "Bearer " + vendorAccessToken))
                .andExpect(status().isOk());
    }
}
