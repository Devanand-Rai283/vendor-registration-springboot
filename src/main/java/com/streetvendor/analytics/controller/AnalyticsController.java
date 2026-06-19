package com.streetvendor.analytics.controller;

import com.streetvendor.analytics.dto.AnalyticsResponseDto;
import com.streetvendor.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/vendors")
@Tag(name = "Analytics", description = "Endpoints for vendor performance analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{id}/analytics")
    @Operation(summary = "Get historical daily performance snapshots for a vendor")
    public ResponseEntity<AnalyticsResponseDto> getVendorAnalytics(
            @PathVariable("id") UUID id,
            @RequestParam(name = "days", defaultValue = "30") int days) {
        
        if (days < 1 || days > 90) {
            throw new IllegalArgumentException("Days parameter must be between 1 and 90");
        }

        AnalyticsResponseDto response = analyticsService.getVendorAnalytics(id, days);
        return ResponseEntity.ok(response);
    }
}
