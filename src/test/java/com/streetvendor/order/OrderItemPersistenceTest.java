package com.streetvendor.order;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.auth.repository.UserRepository;
import com.streetvendor.customer.entity.Customer;
import com.streetvendor.customer.repository.CustomerRepository;
import com.streetvendor.menu.entity.MenuCategory;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuCategoryRepository;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.entity.OrderItem;
import com.streetvendor.order.repository.OrderItemRepository;
import com.streetvendor.order.repository.OrderRepository;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("order-test")
@Transactional
class OrderItemPersistenceTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    private Order testOrder;
    private MenuItem testMenuItem;

    @BeforeEach
    void setUp() {
        User customerUser = new User(UUID.randomUUID(), "customer@example.com", "hashedPassword", Role.CUSTOMER, AccountStatus.ACTIVE);
        userRepository.save(customerUser);

        Customer testCustomer = new Customer(UUID.randomUUID(), customerUser.getId(), "Test Customer", "1234567890", "123 Main St", null, null);
        customerRepository.save(testCustomer);

        User vendorUser = new User(UUID.randomUUID(), "vendor@example.com", "hashedPassword", Role.VENDOR, AccountStatus.ACTIVE);
        userRepository.save(vendorUser);

        Vendor testVendor = new Vendor(UUID.randomUUID(), vendorUser, "Test Vendor Business");
        vendorRepository.save(testVendor);

        MenuCategory category = new MenuCategory(UUID.randomUUID(), testVendor, "Main Course", 1);
        menuCategoryRepository.save(category);

        testMenuItem = new MenuItem(UUID.randomUUID(), category, testVendor, "Butter Chicken", new BigDecimal("250.00"));
        menuItemRepository.save(testMenuItem);

        testOrder = new Order(UUID.randomUUID(), testCustomer, testVendor, new BigDecimal("500.00"), UUID.randomUUID().toString());
        orderRepository.save(testOrder);
        orderRepository.flush();
    }

    @Test
    void shouldSaveAndRetrieveOrderItem() {
        UUID itemId = UUID.randomUUID();
        OrderItem item = new OrderItem(itemId, testOrder, testMenuItem, 2, new BigDecimal("250.00"));

        orderItemRepository.save(item);
        orderItemRepository.flush();

        Optional<OrderItem> found = orderItemRepository.findById(itemId);

        assertTrue(found.isPresent());
        assertEquals(itemId, found.get().getId());
        assertEquals(testOrder.getId(), found.get().getOrder().getId());
        assertEquals(testMenuItem.getId(), found.get().getMenuItem().getId());
        assertEquals(2, found.get().getQuantity());
        assertEquals(new BigDecimal("250.00"), found.get().getUnitPrice());
        assertEquals(new BigDecimal("500.00"), found.get().getSubtotal());
    }

    @Test
    void shouldCalculateSubtotalInConstructor() {
        OrderItem item = new OrderItem(UUID.randomUUID(), testOrder, testMenuItem, 3, new BigDecimal("100.00"));

        assertEquals(new BigDecimal("300.00"), item.getSubtotal());
    }

    @Test
    void shouldFindByOrderId() {
        OrderItem item1 = new OrderItem(UUID.randomUUID(), testOrder, testMenuItem, 1, new BigDecimal("250.00"));
        OrderItem item2 = new OrderItem(UUID.randomUUID(), testOrder, testMenuItem, 2, new BigDecimal("250.00"));
        orderItemRepository.save(item1);
        orderItemRepository.save(item2);
        orderItemRepository.flush();

        List<OrderItem> found = orderItemRepository.findByOrderId(testOrder.getId());

        assertEquals(2, found.size());
    }

    @Test
    void shouldReturnEmptyListForNonExistentOrderId() {
        List<OrderItem> found = orderItemRepository.findByOrderId(UUID.randomUUID());

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldPersistUnitPriceAsSnapshot() {
        BigDecimal snapshotPrice = new BigDecimal("199.99");
        OrderItem item = new OrderItem(UUID.randomUUID(), testOrder, testMenuItem, 1, snapshotPrice);
        orderItemRepository.save(item);
        orderItemRepository.flush();

        Optional<OrderItem> found = orderItemRepository.findById(item.getId());
        assertTrue(found.isPresent());
        assertEquals(snapshotPrice, found.get().getUnitPrice());
    }

    @Test
    void shouldMaintainRelationshipToMenuItem() {
        OrderItem item = new OrderItem(UUID.randomUUID(), testOrder, testMenuItem, 1, new BigDecimal("250.00"));
        orderItemRepository.save(item);
        orderItemRepository.flush();

        Optional<OrderItem> found = orderItemRepository.findById(item.getId());
        assertTrue(found.isPresent());
        assertNotNull(found.get().getMenuItem());
        assertEquals(testMenuItem.getId(), found.get().getMenuItem().getId());
    }

    @Test
    void shouldCascadeDeleteItemsWhenOrderDeleted() {
        OrderItem item = new OrderItem(UUID.randomUUID(), testOrder, testMenuItem, 1, new BigDecimal("250.00"));
        testOrder.addItem(item);
        orderRepository.save(testOrder);
        orderRepository.flush();

        UUID itemId = item.getId();
        assertNotNull(orderItemRepository.findById(itemId).orElse(null));

        orderRepository.delete(testOrder);
        orderRepository.flush();

        assertFalse(orderItemRepository.findById(itemId).isPresent());
    }

    @Test
    void shouldSaveMultipleItemsForSameOrder() {
        MenuItem menuItem2 = new MenuItem(UUID.randomUUID(), testMenuItem.getCategory(), testMenuItem.getVendor(), "Naan", new BigDecimal("50.00"));
        menuItemRepository.save(menuItem2);

        OrderItem item1 = new OrderItem(UUID.randomUUID(), testOrder, testMenuItem, 2, new BigDecimal("250.00"));
        OrderItem item2 = new OrderItem(UUID.randomUUID(), testOrder, menuItem2, 4, new BigDecimal("50.00"));
        orderItemRepository.save(item1);
        orderItemRepository.save(item2);
        orderItemRepository.flush();

        List<OrderItem> found = orderItemRepository.findByOrderId(testOrder.getId());

        assertEquals(2, found.size());
    }
}
