package com.streetvendor.order;

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
import com.streetvendor.order.enums.PaymentStatus;
import com.streetvendor.order.exception.OrderCancellationNotAllowedException;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.order.service.impl.OrderCancellationServiceImpl;
import com.streetvendor.vendor.entity.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditService auditService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OrderCancellationServiceImpl orderCancellationService;

    private UUID userId;
    private Customer testCustomer;
    private Vendor testVendor;
    private UUID orderId;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testCustomer = new Customer(UUID.randomUUID(), userId, "Customer A", "123", "Addr", null, null);
        testVendor = new Vendor(UUID.randomUUID(), null, "Street Food Vendor");
        orderId = UUID.randomUUID();
        testOrder = new Order(orderId, testCustomer, testVendor, new BigDecimal("100.00"), "key-abc");
    }

    @Test
    void cancelOrder_shouldSucceedForPlacedOrder() throws JsonProcessingException {
        // Arrange
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CancelOrderResponse response = orderCancellationService.cancelOrder(orderId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(orderId, response.orderId());
        assertEquals(OrderStatus.CANCELLED, response.status());
        assertEquals(PaymentStatus.PENDING, response.paymentStatus());
        assertEquals(new BigDecimal("100.00"), response.totalAmount());

        // Verify audit log payload
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).logEvent(
                eq(AuditEventType.ORDER_CANCELLED_BY_CUSTOMER),
                eq(testVendor.getId()),
                eq(userId),
                detailsCaptor.capture()
        );

        String details = detailsCaptor.getValue();
        assertNotNull(details);
        // Assert JSON values parsed successfully
        var jsonNode = objectMapper.readTree(details);
        assertEquals(orderId.toString(), jsonNode.get("orderId").asText());
        assertEquals(testCustomer.getId().toString(), jsonNode.get("customerId").asText());
        assertEquals("PLACED", jsonNode.get("fromStatus").asText());
        assertEquals("CANCELLED", jsonNode.get("toStatus").asText());
    }

    @Test
    void cancelOrder_shouldFailWhenCustomerProfileNotFound() {
        // Arrange
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () ->
                orderCancellationService.cancelOrder(orderId, userId)
        );
        verify(orderRepository, never()).findById(any(UUID.class));
    }

    @Test
    void cancelOrder_shouldFailWhenOrderNotFound() {
        // Arrange
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                orderCancellationService.cancelOrder(orderId, userId)
        );
    }

    @Test
    void cancelOrder_shouldFailWhenCustomerDoesNotOwnOrder() {
        // Arrange
        Customer otherCustomer = new Customer(UUID.randomUUID(), UUID.randomUUID(), "Other Customer", "999", "Addr", null, null);
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(otherCustomer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(ForbiddenException.class, () ->
                orderCancellationService.cancelOrder(orderId, userId)
        );
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
    }

    @Test
    void cancelOrder_shouldFailWhenStatusNotPlaced() {
        // Arrange
        testOrder.setStatus(OrderStatus.ACCEPTED);
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(OrderCancellationNotAllowedException.class, () ->
                orderCancellationService.cancelOrder(orderId, userId)
        );
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
    }

    @Test
    void cancelOrder_shouldPreventDoubleCancellation() {
        // Arrange
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act - Call 1
        CancelOrderResponse response = orderCancellationService.cancelOrder(orderId, userId);
        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.status());

        // Act & Assert - Call 2
        // Since state is mutated to CANCELLED, next call checks cancellation logic and throws
        assertThrows(OrderCancellationNotAllowedException.class, () ->
                orderCancellationService.cancelOrder(orderId, userId)
        );

        // Verify audit logged exactly once
        verify(auditService, times(1)).logEvent(
                eq(AuditEventType.ORDER_CANCELLED_BY_CUSTOMER),
                eq(testVendor.getId()),
                eq(userId),
                any(String.class)
        );
    }
}
