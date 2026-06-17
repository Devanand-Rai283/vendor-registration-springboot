package com.streetvendor.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RedisStartupFailFastTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisConfig.class));

    @Test
    void whenRedisReachable_andPingDisabled_startupSucceeds() {
        contextRunner
                .withPropertyValues(
                        "spring.data.redis.host=localhost",
                        "spring.data.redis.port=9999", // Unreachable port but ping is disabled
                        "spring.data.redis.ping-on-startup=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(org.springframework.data.redis.connection.RedisConnectionFactory.class);
                });
    }

    @Test
    void whenRedisUnreachable_andPingEnabled_startupFails() {
        contextRunner
                .withPropertyValues(
                        "spring.data.redis.host=localhost",
                        "spring.data.redis.port=9999", // Unreachable port and ping is enabled
                        "spring.data.redis.ping-on-startup=true"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(java.net.ConnectException.class);
                });
    }
}
