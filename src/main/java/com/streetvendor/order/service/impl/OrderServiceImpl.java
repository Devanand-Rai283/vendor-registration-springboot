package com.streetvendor.order.service.impl;

import com.streetvendor.common.exception.ConflictException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.order.dto.PlaceOrderRequest;
import com.streetvendor.order.dto.PlaceOrderResponse;
import com.streetvendor.order.dto.PlaceOrderResult;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.entity.OrderItem;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.order.service.OrderService;
import com.streetvendor.vendor.entity.Vendor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final MenuItemRepository menuItemRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CustomerRepository customerRepository,
                            MenuItemRepository menuItemRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    @Transactional
    public PlaceOrderResult placeOrder(UUID userId, String idempotencyKey, PlaceOrderRequest request) {
        // 1. Resolve customer profile
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException("Customer profile not found"));

        // 2. Idempotency check scoped by customer
        Optional<Order> existingOrder = orderRepository.findByIdempotencyKeyAndCustomerId(idempotencyKey, customer.getId());
        if (existingOrder.isPresent()) {
            Order order = existingOrder.get();
            PlaceOrderResponse response = new PlaceOrderResponse(
                    order.getId(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    order.getCreatedAt()
            );
            return new PlaceOrderResult(response, true);
        }

        // 3. Aggregate duplicate menu items
        Map<UUID, Integer> aggregatedQuantities = request.items().stream()
                .collect(Collectors.groupingBy(
                        item -> item.menuItemId(),
                        Collectors.summingInt(item -> item.quantity())
                ));

        // 4. Fetch menu items
        List<MenuItem> menuItems = menuItemRepository.findAllById(aggregatedQuantities.keySet());

        // 5. Validate existence
        Map<UUID, MenuItem> menuItemMap = menuItems.stream()
                .collect(Collectors.toMap(MenuItem::getId, item -> item));

        for (UUID requestedId : aggregatedQuantities.keySet()) {
            if (!menuItemMap.containsKey(requestedId)) {
                throw new IllegalArgumentException("Menu item not found: " + requestedId);
            }
        }

        // 6. Validate availability
        for (MenuItem menuItem : menuItems) {
            if (!menuItem.isAvailable()) {
                throw new IllegalArgumentException("Menu item is not available: " + menuItem.getName());
            }
        }

        // 7. Validate single-vendor constraint
        UUID vendorId = menuItems.get(0).getVendor().getId();
        for (MenuItem menuItem : menuItems) {
            if (!menuItem.getVendor().getId().equals(vendorId)) {
                throw new IllegalArgumentException("All items must belong to a single vendor");
            }
        }

        Vendor vendor = menuItems.get(0).getVendor();

        // 8. Construct Order & OrderItems
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, customer, vendor, BigDecimal.ZERO, idempotencyKey);
        order.setNotes(request.notes());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map.Entry<UUID, Integer> entry : aggregatedQuantities.entrySet()) {
            MenuItem menuItem = menuItemMap.get(entry.getKey());
            BigDecimal unitPriceSnapshot = menuItem.getPrice();
            Integer quantity = entry.getValue();

            OrderItem orderItem = new OrderItem(
                    UUID.randomUUID(),
                    order,
                    menuItem,
                    quantity,
                    unitPriceSnapshot
            );
            order.addItem(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());
        }

        order.setTotalAmount(totalAmount);

        // 9. Persist atomically
        Order savedOrder = orderRepository.saveAndFlush(order);

        PlaceOrderResponse response = new PlaceOrderResponse(
                savedOrder.getId(),
                savedOrder.getStatus(),
                savedOrder.getTotalAmount(),
                savedOrder.getCreatedAt()
        );

        return new PlaceOrderResult(response, false);
    }
}
