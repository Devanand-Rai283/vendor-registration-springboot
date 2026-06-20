package com.streetvendor.vendor.dto;

import java.math.BigDecimal;

public record VendorDashboardMetricsResponseDto(
        Integer activeOrders,
        Long totalOrders,
        BigDecimal totalRevenue,
        BigDecimal averageOrderValue,
        BigDecimal averageRating,
        Integer totalReviews
) {}
