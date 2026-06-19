package com.streetvendor.unit;

import com.streetvendor.analytics.dto.AnalyticsResponseDto;
import com.streetvendor.analytics.dto.AnalyticsSnapshotCacheDto;
import com.streetvendor.analytics.dto.AnalyticsSnapshotResponseDto;
import com.streetvendor.analytics.entity.AnalyticsSnapshot;
import com.streetvendor.analytics.repository.AnalyticsSnapshotRepository;
import com.streetvendor.analytics.service.impl.AnalyticsServiceImpl;
import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.order.repository.OrderItemRepository;
import com.streetvendor.payment.repository.PaymentRepository;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceQueryUnitTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private AnalyticsSnapshotRepository analyticsSnapshotRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private SecurityContext originalContext;

    @BeforeEach
    void setUp() {
        originalContext = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.setContext(originalContext);
    }

    @Test
    void shouldThrowUnauthorizedWhenNoAuthentication() {
        // Arrange
        UUID vendorId = UUID.randomUUID();

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> analyticsService.getVendorAnalytics(vendorId, 30));
    }

    @Test
    void shouldThrowUnauthorizedWhenInvalidPrincipal() {
        // Arrange
        UUID vendorId = UUID.randomUUID();
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("Not A User Object");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> analyticsService.getVendorAnalytics(vendorId, 30));
    }

    @Test
    void shouldThrowForbiddenForCustomerRole() {
        // Arrange
        UUID vendorId = UUID.randomUUID();
        User customer = new User(UUID.randomUUID(), "customer@gmail.com", "hash", Role.CUSTOMER, AccountStatus.ACTIVE);
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(customer);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> analyticsService.getVendorAnalytics(vendorId, 30));
    }

    @Test
    void shouldThrowForbiddenForMismatchedVendor() {
        // Arrange
        UUID requestedVendorId = UUID.randomUUID();
        UUID actualVendorId = UUID.randomUUID();

        User vendorUser = new User(UUID.randomUUID(), "vendor@gmail.com", "hash", Role.VENDOR, AccountStatus.ACTIVE);
        Vendor vendorProfile = new Vendor(actualVendorId, vendorUser, "Vendor Shop");

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(vendorUser);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendorProfile));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                analyticsService.getVendorAnalytics(requestedVendorId, 30));
        assertEquals("Vendors are only allowed to access their own analytics", exception.getMessage());
    }

    @Test
    void shouldReturnAnalyticsForMatchingVendorOnCacheHit() {
        // Arrange
        UUID vendorId = UUID.randomUUID();
        User vendorUser = new User(UUID.randomUUID(), "vendor@gmail.com", "hash", Role.VENDOR, AccountStatus.ACTIVE);
        Vendor vendorProfile = new Vendor(vendorId, vendorUser, "Vendor Shop");

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(vendorUser);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendorProfile));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        UUID itemId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<AnalyticsSnapshotCacheDto> cacheData = List.of(
                new AnalyticsSnapshotCacheDto(today.minusDays(1), 10, BigDecimal.valueOf(500), BigDecimal.valueOf(50), itemId, 12),
                new AnalyticsSnapshotCacheDto(today.minusDays(5), 5, BigDecimal.valueOf(250), BigDecimal.valueOf(50), itemId, 13)
        );

        when(valueOperations.get("analytics:" + vendorId)).thenReturn(cacheData);

        MenuItem item = new MenuItem(itemId, null, vendorProfile, "Samosa", BigDecimal.valueOf(50));
        when(menuItemRepository.findAllById(List.of(itemId))).thenReturn(List.of(item));

        // Act
        AnalyticsResponseDto response = analyticsService.getVendorAnalytics(vendorId, 3);

        // Assert
        assertNotNull(response);
        assertEquals(vendorId, response.getVendorId());
        assertEquals(3, response.getPeriodDays());
        // Since we requested 3 days, today.minusDays(5) should be filtered out
        assertEquals(1, response.getSnapshots().size());
        AnalyticsSnapshotResponseDto snapshotDto = response.getSnapshots().get(0);
        assertEquals(today.minusDays(1), snapshotDto.getSnapshotDate());
        assertEquals(10, snapshotDto.getTotalOrders());
        assertEquals(BigDecimal.valueOf(500), snapshotDto.getTotalRevenue());
        assertEquals("Samosa", snapshotDto.getTopItem());
        assertEquals(12, snapshotDto.getPeakHour());

        verify(analyticsSnapshotRepository, never()).findByVendorIdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(any(), any());
    }

    @Test
    void shouldReturnAnalyticsForMatchingVendorOnCacheMissAndRepopulateCache() {
        // Arrange
        UUID vendorId = UUID.randomUUID();
        User vendorUser = new User(UUID.randomUUID(), "vendor@gmail.com", "hash", Role.VENDOR, AccountStatus.ACTIVE);
        Vendor vendorProfile = new Vendor(vendorId, vendorUser, "Vendor Shop");

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(vendorUser);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendorProfile));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("analytics:" + vendorId)).thenReturn(null); // Cache Miss

        UUID itemId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        AnalyticsSnapshot dbSnapshot = new AnalyticsSnapshot(
                UUID.randomUUID(),
                vendorProfile,
                today.minusDays(2),
                15,
                BigDecimal.valueOf(750),
                BigDecimal.valueOf(50),
                itemId,
                14
        );

        when(analyticsSnapshotRepository.findByVendorIdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(eq(vendorId), any(LocalDate.class)))
                .thenReturn(List.of(dbSnapshot));

        MenuItem item = new MenuItem(itemId, null, vendorProfile, "Tea", BigDecimal.valueOf(50));
        when(menuItemRepository.findAllById(List.of(itemId))).thenReturn(List.of(item));

        // Act
        AnalyticsResponseDto response = analyticsService.getVendorAnalytics(vendorId, 30);

        // Assert
        assertNotNull(response);
        assertEquals(vendorId, response.getVendorId());
        assertEquals(30, response.getPeriodDays());
        assertEquals(1, response.getSnapshots().size());
        assertEquals("Tea", response.getSnapshots().get(0).getTopItem());

        verify(valueOperations).set(eq("analytics:" + vendorId), anyList(), eq(Duration.ofSeconds(900)));
    }

    @Test
    void shouldAllowAdminToAccessAnyVendorAnalytics() {
        // Arrange
        UUID targetVendorId = UUID.randomUUID();
        User adminUser = new User(UUID.randomUUID(), "admin@gmail.com", "hash", Role.ADMIN, AccountStatus.ACTIVE);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(adminUser);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("analytics:" + targetVendorId)).thenReturn(Collections.emptyList());

        // Act
        AnalyticsResponseDto response = analyticsService.getVendorAnalytics(targetVendorId, 30);

        // Assert
        assertNotNull(response);
        assertEquals(targetVendorId, response.getVendorId());
        assertTrue(response.getSnapshots().isEmpty());
        verifyNoInteractions(vendorRepository);
    }
}
