package com.streetvendor.order.service;

import com.streetvendor.order.dto.PlaceOrderRequest;
import com.streetvendor.order.dto.PlaceOrderResult;
import java.util.UUID;

public interface OrderService {
    PlaceOrderResult placeOrder(UUID userId, String idempotencyKey, PlaceOrderRequest request);
}
