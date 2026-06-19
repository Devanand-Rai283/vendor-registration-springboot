package com.streetvendor.analytics.repository;

import com.streetvendor.analytics.entity.AnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, UUID> {

    Optional<AnalyticsSnapshot> findByVendorIdAndSnapshotDate(UUID vendorId, LocalDate snapshotDate);

    List<AnalyticsSnapshot> findByVendorIdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(UUID vendorId, LocalDate snapshotDate);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO analytics_snapshots (id, vendor_id, snapshot_date, total_orders, total_revenue, average_order_value, top_item_id, peak_hour, created_at, updated_at) " +
            "VALUES (:id, :vendorId, :snapshotDate, :totalOrders, :totalRevenue, :averageOrderValue, :topItemId, :peakHour, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (vendor_id, snapshot_date) " +
            "DO UPDATE SET total_orders = EXCLUDED.total_orders, " +
            "              total_revenue = EXCLUDED.total_revenue, " +
            "              average_order_value = EXCLUDED.average_order_value, " +
            "              top_item_id = EXCLUDED.top_item_id, " +
            "              peak_hour = EXCLUDED.peak_hour, " +
            "              updated_at = CURRENT_TIMESTAMP", nativeQuery = true)
    void upsertSnapshot(
            @Param("id") UUID id,
            @Param("vendorId") UUID vendorId,
            @Param("snapshotDate") LocalDate snapshotDate,
            @Param("totalOrders") Integer totalOrders,
            @Param("totalRevenue") BigDecimal totalRevenue,
            @Param("averageOrderValue") BigDecimal averageOrderValue,
            @Param("topItemId") UUID topItemId,
            @Param("peakHour") Integer peakHour
    );
}
