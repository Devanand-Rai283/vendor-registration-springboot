package com.streetvendor.discovery.cache;

import java.time.Duration;
import java.util.Optional;

public interface DiscoveryCacheService {

    <T> Optional<T> get(String key, Class<T> type);

    void put(String key, Object value, Duration ttl);

    void evict(String key);

    void evictPattern(String pattern);
}
