package com.streetvendor.discovery.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryCachePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DiscoveryCacheConfig.class);

    @Test
    void defaultTtlValuesAreLoaded() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DiscoveryCacheProperties.class);

            DiscoveryCacheProperties props = context.getBean(DiscoveryCacheProperties.class);
            assertThat(props.getVendorSearchTtl()).isEqualTo(Duration.ofSeconds(600));
            assertThat(props.getVendorMenuTtl()).isEqualTo(Duration.ofSeconds(900));
        });
    }

    @Test
    void customTtlValuesAreBoundSuccessfully() {
        contextRunner.withPropertyValues(
                "discovery.cache.vendor-search-ttl=300",
                "discovery.cache.vendor-menu-ttl=400").run(context -> {
                    assertThat(context).hasSingleBean(DiscoveryCacheProperties.class);

                    DiscoveryCacheProperties props = context.getBean(DiscoveryCacheProperties.class);
                    assertThat(props.getVendorSearchTtl()).isEqualTo(Duration.ofSeconds(300));
                    assertThat(props.getVendorMenuTtl()).isEqualTo(Duration.ofSeconds(400));
                });
    }
}
