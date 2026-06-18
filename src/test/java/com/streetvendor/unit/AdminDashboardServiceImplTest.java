package com.streetvendor.unit;

import com.streetvendor.admin.dto.AdminDashboardResponseDto;
import com.streetvendor.admin.service.AdminDashboardServiceImpl;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminDashboardServiceImpl}.
 *
 * <p>Verifies that each metric is correctly sourced from its respective
 * repository and that the aggregated DTO is assembled correctly.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDashboardServiceImpl")
class AdminDashboardServiceImplTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    private AdminDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminDashboardServiceImpl(vendorRepository, userRepository, orderRepository);
    }

    @Test
    @DisplayName("should return correct total vendor count")
    void shouldReturnTotalVendorCount() {
        when(vendorRepository.count()).thenReturn(120L);
        when(vendorRepository.countByStatus(VendorStatus.PENDING_REVIEW)).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(orderRepository.countByCreatedAtBetween(any(Instant.class), any(Instant.class))).thenReturn(0L);

        AdminDashboardResponseDto result = service.getDashboardMetrics();

        assertThat(result.totalVendors()).isEqualTo(120L);
        verify(vendorRepository).count();
    }

    @Test
    @DisplayName("should return correct pending approval count")
    void shouldReturnPendingApprovalCount() {
        when(vendorRepository.count()).thenReturn(0L);
        when(vendorRepository.countByStatus(VendorStatus.PENDING_REVIEW)).thenReturn(14L);
        when(userRepository.count()).thenReturn(0L);
        when(orderRepository.countByCreatedAtBetween(any(Instant.class), any(Instant.class))).thenReturn(0L);

        AdminDashboardResponseDto result = service.getDashboardMetrics();

        assertThat(result.pendingApprovals()).isEqualTo(14L);
        verify(vendorRepository).countByStatus(eq(VendorStatus.PENDING_REVIEW));
    }

    @Test
    @DisplayName("should return correct total user count")
    void shouldReturnTotalUserCount() {
        when(vendorRepository.count()).thenReturn(0L);
        when(vendorRepository.countByStatus(VendorStatus.PENDING_REVIEW)).thenReturn(0L);
        when(userRepository.count()).thenReturn(850L);
        when(orderRepository.countByCreatedAtBetween(any(Instant.class), any(Instant.class))).thenReturn(0L);

        AdminDashboardResponseDto result = service.getDashboardMetrics();

        assertThat(result.totalUsers()).isEqualTo(850L);
        verify(userRepository).count();
    }

    @Test
    @DisplayName("should return correct orders-today count")
    void shouldReturnOrdersTodayCount() {
        when(vendorRepository.count()).thenReturn(0L);
        when(vendorRepository.countByStatus(VendorStatus.PENDING_REVIEW)).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(orderRepository.countByCreatedAtBetween(any(Instant.class), any(Instant.class))).thenReturn(42L);

        AdminDashboardResponseDto result = service.getDashboardMetrics();

        assertThat(result.totalOrdersToday()).isEqualTo(42L);
        verify(orderRepository).countByCreatedAtBetween(any(Instant.class), any(Instant.class));
    }

    @Test
    @DisplayName("should assemble all four metrics into the DTO correctly")
    void shouldAssembleAllMetricsCorrectly() {
        when(vendorRepository.count()).thenReturn(120L);
        when(vendorRepository.countByStatus(VendorStatus.PENDING_REVIEW)).thenReturn(14L);
        when(userRepository.count()).thenReturn(850L);
        when(orderRepository.countByCreatedAtBetween(any(Instant.class), any(Instant.class))).thenReturn(42L);

        AdminDashboardResponseDto result = service.getDashboardMetrics();

        assertThat(result.totalVendors()).isEqualTo(120L);
        assertThat(result.pendingApprovals()).isEqualTo(14L);
        assertThat(result.totalUsers()).isEqualTo(850L);
        assertThat(result.totalOrdersToday()).isEqualTo(42L);
    }

    @Test
    @DisplayName("should return zero counts when platform is empty")
    void shouldReturnZeroCountsWhenPlatformIsEmpty() {
        when(vendorRepository.count()).thenReturn(0L);
        when(vendorRepository.countByStatus(VendorStatus.PENDING_REVIEW)).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(orderRepository.countByCreatedAtBetween(any(Instant.class), any(Instant.class))).thenReturn(0L);

        AdminDashboardResponseDto result = service.getDashboardMetrics();

        assertThat(result.totalVendors()).isZero();
        assertThat(result.pendingApprovals()).isZero();
        assertThat(result.totalUsers()).isZero();
        assertThat(result.totalOrdersToday()).isZero();
    }
}
