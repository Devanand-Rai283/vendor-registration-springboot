package com.streetvendor.analytics.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class AnalyticsSnapshotCacheDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDate snapshotDate;
    private Integer totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private UUID topItemId;
    private Integer peakHour;

    public AnalyticsSnapshotCacheDto() {
    }

    public AnalyticsSnapshotCacheDto(LocalDate snapshotDate, Integer totalOrders, BigDecimal totalRevenue,
                                     BigDecimal averageOrderValue, UUID topItemId, Integer peakHour) {
        this.snapshotDate = snapshotDate;
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.averageOrderValue = averageOrderValue;
        this.topItemId = topItemId;
        this.peakHour = peakHour;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
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
