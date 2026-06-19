package com.streetvendor.analytics.service;

import com.streetvendor.analytics.dto.AnalyticsResponseDto;
import java.time.LocalDate;
import java.util.UUID;

public interface AnalyticsService {
    /**
     * Aggregates completed, paid order metrics for the specified date,
     * persists them using PostgreSQL native atomic upsert, and populates
     * the Redis cache for affected vendors.
     *
     * @param snapshotDate target date for snapshot aggregation
     */
    void generateSnapshots(LocalDate snapshotDate);

    /**
     * Retrieves historical performance snapshots for a vendor within the specified days window.
     * Enforces ownership checks and utilizes cache read-through pattern.
     *
     * @param vendorId target vendor ID
     * @param days number of days (1 to 90)
     * @return populated AnalyticsResponseDto
     */
    AnalyticsResponseDto getVendorAnalytics(UUID vendorId, int days);
}
