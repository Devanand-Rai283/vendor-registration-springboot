package com.streetvendor.admin.service;

import com.streetvendor.admin.dto.AdminDashboardResponseDto;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.vendor.enums.VendorStatus;
import com.streetvendor.vendor.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Default implementation of {@link AdminDashboardService}.
 *
 * <p>Aggregates counts from {@link VendorRepository}, {@link UserRepository},
 * and {@link OrderRepository}. All reads are performed within a single
 * read-only transaction.
 */
@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public AdminDashboardServiceImpl(
            VendorRepository vendorRepository,
            UserRepository userRepository,
            OrderRepository orderRepository) {
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponseDto getDashboardMetrics() {
        long totalVendors = vendorRepository.count();
        long pendingApprovals = vendorRepository.countByStatus(VendorStatus.PENDING_REVIEW);
        long totalUsers = userRepository.count();
        long totalOrdersToday = countOrdersToday();

        return new AdminDashboardResponseDto(totalVendors, pendingApprovals, totalUsers, totalOrdersToday);
    }

    /**
     * Counts orders whose {@code createdAt} timestamp falls within the current
     * UTC calendar day (midnight UTC to midnight UTC exclusive).
     */
    private long countOrdersToday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfNextDay = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return orderRepository.countByCreatedAtBetween(startOfDay, startOfNextDay);
    }
}
