package com.streetvendor.order;

import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.order.dto.CustomerOrderHistoryResponse;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.enums.PaymentStatus;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.order.service.impl.OrderHistoryServiceImpl;
import com.streetvendor.vendor.entity.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderHistoryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderHistoryServiceImpl orderHistoryService;

    private UUID userId;
    private Customer testCustomer;
    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testCustomer = new Customer(UUID.randomUUID(), userId, "Test Customer", "12345", "Address", null, null);
        testVendor = new Vendor(UUID.randomUUID(), null, "Tasty Vendor");
    }

    @Test
    void getOrderHistory_shouldSucceedAndReturnSortedPage() {
        // Arrange
        int page = 0;
        int size = 10;
        UUID orderId = UUID.randomUUID();
        Order testOrder = new Order(orderId, testCustomer, testVendor, new BigDecimal("150.00"), "idemp-key");

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));

        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(orderRepository.findByCustomerId(eq(testCustomer.getId()), pageableCaptor.capture())).thenReturn(orderPage);

        // Act
        Page<CustomerOrderHistoryResponse> result = orderHistoryService.getOrderHistory(userId, page, size);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        CustomerOrderHistoryResponse dto = result.getContent().get(0);
        assertEquals(orderId, dto.orderId());
        assertEquals(testVendor.getId(), dto.vendorId());
        assertEquals("Tasty Vendor", dto.vendorBusinessName());
        assertEquals(OrderStatus.PLACED, dto.status());
        assertEquals(PaymentStatus.PENDING, dto.paymentStatus());
        assertEquals(new BigDecimal("150.00"), dto.totalAmount());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertNotNull(capturedPageable);
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(10, capturedPageable.getPageSize());

        Sort sort = capturedPageable.getSort();
        assertNotNull(sort);
        Sort.Order createdAtOrder = sort.getOrderFor("createdAt");
        assertNotNull(createdAtOrder);
        assertTrue(createdAtOrder.isDescending());
    }

    @Test
    void getOrderHistory_shouldReturnEmptyPageWhenNoOrdersExist() {
        // Arrange
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findByCustomerId(eq(testCustomer.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        Page<CustomerOrderHistoryResponse> result = orderHistoryService.getOrderHistory(userId, 0, 20);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getOrderHistory_shouldThrowUnauthorizedExceptionWhenCustomerProfileNotFound() {
        // Arrange
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
                orderHistoryService.getOrderHistory(userId, 0, 20)
        );
        assertEquals("Customer profile not found", exception.getMessage());
        verify(orderRepository, never()).findByCustomerId(any(UUID.class), any(Pageable.class));
    }

    @Test
    void getOrderHistory_shouldThrowIllegalArgumentExceptionWhenPageNegative() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderHistoryService.getOrderHistory(userId, -1, 20)
        );
        assertEquals("Page index must not be less than zero", exception.getMessage());
    }

    @Test
    void getOrderHistory_shouldThrowIllegalArgumentExceptionWhenSizeZeroOrNegative() {
        // Act & Assert
        IllegalArgumentException exceptionZero = assertThrows(IllegalArgumentException.class, () ->
                orderHistoryService.getOrderHistory(userId, 0, 0)
        );
        assertEquals("Page size must be greater than zero", exceptionZero.getMessage());

        IllegalArgumentException exceptionNegative = assertThrows(IllegalArgumentException.class, () ->
                orderHistoryService.getOrderHistory(userId, 0, -5)
        );
        assertEquals("Page size must be greater than zero", exceptionNegative.getMessage());
    }

    @Test
    void getOrderHistory_shouldThrowIllegalArgumentExceptionWhenSizeExceedsLimit() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderHistoryService.getOrderHistory(userId, 0, 101)
        );
        assertEquals("Page size must not exceed 100", exception.getMessage());
    }
}
