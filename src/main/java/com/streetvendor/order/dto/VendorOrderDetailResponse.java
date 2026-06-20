package com.streetvendor.order.dto;

import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VendorOrderDetailResponse(
        UUID orderId,
        UUID customerId,
        String customerName,
        String customerPhone,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        String notes,
        List<VendorOrderItemDto> items,
        Instant createdAt,
        Instant updatedAt
) {}
