package com.streetvendor.integration;

import com.streetvendor.analytics.dto.AnalyticsSnapshotCacheDto;
import com.streetvendor.analytics.entity.AnalyticsSnapshot;
import com.streetvendor.analytics.repository.AnalyticsSnapshotRepository;
import com.streetvendor.analytics.service.AnalyticsService;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.entity.OrderItem;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.payment.entity.Payment;
import com.streetvendor.payment.enums.PaymentStatus;
import com.streetvendor.payment.repository.PaymentRepository;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Testcontainers
@ActiveProfiles("vendor-test")
@Transactional
@DisplayName("AnalyticsSnapshot Integration Tests")
class AnalyticsSnapshotIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Postgres properties override
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Redis properties override
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.ping-on-startup", () -> "false");
    }

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AnalyticsSnapshotRepository analyticsSnapshotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Vendor vendor;
    private Customer customer;
    private MenuCategory category;
    private MenuItem itemA;
    private MenuItem itemB;
    private LocalDate yesterdayDate;
    private Instant yesterdayInstant;

    @BeforeEach
    void setUp() {
        // Purge Redis
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        yesterdayDate = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        yesterdayInstant = yesterdayDate.atStartOfDay(ZoneOffset.UTC).toInstant();

        // Create core records
        User vendorUser = new User(UUID.randomUUID(), "vendor" + UUID.randomUUID() + "@test.com", "hash", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);

        vendor = new Vendor(UUID.randomUUID(), vendorUser, "Delicious Foods");
        vendor.setStatus(VendorStatus.APPROVED);
        vendor = vendorRepository.save(vendor);

        User customerUser = new User(UUID.randomUUID(), "customer" + UUID.randomUUID() + "@test.com", "hash", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);

        customer = new Customer(UUID.randomUUID(), customerUser.getId(), "John Doe", "9876543210", "Address", null, null);
        customer = customerRepository.save(customer);

        category = new MenuCategory(UUID.randomUUID(), vendor, "Snacks", 1);
        category = menuCategoryRepository.save(category);

        itemA = new MenuItem(UUID.randomUUID(), category, vendor, "Samosa", BigDecimal.valueOf(30.00));
        itemA = menuItemRepository.save(itemA);

        itemB = new MenuItem(UUID.randomUUID(), category, vendor, "Chai", BigDecimal.valueOf(15.00));
        itemB = menuItemRepository.save(itemB);

        entityManager.flush();
    }

    private Order createOrder(OrderStatus status) {
        Order order = new Order(UUID.randomUUID(), customer, vendor, BigDecimal.valueOf(0), UUID.randomUUID().toString());
        order.setStatus(status);
        return orderRepository.save(order);
    }

    private void backdateOrder(UUID orderId, Instant createdAt) {
        jdbcTemplate.update("UPDATE orders SET created_at = ? WHERE id = ?", Timestamp.from(createdAt), orderId);
    }

    private void addOrderItem(Order order, MenuItem menuItem, int quantity) {
        OrderItem orderItem = new OrderItem(UUID.randomUUID(), order, menuItem, quantity, menuItem.getPrice());
        order.addItem(orderItem);
        order.setTotalAmount(order.getTotalAmount().add(orderItem.getSubtotal()));
        orderRepository.save(order);
    }

    private void createPayment(Order order, PaymentStatus status, int amountInPaise) {
        Payment payment = new Payment(UUID.randomUUID(), order, "rp_ord_" + UUID.randomUUID(), amountInPaise);
        payment.setStatus(status);
        paymentRepository.save(payment);
    }

    @Test
    @DisplayName("should aggregate and save snapshot for yesterday's paid and completed orders")
    void shouldGenerateSnapshotCorrectly() {
        // Arrange
        // Order 1: Completed, Paid, yesterday at 10:00 UTC
        Order order1 = createOrder(OrderStatus.COMPLETED);
        addOrderItem(order1, itemA, 2); // 2 * 30 = 60
        addOrderItem(order1, itemB, 1); // 1 * 15 = 15 -> total order: 75
        createPayment(order1, PaymentStatus.PAID, 7500); // 7500 paise = 75 Rupees

        // Order 2: Completed, Paid, yesterday at 10:30 UTC
        Order order2 = createOrder(OrderStatus.COMPLETED);
        addOrderItem(order2, itemA, 1); // 1 * 30 = 30 -> total order: 30
        createPayment(order2, PaymentStatus.PAID, 3000); // 3000 paise = 30 Rupees

        entityManager.flush();

        // Backdate after flushing all changes to the database
        backdateOrder(order1.getId(), yesterdayInstant.plus(Duration.ofHours(10)));
        backdateOrder(order2.getId(), yesterdayInstant.plus(Duration.ofHours(10)).plus(Duration.ofMinutes(30)));

        entityManager.clear(); // Clear L1 cache to force reload with backdated times

        // Act
        analyticsService.generateSnapshots(yesterdayDate);

        // Assert
        // Check DB snapshot
        Optional<AnalyticsSnapshot> optSnapshot = analyticsSnapshotRepository.findByVendorIdAndSnapshotDate(vendor.getId(), yesterdayDate);
        assertThat(optSnapshot).isPresent();

        AnalyticsSnapshot snapshot = optSnapshot.get();
        assertThat(snapshot.getTotalOrders()).isEqualTo(2);
        assertThat(snapshot.getTotalRevenue().doubleValue()).isEqualTo(105.00);
        assertThat(snapshot.getAverageOrderValue().doubleValue()).isEqualTo(52.50);
        assertThat(snapshot.getTopItemId()).isEqualTo(itemA.getId()); // Samosa total quantity is 3, Chai is 1
        assertThat(snapshot.getPeakHour()).isEqualTo(10); // Both orders created between 10:00 and 11:00 UTC

        // Check Redis cache
        String cacheKey = "analytics:" + vendor.getId();
        List<AnalyticsSnapshotCacheDto> cacheList = (List<AnalyticsSnapshotCacheDto>) redisTemplate.opsForValue().get(cacheKey);
        assertThat(cacheList).isNotNull().hasSize(1);

        AnalyticsSnapshotCacheDto cached = cacheList.get(0);
        assertThat(cached.getSnapshotDate()).isEqualTo(yesterdayDate);
        assertThat(cached.getTotalOrders()).isEqualTo(2);
        assertThat(cached.getTotalRevenue().doubleValue()).isEqualTo(105.00);
        assertThat(cached.getAverageOrderValue().doubleValue()).isEqualTo(52.50);
        assertThat(cached.getTopItemId()).isEqualTo(itemA.getId());
        assertThat(cached.getPeakHour()).isEqualTo(10);

        // Check Cache TTL
        Long expire = redisTemplate.getExpire(cacheKey);
        assertThat(expire).isNotNull().isGreaterThan(0).isLessThanOrEqualTo(900);
    }

    @Test
    @DisplayName("should update database row and cache atomically on conflict")
    void shouldUpsertSnapshotCorrectlyOnConflict() {
        // Arrange first run
        Order order1 = createOrder(OrderStatus.COMPLETED);
        addOrderItem(order1, itemA, 1);
        createPayment(order1, PaymentStatus.PAID, 3000);

        entityManager.flush();

        // Backdate
        backdateOrder(order1.getId(), yesterdayInstant.plus(Duration.ofHours(8)));

        entityManager.clear();

        analyticsService.generateSnapshots(yesterdayDate);

        // Verify first run DB entries
        Optional<AnalyticsSnapshot> snapshot1 = analyticsSnapshotRepository.findByVendorIdAndSnapshotDate(vendor.getId(), yesterdayDate);
        assertThat(snapshot1).isPresent();
        assertThat(snapshot1.get().getTotalOrders()).isEqualTo(1);
        assertThat(snapshot1.get().getTotalRevenue().doubleValue()).isEqualTo(30.00);

        // Act - add another order and run snapshot generator again
        Order order2 = createOrder(OrderStatus.COMPLETED);
        addOrderItem(order2, itemB, 2);
        createPayment(order2, PaymentStatus.PAID, 3000);

        entityManager.flush();

        // Backdate order2
        backdateOrder(order2.getId(), yesterdayInstant.plus(Duration.ofHours(14)));

        entityManager.clear();

        analyticsService.generateSnapshots(yesterdayDate);

        // Assert - DB updated in-place without duplicate rows
        List<AnalyticsSnapshot> allSnapshots = analyticsSnapshotRepository.findByVendorIdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(vendor.getId(), yesterdayDate);
        assertThat(allSnapshots).hasSize(1);

        AnalyticsSnapshot updatedSnapshot = allSnapshots.get(0);
        assertThat(updatedSnapshot.getTotalOrders()).isEqualTo(2);
        assertThat(updatedSnapshot.getTotalRevenue().doubleValue()).isEqualTo(60.00);
        assertThat(updatedSnapshot.getAverageOrderValue().doubleValue()).isEqualTo(30.00);

        // Redis cache should also be updated
        String cacheKey = "analytics:" + vendor.getId();
        List<AnalyticsSnapshotCacheDto> cacheList = (List<AnalyticsSnapshotCacheDto>) redisTemplate.opsForValue().get(cacheKey);
        assertThat(cacheList).isNotNull().hasSize(1);
        assertThat(cacheList.get(0).getTotalOrders()).isEqualTo(2);
        assertThat(cacheList.get(0).getTotalRevenue().doubleValue()).isEqualTo(60.00);
    }

    @Test
    @DisplayName("should exclude non-COMPLETED or non-PAID orders from snapshot calculations")
    void shouldIgnoreUnpaidOrNonCompletedOrders() {
        // Order A: PLACED, PAID (Should be ignored because not COMPLETED)
        Order orderPlaced = createOrder(OrderStatus.PLACED);
        addOrderItem(orderPlaced, itemA, 2);
        createPayment(orderPlaced, PaymentStatus.PAID, 6000);

        // Order B: COMPLETED, CREATED/Unpaid (Should be ignored because payment status is not PAID)
        Order orderUnpaid = createOrder(OrderStatus.COMPLETED);
        addOrderItem(orderUnpaid, itemB, 4);
        createPayment(orderUnpaid, PaymentStatus.CREATED, 6000);

        // Order C: COMPLETED, PAID (Should be counted)
        Order orderValid = createOrder(OrderStatus.COMPLETED);
        addOrderItem(orderValid, itemA, 1);
        createPayment(orderValid, PaymentStatus.PAID, 3000);

        entityManager.flush();

        // Backdate all orders after all saves/flushes are done
        backdateOrder(orderPlaced.getId(), yesterdayInstant.plus(Duration.ofHours(9)));
        backdateOrder(orderUnpaid.getId(), yesterdayInstant.plus(Duration.ofHours(10)));
        backdateOrder(orderValid.getId(), yesterdayInstant.plus(Duration.ofHours(11)));

        entityManager.clear();

        // Act
        analyticsService.generateSnapshots(yesterdayDate);

        // Assert
        Optional<AnalyticsSnapshot> optSnapshot = analyticsSnapshotRepository.findByVendorIdAndSnapshotDate(vendor.getId(), yesterdayDate);
        assertThat(optSnapshot).isPresent();

        AnalyticsSnapshot snapshot = optSnapshot.get();
        // Only Order C should be processed
        assertThat(snapshot.getTotalOrders()).isEqualTo(1);
        assertThat(snapshot.getTotalRevenue().doubleValue()).isEqualTo(30.00);
        assertThat(snapshot.getAverageOrderValue().doubleValue()).isEqualTo(30.00);
        assertThat(snapshot.getTopItemId()).isEqualTo(itemA.getId());
        assertThat(snapshot.getPeakHour()).isEqualTo(11);
    }
}
