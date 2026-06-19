package com.streetvendor.integration;

import com.streetvendor.analytics.dto.AnalyticsResponseDto;
import com.streetvendor.analytics.entity.AnalyticsSnapshot;
import com.streetvendor.analytics.repository.AnalyticsSnapshotRepository;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.security.JwtService;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Testcontainers
@ActiveProfiles("vendor-test")
@Transactional
@DisplayName("Analytics API Integration Tests")
class AnalyticsApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.ping-on-startup", () -> "false");
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AnalyticsSnapshotRepository analyticsSnapshotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    private Vendor vendorA;
    private Vendor vendorB;
    private User vendorUserA;
    private User vendorUserB;
    private User adminUser;
    private User customerUser;
    private MenuItem menuItem;

    private String tokenVendorA;
    private String tokenVendorB;
    private String tokenAdmin;
    private String tokenCustomer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Purge Redis
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        // Create Users
        vendorUserA = new User(UUID.randomUUID(), "vendorA@test.com", "hash", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUserA);

        vendorUserB = new User(UUID.randomUUID(), "vendorB@test.com", "hash", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUserB);

        adminUser = new User(UUID.randomUUID(), "admin@test.com", "hash", Role.ADMIN, AccountStatus.ACTIVE);
        userRepository.save(adminUser);

        customerUser = new User(UUID.randomUUID(), "customer@test.com", "hash", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);

        // Create Vendors
        vendorA = new Vendor(UUID.randomUUID(), vendorUserA, "Vendor A Shop");
        vendorA.setStatus(VendorStatus.APPROVED);
        vendorRepository.save(vendorA);

        vendorB = new Vendor(UUID.randomUUID(), vendorUserB, "Vendor B Shop");
        vendorB.setStatus(VendorStatus.APPROVED);
        vendorRepository.save(vendorB);

        // Create MenuItem for name resolution testing
        MenuCategory category = new MenuCategory(UUID.randomUUID(), vendorA, "Snacks", 1);
        menuCategoryRepository.save(category);

        menuItem = new MenuItem(UUID.randomUUID(), category, vendorA, "Special Samosa", BigDecimal.valueOf(35));
        menuItemRepository.save(menuItem);

        // Generate Tokens
        tokenVendorA = jwtService.generateAccessToken(vendorUserA.getId(), vendorUserA.getEmail(), "VENDOR");
        tokenVendorB = jwtService.generateAccessToken(vendorUserB.getId(), vendorUserB.getEmail(), "VENDOR");
        tokenAdmin = jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), "ADMIN");
        tokenCustomer = jwtService.generateAccessToken(customerUser.getId(), customerUser.getEmail(), "CUSTOMER");
    }

    @Test
    @DisplayName("GET /api/vendors/{id}/analytics - Should return 401 for anonymous access")
    void shouldReturn401ForAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/vendors/" + vendorA.getId() + "/analytics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/vendors/{id}/analytics - Should return 403 for CUSTOMER role")
    void shouldReturn403ForCustomer() throws Exception {
        mockMvc.perform(get("/api/vendors/" + vendorA.getId() + "/analytics")
                        .header("Authorization", "Bearer " + tokenCustomer))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/vendors/{id}/analytics - Should return 403 for mismatched vendor")
    void shouldReturn403ForMismatchedVendor() throws Exception {
        mockMvc.perform(get("/api/vendors/" + vendorA.getId() + "/analytics")
                        .header("Authorization", "Bearer " + tokenVendorB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/vendors/{id}/analytics - Should validate days parameter boundaries")
    void shouldReturn400ForInvalidDaysParameter() throws Exception {
        mockMvc.perform(get("/api/vendors/" + vendorA.getId() + "/analytics?days=0")
                        .header("Authorization", "Bearer " + tokenVendorA))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/vendors/" + vendorA.getId() + "/analytics?days=91")
                        .header("Authorization", "Bearer " + tokenVendorA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/vendors/{id}/analytics - Should return empty snapshots list if no data exists")
    void shouldReturnEmptyListIfNoDataExists() throws Exception {
        mockMvc.perform(get("/api/vendors/" + vendorA.getId() + "/analytics")
                        .header("Authorization", "Bearer " + tokenVendorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorId").value(vendorA.getId().toString()))
                .andExpect(jsonPath("$.periodDays").value(30))
                .andExpect(jsonPath("$.snapshots").isEmpty());
    }

    @Test
    @DisplayName("GET /api/vendors/{id}/analytics - Should fetch snapshots, resolve menu item names, and hit/populate cache")
    void shouldSuccessfullyRetrieveAnalyticsSnapshots() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        AnalyticsSnapshot snapshot = new AnalyticsSnapshot(
                UUID.randomUUID(),
                vendorA,
                today.minusDays(2),
                12,
                BigDecimal.valueOf(420.00),
                BigDecimal.valueOf(35.00),
                menuItem.getId(),
                17
        );
        analyticsSnapshotRepository.save(snapshot);

        // 2. Perform GET - Cache Miss (reads from DB, populates cache)
        mockMvc.perform(get("/api/vendors/" + vendorA.getId() + "/analytics?days=5")
                        .header("Authorization", "Bearer " + tokenVendorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorId").value(vendorA.getId().toString()))
                .andExpect(jsonPath("$.periodDays").value(5))
                .andExpect(jsonPath("$.snapshots.length()").value(1))
                .andExpect(jsonPath("$.snapshots[0].snapshotDate").value(today.minusDays(2).toString()))
                .andExpect(jsonPath("$.snapshots[0].totalOrders").value(12))
                .andExpect(jsonPath("$.snapshots[0].totalRevenue").value(420.00))
                .andExpect(jsonPath("$.snapshots[0].averageOrderValue").value(35.00))
                .andExpect(jsonPath("$.snapshots[0].topItem").value("Special Samosa"))
                .andExpect(jsonPath("$.snapshots[0].peakHour").value(17));

        // 3. Verify Redis cache key is populated
        String cacheKey = "analytics:" + vendorA.getId();
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        assertThat(cachedObj).isNotNull();

        // 4. Delete DB record to prove next request hits Redis cache
        analyticsSnapshotRepository.deleteAll();

        // 5. Perform GET - Cache Hit (reads from cache, DB is empty)
        mockMvc.perform(get("/api/vendors/" + vendorA.getId() + "/analytics?days=5")
                        .header("Authorization", "Bearer " + tokenVendorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshots.length()").value(1))
                .andExpect(jsonPath("$.snapshots[0].topItem").value("Special Samosa"));
    }

    @Test
    @DisplayName("GET /api/vendors/{id}/analytics - ADMIN role should bypass ownership check")
    void shouldAllowAdminToAccessAnyVendor() throws Exception {
        mockMvc.perform(get("/api/vendors/" + vendorA.getId() + "/analytics")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorId").value(vendorA.getId().toString()));
    }
}
