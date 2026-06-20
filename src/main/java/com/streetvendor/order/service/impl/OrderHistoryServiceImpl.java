package com.streetvendor.order.service.impl;

import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.order.dto.CustomerOrderHistoryResponse;
import com.streetvendor.order.dto.VendorOrderDetailResponse;
import com.streetvendor.order.dto.VendorOrderItemDto;
import com.streetvendor.order.dto.VendorOrderSummaryResponse;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.order.service.OrderHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderHistoryServiceImpl implements OrderHistoryService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public OrderHistoryServiceImpl(OrderRepository orderRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerOrderHistoryResponse> getOrderHistory(UUID userId, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        if (size > 100) {
            throw new IllegalArgumentException("Page size must not exceed 100");
        }

        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException("Customer profile not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderRepository.findByCustomerId(customer.getId(), pageable);

        return orders.map(order -> new CustomerOrderHistoryResponse(
                order.getId(),
                order.getVendor().getId(),
                order.getVendor().getBusinessName(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getCreatedAt()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VendorOrderSummaryResponse> getVendorOrders(UUID vendorId, int page, int size, OrderStatus status) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        if (size > 100) {
            throw new IllegalArgumentException("Page size must not exceed 100");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Order> orders;
        if (status != null) {
            orders = orderRepository.findByVendorIdAndStatus(vendorId, status, pageable);
        } else {
            orders = orderRepository.findByVendorId(vendorId, pageable);
        }

        return orders.map(order -> new VendorOrderSummaryResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getFullName(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getCreatedAt()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public VendorOrderDetailResponse getVendorOrderDetail(UUID orderId, UUID vendorId) {
        Order order = orderRepository.findByIdAndVendorId(orderId, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or access denied"));

        List<VendorOrderItemDto> items = order.getItems().stream()
                .map(item -> new VendorOrderItemDto(
                        item.getMenuItem().getId(),
                        item.getMenuItem().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());

        return new VendorOrderDetailResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getFullName(),
                order.getCustomer().getPhone(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getNotes(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
