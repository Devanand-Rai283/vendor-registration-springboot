package com.streetvendor.discovery.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DiscoveryCacheServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private DiscoveryCacheServiceImpl cacheService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new DiscoveryCacheServiceImpl(redisTemplate);
    }

    @Test
    void get_ReturnsValue_OnCacheHit() {
        String key = "test:hit";
        when(valueOperations.get(key)).thenReturn("cached-string");

        Optional<String> result = cacheService.get(key, String.class);

        assertThat(result).isPresent().contains("cached-string");
        verify(valueOperations).get(key);
    }

    @Test
    void get_ReturnsEmpty_OnCacheMiss() {
        String key = "test:miss";
        when(valueOperations.get(key)).thenReturn(null);

        Optional<String> result = cacheService.get(key, String.class);

        assertThat(result).isEmpty();
        verify(valueOperations).get(key);
    }

    @Test
    void put_StoresValueWithTtl() {
        String key = "test:write";
        String value = "value";
        Duration ttl = Duration.ofSeconds(600);

        cacheService.put(key, value, ttl);

        verify(valueOperations).set(key, value, ttl);
    }

    @Test
    void evict_DeletesKey() {
        String key = "test:evict";

        cacheService.evict(key);

        verify(redisTemplate).delete(key);
    }

    @Test
    void evictPattern_DeletesMatchingKeys() {
        String pattern = "test:*";
        Set<String> keys = Set.of("test:1", "test:2");
        when(redisTemplate.keys(pattern)).thenReturn(keys);

        cacheService.evictPattern(pattern);

        verify(redisTemplate).keys(pattern);
        verify(redisTemplate).delete(keys);
    }

    @Test
    void evictPattern_DoesNothing_WhenNoKeysMatch() {
        String pattern = "empty:*";
        when(redisTemplate.keys(pattern)).thenReturn(Set.of());

        cacheService.evictPattern(pattern);

        verify(redisTemplate).keys(pattern);
        verify(redisTemplate, never()).delete(any(Set.class));
    }
}
