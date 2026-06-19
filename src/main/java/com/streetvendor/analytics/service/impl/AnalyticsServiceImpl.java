package com.streetvendor.analytics.service.impl;

import com.streetvendor.analytics.dto.AnalyticsResponseDto;
import com.streetvendor.analytics.dto.AnalyticsSnapshotResponseDto;
import com.streetvendor.analytics.dto.AnalyticsSnapshotCacheDto;
import com.streetvendor.analytics.entity.AnalyticsSnapshot;
import com.streetvendor.analytics.repository.AnalyticsSnapshotRepository;
import com.streetvendor.analytics.service.AnalyticsService;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.common.exception.ForbiddenException;
import com.streetvendor.common.exception.UnauthorizedException;
import com.streetvendor.menu.entity.MenuItem;
import com.streetvendor.menu.repository.MenuItemRepository;
import com.streetvendor.order.entity.Order;
import com.streetvendor.order.entity.OrderItem;
import com.streetvendor.order.repository.OrderItemRepository;
import com.streetvendor.payment.entity.Payment;
import com.streetvendor.payment.repository.PaymentRepository;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final AnalyticsSnapshotRepository analyticsSnapshotRepository;
    private final VendorRepository vendorRepository;
    private final MenuItemRepository menuItemRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public AnalyticsServiceImpl(PaymentRepository paymentRepository,
                                OrderItemRepository orderItemRepository,
                                AnalyticsSnapshotRepository analyticsSnapshotRepository,
                                VendorRepository vendorRepository,
                                MenuItemRepository menuItemRepository,
                                RedisTemplate<String, Object> redisTemplate) {
        this.paymentRepository = paymentRepository;
        this.orderItemRepository = orderItemRepository;
        this.analyticsSnapshotRepository = analyticsSnapshotRepository;
        this.vendorRepository = vendorRepository;
        this.menuItemRepository = menuItemRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public void generateSnapshots(LocalDate snapshotDate) {
        // 1. Calculate time boundaries in UTC
        Instant start = snapshotDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = snapshotDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // 2. Fetch paid completed payments and group by Vendor
        List<Payment> payments = paymentRepository.findPaidCompletedPaymentsForOrdersCreatedBetween(start, end);
        Map<Vendor, List<Payment>> paymentsByVendor = payments.stream()
                .collect(Collectors.groupingBy(p -> p.getOrder().getVendor()));

        for (Map.Entry<Vendor, List<Payment>> entry : paymentsByVendor.entrySet()) {
            Vendor vendor = entry.getKey();
            List<Payment> vendorPayments = entry.getValue();

            // 3. Extract distinct paid completed orders
            List<Order> vendorOrders = vendorPayments.stream()
                    .map(Payment::getOrder)
                    .distinct()
                    .collect(Collectors.toList());

            int totalOrders = vendorOrders.size();
            if (totalOrders == 0) {
                continue;
            }

            // 4. Calculate total revenue (converting from paise to Rupees)
            long totalPaise = vendorPayments.stream()
                    .mapToLong(Payment::getAmount)
                    .sum();
            BigDecimal totalRevenue = BigDecimal.valueOf(totalPaise)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // 5. Calculate average order value
            BigDecimal averageOrderValue = totalRevenue
                    .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);

            // 6. Fetch all order items for the vendor's paid orders
            List<UUID> orderIds = vendorOrders.stream()
                    .map(Order::getId)
                    .collect(Collectors.toList());
            List<OrderItem> orderItems = orderItemRepository.findByOrderIdIn(orderIds);

            // 7. Find top menu item ordered (with deterministic tie-breaker)
            Map<UUID, Integer> itemQuantities = orderItems.stream()
                    .collect(Collectors.groupingBy(
                            oi -> oi.getMenuItem().getId(),
                            Collectors.summingInt(OrderItem::getQuantity)
                    ));

            UUID topItemId = itemQuantities.entrySet().stream()
                    .sorted((e1, e2) -> {
                        int compareQty = e2.getValue().compareTo(e1.getValue());
                        if (compareQty != 0) {
                            return compareQty;
                        }
                        return e1.getKey().compareTo(e2.getKey()); // Tie breaker: ascending UUID
                    })
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            // 8. Find peak hour (with deterministic tie-breaker)
            Map<Integer, Long> hourCounts = vendorOrders.stream()
                    .map(o -> o.getCreatedAt().atZone(ZoneOffset.UTC).getHour())
                    .collect(Collectors.groupingBy(h -> h, Collectors.counting()));

            Integer peakHour = hourCounts.entrySet().stream()
                    .sorted((e1, e2) -> {
                        int compareCount = e2.getValue().compareTo(e1.getValue());
                        if (compareCount != 0) {
                            return compareCount;
                        }
                        return e1.getKey().compareTo(e2.getKey()); // Tie breaker: earlier hour wins
                    })
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(0);

            // 9. Perform PostgreSQL atomic upsert
            analyticsSnapshotRepository.upsertSnapshot(
                    UUID.randomUUID(),
                    vendor.getId(),
                    snapshotDate,
                    totalOrders,
                    totalRevenue,
                    averageOrderValue,
                    topItemId,
                    peakHour
            );

            // 10. Query last 90 days and refresh Redis cache
            LocalDate ninetyDaysAgo = snapshotDate.minusDays(90);
            List<AnalyticsSnapshot> snapshots = analyticsSnapshotRepository
                    .findByVendorIdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(vendor.getId(), ninetyDaysAgo);

            List<AnalyticsSnapshotCacheDto> cacheList = snapshots.stream()
                    .map(s -> new AnalyticsSnapshotCacheDto(
                            s.getSnapshotDate(),
                            s.getTotalOrders(),
                            s.getTotalRevenue(),
                            s.getAverageOrderValue(),
                            s.getTopItemId(),
                            s.getPeakHour()
                    ))
                    .collect(Collectors.toList());

            String cacheKey = "analytics:" + vendor.getId();
            redisTemplate.opsForValue().set(cacheKey, cacheList, Duration.ofSeconds(900));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsResponseDto getVendorAnalytics(UUID vendorId, int days) {
        // 1. Ownership & Role Validation
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new UnauthorizedException("Invalid authentication principal");
        }

        if (user.getRole() == Role.CUSTOMER) {
            throw new ForbiddenException("Customers are not allowed to access analytics");
        }

        if (user.getRole() == Role.VENDOR) {
            Vendor vendor = vendorRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ForbiddenException("Authenticated vendor has no vendor profile"));
            if (!vendor.getId().equals(vendorId)) {
                throw new ForbiddenException("Vendors are only allowed to access their own analytics");
            }
        }

        // 2. Read-through Caching
        String cacheKey = "analytics:" + vendorId;
        List<AnalyticsSnapshotCacheDto> cachedList = null;
        try {
            cachedList = (List<AnalyticsSnapshotCacheDto>) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Failed to retrieve analytics from cache for vendor: {}", vendorId, e);
        }

        List<AnalyticsSnapshotCacheDto> snapshotsList;
        if (cachedList != null) {
            snapshotsList = cachedList;
        } else {
            // Cache Miss - Query last 90 days from DB
            LocalDate ninetyDaysAgo = LocalDate.now(ZoneOffset.UTC).minusDays(90);
            List<AnalyticsSnapshot> allSnapshots = analyticsSnapshotRepository
                    .findByVendorIdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(vendorId, ninetyDaysAgo);

            snapshotsList = allSnapshots.stream()
                    .map(s -> new AnalyticsSnapshotCacheDto(
                            s.getSnapshotDate(),
                            s.getTotalOrders(),
                            s.getTotalRevenue(),
                            s.getAverageOrderValue(),
                            s.getTopItemId(),
                            s.getPeakHour()
                    ))
                    .collect(Collectors.toList());

            try {
                redisTemplate.opsForValue().set(cacheKey, snapshotsList, Duration.ofSeconds(900));
            } catch (Exception e) {
                log.warn("Failed to populate analytics cache for vendor: {}", vendorId, e);
            }
        }

        // 3. Filter by date range (snapshotDate >= LocalDate.now(ZoneOffset.UTC) - days) and sort ASC
        LocalDate startDate = LocalDate.now(ZoneOffset.UTC).minusDays(days);
        List<AnalyticsSnapshotCacheDto> filteredList = snapshotsList.stream()
                .filter(s -> !s.getSnapshotDate().isBefore(startDate))
                .sorted((s1, s2) -> s1.getSnapshotDate().compareTo(s2.getSnapshotDate()))
                .collect(Collectors.toList());

        // 4. Resolve MenuItem names to prevent N+1 queries
        List<UUID> topItemIds = filteredList.stream()
                .map(AnalyticsSnapshotCacheDto::getTopItemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, String> itemNameMap = new java.util.HashMap<>();
        if (!topItemIds.isEmpty()) {
            List<MenuItem> menuItems = menuItemRepository.findAllById(topItemIds);
            for (MenuItem item : menuItems) {
                itemNameMap.put(item.getId(), item.getName());
            }
        }

        // 5. Build DTO Response
        List<AnalyticsSnapshotResponseDto> snapshotDtos = filteredList.stream()
                .map(s -> new AnalyticsSnapshotResponseDto(
                        s.getSnapshotDate(),
                        s.getTotalOrders(),
                        s.getTotalRevenue(),
                        s.getAverageOrderValue(),
                        s.getTopItemId() != null ? itemNameMap.get(s.getTopItemId()) : null,
                        s.getPeakHour()
                ))
                .collect(Collectors.toList());

        return new AnalyticsResponseDto(vendorId, snapshotDtos, days);
    }
}
