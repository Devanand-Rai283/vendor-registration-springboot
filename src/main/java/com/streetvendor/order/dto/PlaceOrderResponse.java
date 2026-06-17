package com.streetvendor.order.dto;

import com.streetvendor.order.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlaceOrderResponse(
        UUID orderId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt
) {
}
