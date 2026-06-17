package com.streetvendor.order.service;

import com.streetvendor.order.dto.CancelOrderResponse;
import java.util.UUID;

public interface OrderCancellationService {
    CancelOrderResponse cancelOrder(UUID orderId, UUID userId);
}
