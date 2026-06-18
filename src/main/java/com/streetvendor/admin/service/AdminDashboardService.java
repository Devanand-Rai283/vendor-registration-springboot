package com.streetvendor.admin.service;

import com.streetvendor.admin.dto.AdminDashboardResponseDto;

/**
 * Service contract for admin dashboard operations.
 *
 * <p>Provides aggregate platform metrics for the ADMIN dashboard.
 */
public interface AdminDashboardService {

    /**
     * Aggregates platform-wide metrics for the admin dashboard.
     *
     * @return {@link AdminDashboardResponseDto} containing counts for
     *         total vendors, pending approvals, total users, and orders placed today
     */
    AdminDashboardResponseDto getDashboardMetrics();
}
