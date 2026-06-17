package com.streetvendor.order.service;

import com.streetvendor.order.dto.OrderResponse;
import com.streetvendor.order.enums.OrderStatus;
import java.util.UUID;

public interface OrderProcessingService {
    OrderResponse updateStatus(UUID orderId, OrderStatus newStatus, UUID userId);
}
