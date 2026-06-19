package com.streetvendor.analytics.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AnalyticsSnapshotResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDate snapshotDate;
    private Integer totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private String topItem;
    private Integer peakHour;

    public AnalyticsSnapshotResponseDto() {
    }

    public AnalyticsSnapshotResponseDto(LocalDate snapshotDate, Integer totalOrders, BigDecimal totalRevenue,
                                        BigDecimal averageOrderValue, String topItem, Integer peakHour) {
        this.snapshotDate = snapshotDate;
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.averageOrderValue = averageOrderValue;
        this.topItem = topItem;
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

    public String getTopItem() {
        return topItem;
    }

    public void setTopItem(String topItem) {
        this.topItem = topItem;
    }

    public Integer getPeakHour() {
        return peakHour;
    }

    public void setPeakHour(Integer peakHour) {
        this.peakHour = peakHour;
    }
}
