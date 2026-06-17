package com.streetvendor.discovery.cache;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CacheKeyGeneratorTest {

    @Test
    void vendorSearchKey() {
        String key = CacheKeyGenerator.vendorSearchKey(12.34, 56.78, 5.0);
        assertThat(key).isEqualTo("search:vendors:12.34:56.78:5.0");
    }

    @Test
    void vendorMenuKey() {
        UUID vendorId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String key = CacheKeyGenerator.vendorMenuKey(vendorId);
        assertThat(key).isEqualTo("vendor:menu:123e4567-e89b-12d3-a456-426614174000");
    }
}
