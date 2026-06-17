package com.streetvendor.discovery.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DiscoveryCacheProperties.class)
public class DiscoveryCacheConfig {
    // Enables constructor-bound DiscoveryCacheProperties
}
