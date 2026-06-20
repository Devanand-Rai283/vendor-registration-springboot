package com.streetvendor.vendor.service;

import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.vendor.dto.VendorDashboardMetricsResponseDto;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class VendorDashboardServiceImpl implements VendorDashboardService {

    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;

    public VendorDashboardServiceImpl(OrderRepository orderRepository, VendorRepository vendorRepository) {
        this.orderRepository = orderRepository;
        this.vendorRepository = vendorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public VendorDashboardMetricsResponseDto getDashboardMetrics(UUID vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        long totalOrders = orderRepository.countByVendorId(vendorId);
        
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PLACED,
                OrderStatus.ACCEPTED,
                OrderStatus.PREPARING,
                OrderStatus.READY
        );
        long activeOrders = orderRepository.countByVendorIdAndStatusIn(vendorId, activeStatuses);
        
        BigDecimal totalRevenue = orderRepository.sumRevenueByVendorId(vendorId);
        BigDecimal averageOrderValue = orderRepository.averageOrderValueByVendorId(vendorId);

        return new VendorDashboardMetricsResponseDto(
                (int) activeOrders,
                totalOrders,
                totalRevenue,
                averageOrderValue,
                vendor.getAverageRating(),
                vendor.getTotalReviews()
        );
    }
}
