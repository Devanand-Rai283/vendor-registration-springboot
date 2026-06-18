package com.streetvendor.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO returned by {@code GET /api/admin/dashboard}.
 *
 * <p>Contains platform-wide aggregate metrics visible only to ADMIN users.
 * No personally-identifiable information is exposed.
 */
@Schema(description = "Admin dashboard aggregate metrics")
public record AdminDashboardResponseDto(

        @Schema(description = "Total number of vendor profiles in the platform", example = "120")
        long totalVendors,

        @Schema(description = "Number of vendor profiles awaiting admin review", example = "14")
        long pendingApprovals,

        @Schema(description = "Total number of registered users (all roles)", example = "850")
        long totalUsers,

        @Schema(description = "Number of orders placed today (UTC calendar day)", example = "42")
        long totalOrdersToday
) {
}
