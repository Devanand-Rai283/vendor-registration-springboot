package com.streetvendor.order.service.impl;

import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.common.audit.AuditService;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.order.dto.OrderResponse;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.exception.InvalidOrderStatusTransitionException;
import com.streetvendor.order.exception.OrderAlreadyFinalizedException;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.order.service.OrderProcessingService;
import com.streetvendor.order.validation.OrderStatusTransitionValidator;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderProcessingServiceImpl implements OrderProcessingService {

    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;
    private final AuditService auditService;

    public OrderProcessingServiceImpl(OrderRepository orderRepository,
                                      VendorRepository vendorRepository,
                                      AuditService auditService) {
        this.orderRepository = orderRepository;
        this.vendorRepository = vendorRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus newStatus, UUID userId) {
        // 1. Load authenticated vendor
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Only vendors can process orders"));

        // 2. Load order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // 3. Verify vendor ownership
        if (!order.getVendor().getId().equals(vendor.getId())) {
            throw new ForbiddenException("You do not own this order");
        }

        OrderStatus fromStatus = order.getStatus();

        // 4. Check if finalized
        if (fromStatus == OrderStatus.COMPLETED || fromStatus == OrderStatus.CANCELLED) {
            throw new OrderAlreadyFinalizedException("Cannot transition from finalized status: " + fromStatus);
        }

        // 5. Validate transition using validation engine
        if (!OrderStatusTransitionValidator.canTransition(fromStatus, newStatus)) {
            throw new InvalidOrderStatusTransitionException("Invalid transition from " + fromStatus + " to " + newStatus);
        }

        // 6. Update status and save
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.saveAndFlush(order);

        // 7. Write enhanced JSON audit log
        AuditEventType auditEventType = getAuditEventTypeForStatus(newStatus);
        String details = String.format(
                "{\"orderId\":\"%s\",\"vendorId\":\"%s\",\"fromStatus\":\"%s\",\"toStatus\":\"%s\"}",
                orderId,
                vendor.getId(),
                fromStatus,
                newStatus
        );
        auditService.logEvent(auditEventType, savedOrder.getVendor().getId(), userId, details);

        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getStatus(),
                savedOrder.getPaymentStatus(),
                savedOrder.getTotalAmount(),
                savedOrder.getCustomer().getId(),
                savedOrder.getVendor().getId(),
                savedOrder.getCreatedAt(),
                savedOrder.getUpdatedAt()
        );
    }

    private AuditEventType getAuditEventTypeForStatus(OrderStatus status) {
        return switch (status) {
            case ACCEPTED -> AuditEventType.ORDER_ACCEPTED;
            case CANCELLED -> AuditEventType.ORDER_CANCELLED;
            case PREPARING -> AuditEventType.ORDER_PREPARING;
            case READY -> AuditEventType.ORDER_READY;
            case COMPLETED -> AuditEventType.ORDER_COMPLETED;
            default -> throw new IllegalArgumentException("No audit event for status " + status);
        };
    }
}
