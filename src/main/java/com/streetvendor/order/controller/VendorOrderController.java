package com.streetvendor.order.controller;

import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.common.response.ApiResponse;
import com.streetvendor.order.dto.VendorOrderDetailResponse;
import com.streetvendor.order.dto.VendorOrderSummaryResponse;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.service.OrderHistoryService;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.service.VendorService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/vendors/orders")
public class VendorOrderController {

    private final OrderHistoryService orderHistoryService;
    private final VendorService vendorService;

    public VendorOrderController(OrderHistoryService orderHistoryService, VendorService vendorService) {
        this.orderHistoryService = orderHistoryService;
        this.vendorService = vendorService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<VendorOrderSummaryResponse>>> getVendorOrders(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status) {

        if (user == null) {
            throw new UnauthorizedException("Not authenticated");
        }

        Vendor vendor = vendorService.getVendorByUserId(user.getId());
        Page<VendorOrderSummaryResponse> orders = orderHistoryService.getVendorOrders(vendor.getId(), page, size, status);

        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<VendorOrderDetailResponse>> getVendorOrderDetail(
            @AuthenticationPrincipal User user,
            @PathVariable UUID orderId) {

        if (user == null) {
            throw new UnauthorizedException("Not authenticated");
        }

        Vendor vendor = vendorService.getVendorByUserId(user.getId());
        VendorOrderDetailResponse orderDetail = orderHistoryService.getVendorOrderDetail(orderId, vendor.getId());

        return ResponseEntity.ok(ApiResponse.success("Order detail retrieved successfully", orderDetail));
    }
}
