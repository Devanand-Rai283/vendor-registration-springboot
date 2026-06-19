package com.streetvendor.unit;

import com.streetvendor.analytics.dto.AnalyticsSnapshotCacheDto;
import com.streetvendor.analytics.repository.AnalyticsSnapshotRepository;
import com.streetvendor.analytics.service.impl.AnalyticsServiceImpl;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.entity.OrderItem;
import com.streetvendor.order.repository.OrderItemRepository;
import com.streetvendor.payment.entity.Payment;
import com.streetvendor.payment.repository.PaymentRepository;
import com.streetvendor.vendor.entity.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceUnitTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private AnalyticsSnapshotRepository analyticsSnapshotRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private Vendor vendorA;
    private Vendor vendorB;
    private LocalDate targetDate;
    private Instant targetDateStart;
    private Instant targetDateEnd;

    @BeforeEach
    void setUp() {
        vendorA = new Vendor(UUID.randomUUID(), null, "Vendor A");
        vendorB = new Vendor(UUID.randomUUID(), null, "Vendor B");
        targetDate = LocalDate.of(2026, 6, 19);
        targetDateStart = targetDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        targetDateEnd = targetDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    @Test
    void shouldGenerateSnapshotsForVendorsWithPaidOrders() {
        // Arrange
        Order order = new Order(UUID.randomUUID(), null, vendorA, BigDecimal.valueOf(250), "idemp1");
        ReflectionTestUtils.setField(order, "createdAt", targetDateStart.plusSeconds(3600 * 12)); // 12:00 UTC
        Payment payment = new Payment(UUID.randomUUID(), order, "razorpay_order_1", 25000); // 25000 paise = 250 Rupees
        payment.setStatus(com.streetvendor.payment.enums.PaymentStatus.PAID);

        MenuItem item = new MenuItem(UUID.randomUUID(), null, vendorA, "Samosa", BigDecimal.valueOf(50));
        OrderItem orderItem = new OrderItem(UUID.randomUUID(), order, item, 5, BigDecimal.valueOf(50));
        order.addItem(orderItem);

        when(paymentRepository.findPaidCompletedPaymentsForOrdersCreatedBetween(targetDateStart, targetDateEnd))
                .thenReturn(List.of(payment));
        when(orderItemRepository.findByOrderIdIn(List.of(order.getId())))
                .thenReturn(List.of(orderItem));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        analyticsService.generateSnapshots(targetDate);

        // Assert
        verify(analyticsSnapshotRepository).upsertSnapshot(
                any(UUID.class),
                eq(vendorA.getId()),
                eq(targetDate),
                eq(1),
                eq(BigDecimal.valueOf(250.00).setScale(2)),
                eq(BigDecimal.valueOf(250.00).setScale(2)),
                eq(item.getId()),
                eq(12)
        );
        verify(redisTemplate.opsForValue()).set(eq("analytics:" + vendorA.getId()), anyList(), any());
    }

    @Test
    void shouldNotGenerateSnapshotsWhenNoPaidOrders() {
        // Arrange
        when(paymentRepository.findPaidCompletedPaymentsForOrdersCreatedBetween(targetDateStart, targetDateEnd))
                .thenReturn(Collections.emptyList());

        // Act
        analyticsService.generateSnapshots(targetDate);

        // Assert
        verify(analyticsSnapshotRepository, never()).upsertSnapshot(any(), any(), any(), anyInt(), any(), any(), any(), anyInt());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldResolveTopItemTieUsingUuidAscending() {
        // Arrange
        Order order = new Order(UUID.randomUUID(), null, vendorA, BigDecimal.valueOf(100), "idemp1");
        ReflectionTestUtils.setField(order, "createdAt", targetDateStart.plusSeconds(3600 * 10)); // 10:00 UTC
        Payment payment = new Payment(UUID.randomUUID(), order, "razor_order_1", 10000);
        payment.setStatus(com.streetvendor.payment.enums.PaymentStatus.PAID);

        // Create item1 and item2, determine which has the smaller UUID
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID smallerUuid = uuid1.compareTo(uuid2) < 0 ? uuid1 : uuid2;
        UUID largerUuid = uuid1.compareTo(uuid2) < 0 ? uuid2 : uuid1;

        MenuItem item1 = new MenuItem(smallerUuid, null, vendorA, "Water", BigDecimal.valueOf(50));
        MenuItem item2 = new MenuItem(largerUuid, null, vendorA, "Tea", BigDecimal.valueOf(50));

        OrderItem orderItem1 = new OrderItem(UUID.randomUUID(), order, item1, 2, BigDecimal.valueOf(50));
        OrderItem orderItem2 = new OrderItem(UUID.randomUUID(), order, item2, 2, BigDecimal.valueOf(50));
        order.addItem(orderItem1);
        order.addItem(orderItem2);

        when(paymentRepository.findPaidCompletedPaymentsForOrdersCreatedBetween(targetDateStart, targetDateEnd))
                .thenReturn(List.of(payment));
        when(orderItemRepository.findByOrderIdIn(List.of(order.getId())))
                .thenReturn(List.of(orderItem1, orderItem2));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        analyticsService.generateSnapshots(targetDate);

        // Assert
        // Tie breaker should select the item with the smaller UUID (smallerUuid)
        verify(analyticsSnapshotRepository).upsertSnapshot(
                any(UUID.class),
                eq(vendorA.getId()),
                eq(targetDate),
                eq(1),
                eq(BigDecimal.valueOf(100.00).setScale(2)),
                eq(BigDecimal.valueOf(100.00).setScale(2)),
                eq(smallerUuid),
                eq(10)
        );
    }

    @Test
    void shouldResolvePeakHourTieUsingEarlierHour() {
        // Arrange
        // We have two separate orders: one at 08:00 and one at 18:00
        Order order1 = new Order(UUID.randomUUID(), null, vendorA, BigDecimal.valueOf(50), "idemp1");
        ReflectionTestUtils.setField(order1, "createdAt", targetDateStart.plusSeconds(3600 * 8)); // 08:00 UTC
        Payment payment1 = new Payment(UUID.randomUUID(), order1, "razor_order_1", 5000);
        payment1.setStatus(com.streetvendor.payment.enums.PaymentStatus.PAID);

        Order order2 = new Order(UUID.randomUUID(), null, vendorA, BigDecimal.valueOf(50), "idemp2");
        ReflectionTestUtils.setField(order2, "createdAt", targetDateStart.plusSeconds(3600 * 18)); // 18:00 UTC
        Payment payment2 = new Payment(UUID.randomUUID(), order2, "razor_order_2", 5000);
        payment2.setStatus(com.streetvendor.payment.enums.PaymentStatus.PAID);

        MenuItem item = new MenuItem(UUID.randomUUID(), null, vendorA, "Water", BigDecimal.valueOf(50));
        OrderItem orderItem1 = new OrderItem(UUID.randomUUID(), order1, item, 1, BigDecimal.valueOf(50));
        OrderItem orderItem2 = new OrderItem(UUID.randomUUID(), order2, item, 1, BigDecimal.valueOf(50));
        order1.addItem(orderItem1);
        order2.addItem(orderItem2);

        when(paymentRepository.findPaidCompletedPaymentsForOrdersCreatedBetween(targetDateStart, targetDateEnd))
                .thenReturn(List.of(payment1, payment2));
        when(orderItemRepository.findByOrderIdIn(anyList()))
                .thenReturn(List.of(orderItem1, orderItem2));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        analyticsService.generateSnapshots(targetDate);

        // Assert
        // Tie breaker should select the earlier hour (8)
        verify(analyticsSnapshotRepository).upsertSnapshot(
                any(UUID.class),
                eq(vendorA.getId()),
                eq(targetDate),
                eq(2),
                eq(BigDecimal.valueOf(100.00).setScale(2)),
                eq(BigDecimal.valueOf(50.00).setScale(2)),
                eq(item.getId()),
                eq(8)
        );
    }

    @Test
    void shouldHandleMultipleVendorsSeparately() {
        // Arrange
        // Order for Vendor A
        Order orderA = new Order(UUID.randomUUID(), null, vendorA, BigDecimal.valueOf(150), "idempA");
        ReflectionTestUtils.setField(orderA, "createdAt", targetDateStart.plusSeconds(3600 * 9));
        Payment paymentA = new Payment(UUID.randomUUID(), orderA, "razor_order_A", 15000);
        paymentA.setStatus(com.streetvendor.payment.enums.PaymentStatus.PAID);

        MenuItem itemA = new MenuItem(UUID.randomUUID(), null, vendorA, "Water", BigDecimal.valueOf(50));
        OrderItem orderItemA = new OrderItem(UUID.randomUUID(), orderA, itemA, 3, BigDecimal.valueOf(50));
        orderA.addItem(orderItemA);

        // Order for Vendor B
        Order orderB = new Order(UUID.randomUUID(), null, vendorB, BigDecimal.valueOf(80), "idempB");
        ReflectionTestUtils.setField(orderB, "createdAt", targetDateStart.plusSeconds(3600 * 14));
        Payment paymentB = new Payment(UUID.randomUUID(), orderB, "razor_order_B", 8000);
        paymentB.setStatus(com.streetvendor.payment.enums.PaymentStatus.PAID);

        MenuItem itemB = new MenuItem(UUID.randomUUID(), null, vendorB, "Tea", BigDecimal.valueOf(40));
        OrderItem orderItemB = new OrderItem(UUID.randomUUID(), orderB, itemB, 2, BigDecimal.valueOf(40));
        orderB.addItem(orderItemB);

        when(paymentRepository.findPaidCompletedPaymentsForOrdersCreatedBetween(targetDateStart, targetDateEnd))
                .thenReturn(List.of(paymentA, paymentB));
        when(orderItemRepository.findByOrderIdIn(List.of(orderA.getId())))
                .thenReturn(List.of(orderItemA));
        when(orderItemRepository.findByOrderIdIn(List.of(orderB.getId())))
                .thenReturn(List.of(orderItemB));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        analyticsService.generateSnapshots(targetDate);

        // Assert
        verify(analyticsSnapshotRepository).upsertSnapshot(
                any(UUID.class), eq(vendorA.getId()), eq(targetDate), eq(1),
                eq(BigDecimal.valueOf(150.00).setScale(2)), eq(BigDecimal.valueOf(150.00).setScale(2)),
                eq(itemA.getId()), eq(9)
        );

        verify(analyticsSnapshotRepository).upsertSnapshot(
                any(UUID.class), eq(vendorB.getId()), eq(targetDate), eq(1),
                eq(BigDecimal.valueOf(80.00).setScale(2)), eq(BigDecimal.valueOf(80.00).setScale(2)),
                eq(itemB.getId()), eq(14)
        );
    }
}
