package com.streetvendor.order;

import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.order.dto.OrderItemRequest;
import com.streetvendor.order.dto.PlaceOrderRequest;
import com.streetvendor.order.dto.PlaceOrderResponse;
import com.streetvendor.order.dto.PlaceOrderResult;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.order.service.impl.OrderServiceImpl;
import com.streetvendor.vendor.entity.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID userId;
    private Customer testCustomer;
    private Vendor testVendor;
    private MenuItem testMenuItem1;
    private MenuItem testMenuItem2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testCustomer = new Customer(UUID.randomUUID(), userId, "Test Customer", "123456", "Addr", null, null);
        testVendor = new Vendor(UUID.randomUUID(), null, "Test Vendor");

        MenuCategory category = new MenuCategory(UUID.randomUUID(), testVendor, "Cat", 1);
        testMenuItem1 = new MenuItem(UUID.randomUUID(), category, testVendor, "Item 1", new BigDecimal("100.00"));
        testMenuItem1.setAvailable(true);

        testMenuItem2 = new MenuItem(UUID.randomUUID(), category, testVendor, "Item 2", new BigDecimal("50.00"));
        testMenuItem2.setAvailable(true);
    }

    @Test
    void placeOrder_shouldSucceedAndSnapshotPrices() {
        // Arrange
        String idempotencyKey = "key-123";
        OrderItemRequest itemReq1 = new OrderItemRequest(testMenuItem1.getId(), 2);
        OrderItemRequest itemReq2 = new OrderItemRequest(testMenuItem2.getId(), 3);
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(itemReq1, itemReq2), "Spicy please");

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, testCustomer.getId())).thenReturn(Optional.empty());
        when(menuItemRepository.findAllById(any())).thenReturn(List.of(testMenuItem1, testMenuItem2));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            return order;
        });

        // Act
        PlaceOrderResult result = orderService.placeOrder(userId, idempotencyKey, request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isDuplicate());
        PlaceOrderResponse response = result.response();
        assertNotNull(response.orderId());
        assertEquals(OrderStatus.PLACED, response.status());
        // Expected total = 100.00 * 2 + 50.00 * 3 = 350.00
        assertEquals(new BigDecimal("350.00"), response.totalAmount());
    }

    @Test
    void placeOrder_shouldReturnDuplicateOrderForSameCustomer() {
        // Arrange
        String idempotencyKey = "key-123";
        PlaceOrderRequest request = new PlaceOrderRequest(Collections.emptyList(), null);
        Order mockOrder = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("350.00"), idempotencyKey);

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, testCustomer.getId())).thenReturn(Optional.of(mockOrder));

        // Act
        PlaceOrderResult result = orderService.placeOrder(userId, idempotencyKey, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isDuplicate());
        assertEquals(mockOrder.getId(), result.response().orderId());
        assertEquals(mockOrder.getTotalAmount(), result.response().totalAmount());
    }

    @Test
    void placeOrder_shouldAllowSameIdempotencyKeyForDifferentCustomer() {
        // Arrange
        String idempotencyKey = "key-123";
        OrderItemRequest itemReq = new OrderItemRequest(testMenuItem1.getId(), 2);
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(itemReq), null);

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        // Database find returns empty for this customer + key combo
        when(orderRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, testCustomer.getId())).thenReturn(Optional.empty());
        when(menuItemRepository.findAllById(any())).thenReturn(List.of(testMenuItem1));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PlaceOrderResult result = orderService.placeOrder(userId, idempotencyKey, request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isDuplicate());
    }

    @Test
    void placeOrder_shouldThrowIllegalArgumentExceptionIfMenuItemNotFound() {
        // Arrange
        String idempotencyKey = "key-123";
        OrderItemRequest itemReq = new OrderItemRequest(UUID.randomUUID(), 2);
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(itemReq), null);

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, testCustomer.getId())).thenReturn(Optional.empty());
        when(menuItemRepository.findAllById(any())).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            orderService.placeOrder(userId, idempotencyKey, request)
        );
    }

    @Test
    void placeOrder_shouldThrowIllegalArgumentExceptionIfMenuItemUnavailable() {
        // Arrange
        String idempotencyKey = "key-123";
        testMenuItem1.setAvailable(false);
        OrderItemRequest itemReq = new OrderItemRequest(testMenuItem1.getId(), 2);
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(itemReq), null);

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, testCustomer.getId())).thenReturn(Optional.empty());
        when(menuItemRepository.findAllById(any())).thenReturn(List.of(testMenuItem1));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            orderService.placeOrder(userId, idempotencyKey, request)
        );
    }

    @Test
    void placeOrder_shouldThrowIllegalArgumentExceptionIfMultipleVendors() {
        // Arrange
        String idempotencyKey = "key-123";
        Vendor otherVendor = new Vendor(UUID.randomUUID(), null, "Other Vendor");
        MenuCategory otherCategory = new MenuCategory(UUID.randomUUID(), otherVendor, "Other Cat", 1);
        MenuItem otherItem = new MenuItem(UUID.randomUUID(), otherCategory, otherVendor, "Other Item", new BigDecimal("10.00"));
        otherItem.setAvailable(true);

        OrderItemRequest itemReq1 = new OrderItemRequest(testMenuItem1.getId(), 2);
        OrderItemRequest itemReq2 = new OrderItemRequest(otherItem.getId(), 1);
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(itemReq1, itemReq2), null);

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, testCustomer.getId())).thenReturn(Optional.empty());
        when(menuItemRepository.findAllById(any())).thenReturn(List.of(testMenuItem1, otherItem));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            orderService.placeOrder(userId, idempotencyKey, request)
        );
    }

    @Test
    void placeOrder_shouldAggregateDuplicateMenuItemIds() {
        // Arrange
        String idempotencyKey = "key-123";
        // Two requests for testMenuItem1: qty 2 and qty 3
        OrderItemRequest itemReq1 = new OrderItemRequest(testMenuItem1.getId(), 2);
        OrderItemRequest itemReq2 = new OrderItemRequest(testMenuItem1.getId(), 3);
        PlaceOrderRequest request = new PlaceOrderRequest(List.of(itemReq1, itemReq2), null);

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, testCustomer.getId())).thenReturn(Optional.empty());
        when(menuItemRepository.findAllById(any())).thenReturn(List.of(testMenuItem1));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            return order;
        });

        // Act
        PlaceOrderResult result = orderService.placeOrder(userId, idempotencyKey, request);

        // Assert
        assertNotNull(result);
        // Total should be: 100.00 * (2 + 3) = 500.00
        assertEquals(new BigDecimal("500.00"), result.response().totalAmount());
    }

    @Test
    void placeOrder_shouldThrowUnauthorizedExceptionIfCustomerProfileMissing() {
        // Arrange
        String idempotencyKey = "key-123";
        PlaceOrderRequest request = new PlaceOrderRequest(Collections.emptyList(), null);

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () ->
            orderService.placeOrder(userId, idempotencyKey, request)
        );
    }
}
