package com.student.management.common;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class RedisCacheService {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    private static final String KEY_PREFIX = "teaching-affairs:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Duration defaultTtl;
    private final Duration failureBackoff;
    private volatile long retryAtMillis;

    public RedisCacheService(StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper,
                             @Value("${app.cache.enabled:true}") boolean enabled,
                             @Value("${app.cache.ttl-seconds:300}") long ttlSeconds,
                             @Value("${app.cache.redis-backoff-seconds:30}") long redisBackoffSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper.copy();
        this.enabled = enabled;
        this.defaultTtl = Duration.ofSeconds(Math.max(ttlSeconds, 1));
        this.failureBackoff = Duration.ofSeconds(Math.max(redisBackoffSeconds, 1));
    }

    public <T> T get(String key, TypeReference<T> type, Supplier<T> loader) {
        T cached = getValue(key, type);
        if (cached != null) {
            return cached;
        }
        T value = loader.get();
        put(key, value);
        return value;
    }

    public <T> T getValue(String key, TypeReference<T> type) {
        if (!redisAllowed()) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(redisKey(key));
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            log.debug("Failed to deserialize Redis cache key {}", key, ex);
            return null;
        } catch (RuntimeException ex) {
            markRedisFailure(ex);
            return null;
        }
    }

    public boolean put(String key, Object value) {
        return put(key, value, defaultTtl);
    }

    public boolean put(String key, Object value, Duration ttl) {
        if (value == null || !redisAllowed()) {
            return false;
        }
        try {
            redisTemplate.opsForValue().set(redisKey(key), objectMapper.writeValueAsString(value), ttl);
            return true;
        } catch (JsonProcessingException ex) {
            log.debug("Failed to serialize Redis cache key {}", key, ex);
            return false;
        } catch (RuntimeException ex) {
            markRedisFailure(ex);
            return false;
        }
    }

    public void evict(String... keys) {
        if (!redisAllowed()) {
            return;
        }
        for (String key : keys) {
            try {
                redisTemplate.delete(redisKey(key));
            } catch (RuntimeException ex) {
                markRedisFailure(ex);
                return;
            }
        }
    }

    public void evictByPrefix(String... prefixes) {
        if (!redisAllowed()) {
            return;
        }
        for (String prefix : prefixes) {
            try {
                Set<String> keys = redisTemplate.keys(redisKey(prefix) + "*");
                if (!CollectionUtils.isEmpty(keys)) {
                    redisTemplate.delete(keys);
                }
            } catch (RuntimeException ex) {
                markRedisFailure(ex);
                return;
            }
        }
    }

    public String keyPart(Object value) {
        if (value == null) {
            return "all";
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return "all";
        }
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private boolean redisAllowed() {
        return enabled && System.currentTimeMillis() >= retryAtMillis;
    }

    private String redisKey(String key) {
        return KEY_PREFIX + key;
    }

    private void markRedisFailure(RuntimeException ex) {
        retryAtMillis = System.currentTimeMillis() + failureBackoff.toMillis();
        log.debug("Redis unavailable, bypass cache for {} seconds", failureBackoff.toSeconds(), ex);
    }
}
