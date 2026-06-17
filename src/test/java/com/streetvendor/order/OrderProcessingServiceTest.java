package com.streetvendor.order;

import com.streetvendor.common.audit.AuditEventType;
import com.streetvendor.common.audit.AuditService;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.order.dto.OrderResponse;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.enums.PaymentStatus;
import com.streetvendor.order.exception.InvalidOrderStatusTransitionException;
import com.streetvendor.order.exception.OrderAlreadyFinalizedException;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.order.service.impl.OrderProcessingServiceImpl;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private OrderProcessingServiceImpl orderProcessingService;

    private UUID userId;
    private Vendor testVendor;
    private Customer testCustomer;
    private UUID orderId;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testVendor = new Vendor(UUID.randomUUID(), null, "Business");
        testCustomer = new Customer(UUID.randomUUID(), UUID.randomUUID(), "Customer", "123", "Addr", null, null);
        orderId = UUID.randomUUID();
        testOrder = new Order(orderId, testCustomer, testVendor, new BigDecimal("100.00"), "key-123");
    }

    @Test
    void updateStatus_shouldSucceedForValidTransitionAndWriteAudit() {
        // Arrange
        OrderStatus newStatus = OrderStatus.ACCEPTED;
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(testVendor));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse response = orderProcessingService.updateStatus(orderId, newStatus, userId);

        // Assert
        assertNotNull(response);
        assertEquals(newStatus, response.status());
        verify(auditService).logEvent(
                eq(AuditEventType.ORDER_ACCEPTED),
                eq(testVendor.getId()),
                eq(userId),
                contains("\"fromStatus\":\"PLACED\"")
        );
    }

    @Test
    void updateStatus_shouldThrowOrderAlreadyFinalizedExceptionWhenFinalized() {
        // Arrange
        testOrder.setStatus(OrderStatus.COMPLETED);
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(testVendor));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(OrderAlreadyFinalizedException.class, () ->
                orderProcessingService.updateStatus(orderId, OrderStatus.PREPARING, userId)
        );
    }

    @Test
    void updateStatus_shouldThrowInvalidOrderStatusTransitionExceptionWhenInvalid() {
        // Arrange
        // Transition PLACED -> PREPARING is invalid (must be ACCEPTED first)
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(testVendor));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(InvalidOrderStatusTransitionException.class, () ->
                orderProcessingService.updateStatus(orderId, OrderStatus.PREPARING, userId)
        );
    }

    @Test
    void updateStatus_shouldThrowForbiddenExceptionWhenVendorDoesNotOwnOrder() {
        // Arrange
        Vendor otherVendor = new Vendor(UUID.randomUUID(), null, "Other Business");
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(otherVendor));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(ForbiddenException.class, () ->
                orderProcessingService.updateStatus(orderId, OrderStatus.ACCEPTED, userId)
        );
    }
}
