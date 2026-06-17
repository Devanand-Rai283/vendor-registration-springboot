package com.streetvendor.order.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.common.audit.AuditService;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.ResourceNotFoundException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.order.dto.CancelOrderResponse;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.exception.OrderCancellationNotAllowedException;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.order.service.OrderCancellationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class OrderCancellationServiceImpl implements OrderCancellationService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public OrderCancellationServiceImpl(OrderRepository orderRepository,
                                        CustomerRepository customerRepository,
                                        AuditService auditService,
                                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public CancelOrderResponse cancelOrder(UUID orderId, UUID userId) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException("Customer profile not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("You do not own this order");
        }

        OrderStatus fromStatus = order.getStatus();
        if (fromStatus != OrderStatus.PLACED) {
            throw new OrderCancellationNotAllowedException("Cannot cancel order in status: " + fromStatus);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.saveAndFlush(order);

        String details;
        try {
            Map<String, Object> payload = Map.of(
                    "orderId", orderId.toString(),
                    "customerId", customer.getId().toString(),
                    "fromStatus", fromStatus.name(),
                    "toStatus", OrderStatus.CANCELLED.name()
            );
            details = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize audit log details payload", e);
        }

        auditService.logEvent(AuditEventType.ORDER_CANCELLED_BY_CUSTOMER, savedOrder.getVendor().getId(), userId, details);

        return new CancelOrderResponse(
                savedOrder.getId(),
                savedOrder.getStatus(),
                savedOrder.getPaymentStatus(),
                savedOrder.getTotalAmount(),
                savedOrder.getUpdatedAt()
        );
    }
}
