package com.streetvendor.analytics.entity;

import com.streetvendor.common.audit.AuditableEntity;
import com.streetvendor.vendor.entity.Vendor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "analytics_snapshots",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_analytics_snapshots_vendor_date", columnNames = {"vendor_id", "snapshot_date"})
    }
)
public class AnalyticsSnapshot extends AuditableEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @NotNull
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @NotNull
    @PositiveOrZero
    @Column(name = "total_orders", nullable = false)
    private Integer totalOrders;

    @NotNull
    @PositiveOrZero
    @Column(name = "total_revenue", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalRevenue;

    @NotNull
    @PositiveOrZero
    @Column(name = "average_order_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal averageOrderValue;

    @Column(name = "top_item_id", columnDefinition = "uuid")
    private UUID topItemId;

    @NotNull
    @Column(name = "peak_hour", nullable = false)
    private Integer peakHour;

    protected AnalyticsSnapshot() {
    }

    public AnalyticsSnapshot(UUID id, Vendor vendor, LocalDate snapshotDate, Integer totalOrders,
                             BigDecimal totalRevenue, BigDecimal averageOrderValue, UUID topItemId, Integer peakHour) {
        this.id = id;
        this.vendor = vendor;
        this.snapshotDate = snapshotDate;
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.averageOrderValue = averageOrderValue;
        this.topItemId = topItemId;
        this.peakHour = peakHour;
    }

    public UUID getId() {
        return id;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(BigDecimal averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public UUID getTopItemId() {
        return topItemId;
    }

    public void setTopItemId(UUID topItemId) {
        this.topItemId = topItemId;
    }

    public Integer getPeakHour() {
        return peakHour;
    }

    public void setPeakHour(Integer peakHour) {
        this.peakHour = peakHour;
    }
}
