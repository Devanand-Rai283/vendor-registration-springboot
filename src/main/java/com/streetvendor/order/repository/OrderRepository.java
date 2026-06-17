package com.streetvendor.order.repository;

import com.streetvendor.order.entity.Order;
import com.streetvendor.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
