package com.streetvendor.order.service;

import com.streetvendor.order.dto.CustomerOrderHistoryResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface OrderHistoryService {
    Page<CustomerOrderHistoryResponse> getOrderHistory(UUID userId, int page, int size);
}
