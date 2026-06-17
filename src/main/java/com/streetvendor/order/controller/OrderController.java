package com.streetvendor.order.controller;

import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.order.dto.CancelOrderResponse;
import com.streetvendor.order.dto.CustomerOrderHistoryResponse;
import com.streetvendor.order.dto.OrderResponse;
import com.streetvendor.order.dto.PlaceOrderRequest;
import com.streetvendor.order.dto.PlaceOrderResponse;
import com.streetvendor.order.dto.PlaceOrderResult;
import com.streetvendor.order.dto.UpdateOrderStatusRequest;
import com.streetvendor.order.service.OrderCancellationService;
import com.streetvendor.order.service.OrderHistoryService;
import com.streetvendor.order.service.OrderProcessingService;
import com.streetvendor.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderProcessingService orderProcessingService;
    private final OrderHistoryService orderHistoryService;
    private final OrderCancellationService orderCancellationService;

    public OrderController(OrderService orderService,
                           OrderProcessingService orderProcessingService,
                           OrderHistoryService orderHistoryService,
                           OrderCancellationService orderCancellationService) {
        this.orderService = orderService;
        this.orderProcessingService = orderProcessingService;
        this.orderHistoryService = orderHistoryService;
        this.orderCancellationService = orderCancellationService;
    }

    @PostMapping
    public ResponseEntity<PlaceOrderResponse> placeOrder(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("X-Idempotency-Key header is required");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new UnauthorizedException("Invalid authentication");
        }

        UUID userId = user.getId();
        PlaceOrderResult result = orderService.placeOrder(userId, idempotencyKey, request);

        if (result.isDuplicate()) {
            return ResponseEntity.ok(result.response());
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body(result.response());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new UnauthorizedException("Invalid authentication");
        }

        UUID userId = user.getId();
        OrderResponse response = orderProcessingService.updateStatus(id, request.status(), userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<CustomerOrderHistoryResponse>> getOrderHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (user == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        Page<CustomerOrderHistoryResponse> response = orderHistoryService.getOrderHistory(user.getId(), page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<CancelOrderResponse> cancelOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        CancelOrderResponse response = orderCancellationService.cancelOrder(id, user.getId());
        return ResponseEntity.ok(response);
    }
}
