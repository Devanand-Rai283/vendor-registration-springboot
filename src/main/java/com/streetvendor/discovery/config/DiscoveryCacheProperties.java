package com.streetvendor.discovery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@ConfigurationProperties(prefix = "discovery.cache")
public class DiscoveryCacheProperties {

    private final Duration vendorSearchTtl;
    private final Duration vendorMenuTtl;

    public DiscoveryCacheProperties(
            @DefaultValue("600") @DurationUnit(ChronoUnit.SECONDS) Duration vendorSearchTtl,
            @DefaultValue("900") @DurationUnit(ChronoUnit.SECONDS) Duration vendorMenuTtl) {
        this.vendorSearchTtl = vendorSearchTtl;
        this.vendorMenuTtl = vendorMenuTtl;
    }

    public Duration getVendorSearchTtl() {
        return vendorSearchTtl;
    }

    public Duration getVendorMenuTtl() {
        return vendorMenuTtl;
    }
}
