package com.streetvendor.order.dto;

import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerOrderHistoryResponse(
        UUID orderId,
        UUID vendorId,
        String vendorBusinessName,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        Instant createdAt
) {
}
