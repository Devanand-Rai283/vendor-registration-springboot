package com.streetvendor.admin.controller;

import com.streetvendor.admin.dto.AdminDashboardResponseDto;
import com.streetvendor.admin.service.AdminDashboardService;
import com.streetvendor.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing admin dashboard endpoints.
 *
 * <p>All endpoints under {@code /api/admin/dashboard} require the ADMIN role.
 * Authorization is enforced at the security filter chain level in
 * {@code SecurityConfig}. The controller contains no business logic.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin Dashboard", description = "Platform-wide overview metrics for administrators")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    /**
     * Returns aggregate platform metrics for the admin overview dashboard.
     *
     * @return 200 OK with {@link AdminDashboardResponseDto} wrapped in {@link ApiResponse}
     */
    @GetMapping
    @Operation(
            summary = "Get admin dashboard metrics",
            description = "Returns platform-wide aggregate counts: total vendors, "
                    + "pending approvals, total users, and orders placed today (UTC). "
                    + "Requires ADMIN role.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Dashboard metrics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AdminDashboardResponseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated — valid JWT required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — ADMIN role required")
    })
    public ResponseEntity<ApiResponse<AdminDashboardResponseDto>> getDashboard() {
        AdminDashboardResponseDto metrics = adminDashboardService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics retrieved successfully.", metrics));
    }
}
