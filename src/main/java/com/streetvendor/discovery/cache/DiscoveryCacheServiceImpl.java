package com.streetvendor.discovery.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Service
public class DiscoveryCacheServiceImpl implements DiscoveryCacheService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryCacheServiceImpl.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public DiscoveryCacheServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Cache hit for key: {}", key);
                return Optional.of(type.cast(value));
            }
            log.debug("Cache miss for key: {}", key);
        } catch (Exception e) {
            log.warn("Cache error reading key {}: {}", key, e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Cache write for key: {} with TTL: {}s", key, ttl.getSeconds());
        } catch (Exception e) {
            log.warn("Cache error writing key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public void evict(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Cache evict for key: {} (deleted: {})", key, deleted);
        } catch (Exception e) {
            log.warn("Cache error evicting key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public void evictPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (!CollectionUtils.isEmpty(keys)) {
                Long deleted = redisTemplate.delete(keys);
                log.debug("Cache pattern evict for pattern: {} (deleted: {} keys)", pattern, deleted);
            } else {
                log.debug("Cache pattern evict for pattern: {} (no keys matched)", pattern);
            }
        } catch (Exception e) {
            log.warn("Cache error evicting pattern {}: {}", pattern, e.getMessage());
        }
    }
}
