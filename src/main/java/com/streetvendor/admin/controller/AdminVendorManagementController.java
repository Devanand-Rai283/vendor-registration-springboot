package com.streetvendor.admin.controller;

import com.streetvendor.admin.dto.AdminVendorDetailResponseDto;
import com.streetvendor.admin.dto.AdminVendorSummaryDto;
import com.streetvendor.admin.service.AdminVendorManagementService;
import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.common.response.ApiResponse;
import com.streetvendor.vendor.enums.VendorStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for administrator actions on vendors.
 *
 * <p>All endpoints require the ADMIN role, which is enforced by the security
 * filter chain at {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/vendors")
@Tag(name = "Admin Vendor Management", description = "Administrative operations for vendor accounts")
@SecurityRequirement(name = "bearerAuth")
public class AdminVendorManagementController {

    private final AdminVendorManagementService adminVendorManagementService;

    public AdminVendorManagementController(AdminVendorManagementService adminVendorManagementService) {
        this.adminVendorManagementService = adminVendorManagementService;
    }

    /**
     * Retrieves a paginated list of vendor accounts, optionally filtered by status.
     * Results are sorted by createdAt descending.
     */
    @GetMapping
    @Operation(
            summary = "List all vendors (paginated)",
            description = "Returns a paginated list of all vendors in the system. "
                    + "Allows filtering by status (PENDING_REVIEW, APPROVED, REJECTED). "
                    + "Sorted by creation timestamp descending. Requires ADMIN role.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Vendor list retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated — valid JWT required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — ADMIN role required")
    })
    public ResponseEntity<ApiResponse<Page<AdminVendorSummaryDto>>> getVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) VendorStatus status) {
        Page<AdminVendorSummaryDto> vendorsPage = adminVendorManagementService.getVendors(status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("Vendors retrieved successfully.", vendorsPage));
    }

    /**
     * Suspends a vendor's account, invalidates active sessions, and revokes refresh tokens.
     */
    @PostMapping("/{id}/suspend")
    @Operation(
            summary = "Suspend vendor account",
            description = "Suspends the vendor account with the specified ID. "
                    + "This updates the user account status to SUSPENDED, revokes all active refresh tokens "
                    + "for the user, blacklists the user ID in Redis with a 15-minute TTL, and logs the event. "
                    + "Requires ADMIN role.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Vendor account suspended successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Vendor or associated user not found")
    })
    public ResponseEntity<ApiResponse<Void>> suspendVendor(
            @Parameter(description = "The vendor ID to suspend") @PathVariable UUID id,
            @AuthenticationPrincipal User adminUser) {
        if (adminUser == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        adminVendorManagementService.suspendVendor(id, adminUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Vendor account suspended successfully."));
    }

    /**
     * Reactivates a suspended vendor's account.
     */
    @PostMapping("/{id}/reactivate")
    @Operation(
            summary = "Reactivate vendor account",
            description = "Reactivates the vendor account with the specified ID. "
                    + "This restores the user account status to ACTIVE, removes the Redis restriction, "
                    + "and logs the event. Requires ADMIN role.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Vendor account reactivated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Vendor or associated user not found")
    })
    public ResponseEntity<ApiResponse<Void>> reactivateVendor(
            @Parameter(description = "The vendor ID to reactivate") @PathVariable UUID id,
            @AuthenticationPrincipal User adminUser) {
        if (adminUser == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        adminVendorManagementService.reactivateVendor(id, adminUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Vendor account reactivated successfully."));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information about a vendor and its documents")
    public ResponseEntity<ApiResponse<AdminVendorDetailResponseDto>> getVendorDetails(
            @PathVariable UUID id) {
        AdminVendorDetailResponseDto details = adminVendorManagementService.getVendorDetails(id);
        return ResponseEntity.ok(ApiResponse.success("Vendor details retrieved successfully.", details));
    }
}
