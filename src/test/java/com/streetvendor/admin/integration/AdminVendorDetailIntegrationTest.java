package com.streetvendor.admin.integration;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.support.AbstractSecurityTest;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.entity.VendorDocument;
import com.streetvendor.vendor.enums.DocumentType;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.enums.VerificationStatus;
import com.streetvendor.vendor.repository.VendorDocumentRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("admin-test")
@Testcontainers
@Transactional
@DisplayName("Admin Vendor Detail Integration Tests")
class AdminVendorDetailIntegrationTest extends AbstractSecurityTest {

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
    private VendorDocumentRepository vendorDocumentRepository;

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
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        vendorDocumentRepository.deleteAll();
        vendorRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = new User(UUID.randomUUID(), "admin.details@inttest.com",
                passwordEncoder.encode("Password1!"), Role.ADMIN, AccountStatus.ACTIVE);
        userRepository.save(adminUser);

        adminToken = jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("GET /api/admin/vendors/{id} - returns full vendor details and documents")
    void shouldReturnVendorDetailsAndDocuments() throws Exception {
        User vUser = new User(UUID.randomUUID(), "vendor.info@inttest.com", "hashed", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vUser);

        Vendor vendor = new Vendor(UUID.randomUUID(), vUser, "Integration Tacos");
        vendor.setOwnerName("Integration Owner");
        vendor.setPhone("9876543210");
        vendor.setStatus(VendorStatus.PENDING_REVIEW);
        vendor.setAddress("456 Integration Ave");
        vendor.setLatitude(BigDecimal.valueOf(34.05));
        vendor.setLongitude(BigDecimal.valueOf(-118.25));
        vendor.setAverageRating(BigDecimal.valueOf(4.8));
        vendor.setTotalReviews(25);
        vendorRepository.save(vendor);

        VendorDocument doc1 = new VendorDocument(UUID.randomUUID(), vendor, DocumentType.FSSAI_CERTIFICATE,
                "http://s3.url/doc1", VerificationStatus.VERIFIED, Instant.now());
        
        VendorDocument doc2 = new VendorDocument(UUID.randomUUID(), vendor, DocumentType.IDENTITY_PROOF,
                "http://s3.url/doc2", VerificationStatus.REJECTED, Instant.now());
        doc2.setRejectionReason("ID expired");
        
        vendorDocumentRepository.save(doc1);
        vendorDocumentRepository.save(doc2);

        mockMvc.perform(get("/api/admin/vendors/{id}", vendor.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(vendor.getId().toString()))
                .andExpect(jsonPath("$.data.businessName").value("Integration Tacos"))
                .andExpect(jsonPath("$.data.email").value("vendor.info@inttest.com"))
                .andExpect(jsonPath("$.data.phoneNumber").value("9876543210"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.documents.length()").value(2))
                .andExpect(jsonPath("$.data.documents[0].documentType").value("FSSAI_CERTIFICATE"))
                .andExpect(jsonPath("$.data.documents[0].status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.documents[1].documentType").value("IDENTITY_PROOF"))
                .andExpect(jsonPath("$.data.documents[1].status").value("REJECTED"))
                .andExpect(jsonPath("$.data.documents[1].rejectionReason").value("ID expired"));
    }
}
