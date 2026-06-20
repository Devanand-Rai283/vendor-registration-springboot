package com.streetvendor.order.service;

import com.streetvendor.order.dto.CustomerOrderHistoryResponse;
import com.streetvendor.order.dto.VendorOrderDetailResponse;
import com.streetvendor.order.dto.VendorOrderSummaryResponse;
import com.streetvendor.order.enums.OrderStatus;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface OrderHistoryService {
    Page<CustomerOrderHistoryResponse> getOrderHistory(UUID userId, int page, int size);

    Page<VendorOrderSummaryResponse> getVendorOrders(UUID vendorId, int page, int size, OrderStatus status);

    VendorOrderDetailResponse getVendorOrderDetail(UUID orderId, UUID vendorId);
}
