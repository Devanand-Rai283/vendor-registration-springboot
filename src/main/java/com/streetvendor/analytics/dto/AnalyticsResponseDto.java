package com.streetvendor.analytics.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class AnalyticsResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID vendorId;
    private List<AnalyticsSnapshotResponseDto> snapshots;
    private Integer periodDays;

    public AnalyticsResponseDto() {
    }

    public AnalyticsResponseDto(UUID vendorId, List<AnalyticsSnapshotResponseDto> snapshots, Integer periodDays) {
        this.vendorId = vendorId;
        this.snapshots = snapshots;
        this.periodDays = periodDays;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public void setVendorId(UUID vendorId) {
        this.vendorId = vendorId;
    }

    public List<AnalyticsSnapshotResponseDto> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<AnalyticsSnapshotResponseDto> snapshots) {
        this.snapshots = snapshots;
    }

    public Integer getPeriodDays() {
        return periodDays;
    }

    public void setPeriodDays(Integer periodDays) {
        this.periodDays = periodDays;
    }
}
