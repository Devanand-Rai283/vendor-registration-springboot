package com.streetvendor.order.repository;

import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerId(UUID customerId);

    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    List<Order> findByVendorId(UUID vendorId);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByIdempotencyKeyAndCustomerId(String idempotencyKey, UUID customerId);

    List<Order> findByStatus(OrderStatus status);

    /**
     * Counts orders whose {@code createdAt} timestamp falls within
     * the given half-open interval {@code [start, end)}.
     *
     * <p>Used by the admin dashboard to compute total orders placed today.
     *
     * @param start inclusive start of the interval (e.g. midnight UTC today)
     * @param end   exclusive end of the interval (e.g. midnight UTC tomorrow)
     * @return count of orders created within the interval
     */
    long countByCreatedAtBetween(Instant start, Instant end);

    Page<Order> findByVendorId(UUID vendorId, Pageable pageable);

    Page<Order> findByVendorIdAndStatus(UUID vendorId, OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndVendorId(UUID id, UUID vendorId);

    long countByVendorId(UUID vendorId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.vendor.id = :vendorId AND o.status IN :statuses")
    long countByVendorIdAndStatusIn(@Param("vendorId") UUID vendorId, @Param("statuses") Collection<OrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.vendor.id = :vendorId AND o.status <> com.streetvendor.order.enums.OrderStatus.CANCELLED")
    BigDecimal sumRevenueByVendorId(@Param("vendorId") UUID vendorId);

    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o WHERE o.vendor.id = :vendorId AND o.status <> com.streetvendor.order.enums.OrderStatus.CANCELLED")
    BigDecimal averageOrderValueByVendorId(@Param("vendorId") UUID vendorId);
}
