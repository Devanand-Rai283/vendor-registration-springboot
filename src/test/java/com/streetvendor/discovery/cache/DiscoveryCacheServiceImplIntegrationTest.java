package com.streetvendor.discovery.cache;

import com.streetvendor.config.AbstractRedisIntegrationTest;
import com.streetvendor.discovery.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryCacheServiceImplIntegrationTest extends AbstractRedisIntegrationTest {

    @Autowired
    private DiscoveryCacheService cacheService;

    @Test
    void testCacheWriteAndRead() {
        String key = "test:key:write-read";
        VendorSummaryResponse originalDto = new VendorSummaryResponse(
                UUID.randomUUID(),
                "Yummy Bites",
                "Snacks",
                "789 Snack Alley",
                new BigDecimal("4.8"),
                12.9716,
                77.5946,
                0.5
        );

        cacheService.put(key, originalDto, Duration.ofMinutes(5));

        Optional<VendorSummaryResponse> cached = cacheService.get(key, VendorSummaryResponse.class);
        assertThat(cached).isPresent();
        assertThat(cached.get().id()).isEqualTo(originalDto.id());
        assertThat(cached.get().businessName()).isEqualTo(originalDto.businessName());
        assertThat(cached.get().averageRating()).isEqualTo(originalDto.averageRating());
    }

    @Test
    void testCacheMissReturnsEmpty() {
        String key = "test:key:non-existent";
        Optional<VendorSummaryResponse> cached = cacheService.get(key, VendorSummaryResponse.class);
        assertThat(cached).isEmpty();
    }

    @Test
    void testCacheEviction() {
        String key = "test:key:evict";
        VendorSummaryResponse originalDto = new VendorSummaryResponse(
                UUID.randomUUID(),
                "Yummy Bites",
                "Snacks",
                "789 Snack Alley",
                new BigDecimal("4.8"),
                12.9716,
                77.5946,
                0.5
        );

        cacheService.put(key, originalDto, Duration.ofMinutes(5));
        assertThat(cacheService.get(key, VendorSummaryResponse.class)).isPresent();

        cacheService.evict(key);
        assertThat(cacheService.get(key, VendorSummaryResponse.class)).isEmpty();
    }

    @Test
    void testPatternEviction() {
        String key1 = "search:vendors:key1";
        String key2 = "search:vendors:key2";
        String key3 = "vendor:menu:key3";

        VendorSummaryResponse dto = new VendorSummaryResponse(
                UUID.randomUUID(),
                "Yummy Bites",
                "Snacks",
                "789 Snack Alley",
                new BigDecimal("4.8"),
                12.9716,
                77.5946,
                0.5
        );

        cacheService.put(key1, dto, Duration.ofMinutes(5));
        cacheService.put(key2, dto, Duration.ofMinutes(5));
        cacheService.put(key3, dto, Duration.ofMinutes(5));

        assertThat(cacheService.get(key1, VendorSummaryResponse.class)).isPresent();
        assertThat(cacheService.get(key2, VendorSummaryResponse.class)).isPresent();
        assertThat(cacheService.get(key3, VendorSummaryResponse.class)).isPresent();

        cacheService.evictPattern("search:vendors:*");

        assertThat(cacheService.get(key1, VendorSummaryResponse.class)).isEmpty();
        assertThat(cacheService.get(key2, VendorSummaryResponse.class)).isEmpty();
        assertThat(cacheService.get(key3, VendorSummaryResponse.class)).isPresent();
    }

    @Test
    void testCacheTtlExpiration() throws InterruptedException {
        String key = "test:key:ttl";
        VendorSummaryResponse dto = new VendorSummaryResponse(
                UUID.randomUUID(),
                "Yummy Bites",
                "Snacks",
                "789 Snack Alley",
                new BigDecimal("4.8"),
                12.9716,
                77.5946,
                0.5
        );

        cacheService.put(key, dto, Duration.ofSeconds(1));
        assertThat(cacheService.get(key, VendorSummaryResponse.class)).isPresent();

        // Busy-wait or sleep for expiration
        long startTime = System.currentTimeMillis();
        boolean expired = false;
        while (System.currentTimeMillis() - startTime < 3000) {
            if (cacheService.get(key, VendorSummaryResponse.class).isEmpty()) {
                expired = true;
                break;
            }
            Thread.sleep(100);
        }

        assertThat(expired).isTrue();
    }

    @Test
    void testSerializationRoundTripPreservesTypes() {
        MenuItemResponseDto item = new MenuItemResponseDto(
                UUID.randomUUID(),
                "Noodles",
                "Tasty noodles",
                new BigDecimal("10.50"),
                "Veg",
                "http://example.com/noodles.jpg",
                true
        );

        MenuCategoryResponseDto category = new MenuCategoryResponseDto(
                UUID.randomUUID(),
                "Chinese",
                1,
                List.of(item)
        );

        VendorMenuResponseDto menu = new VendorMenuResponseDto(
                UUID.randomUUID(),
                "Golden Dragon",
                List.of(category)
        );

        String key = "test:menu:serialization";
        cacheService.put(key, menu, Duration.ofMinutes(5));

        Optional<VendorMenuResponseDto> cached = cacheService.get(key, VendorMenuResponseDto.class);
        assertThat(cached).isPresent();

        VendorMenuResponseDto restored = cached.get();
        assertThat(restored.vendorId()).isEqualTo(menu.vendorId());
        assertThat(restored.vendorName()).isEqualTo(menu.vendorName());
        assertThat(restored.categories()).hasSize(1);
        assertThat(restored.categories().get(0).id()).isEqualTo(category.id());
        assertThat(restored.categories().get(0).items()).hasSize(1);
        assertThat(restored.categories().get(0).items().get(0).price()).isEqualTo(new BigDecimal("10.50"));
    }
}
