package com.streetvendor.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VendorOrderItemDto(
        UUID menuItemId,
        String itemName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
