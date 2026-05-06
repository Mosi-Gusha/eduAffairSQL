package com.student.management.security;

import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.student.management.common.RedisCacheService;
import org.springframework.stereotype.Component;

@Component
public class SessionRegistry {
    private static final long TTL_SECONDS = 8 * 60 * 60;
    private static final TypeReference<SessionUser> SESSION_USER_TYPE = new TypeReference<>() {
    };
    private final ConcurrentHashMap<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private final RedisCacheService cache;

    public SessionRegistry(RedisCacheService cache) {
        this.cache = cache;
    }

    public String create(SessionUser user) {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new SessionEntry(user, Instant.now().plusSeconds(TTL_SECONDS)));
        cache.put(sessionKey(token), user, Duration.ofSeconds(TTL_SECONDS));
        return token;
    }

    public Optional<SessionUser> find(String token) {
        SessionUser redisUser = cache.getValue(sessionKey(token), SESSION_USER_TYPE);
        if (redisUser != null) {
            sessions.put(token, new SessionEntry(redisUser, Instant.now().plusSeconds(TTL_SECONDS)));
            cache.put(sessionKey(token), redisUser, Duration.ofSeconds(TTL_SECONDS));
            return Optional.of(redisUser);
        }
        SessionEntry entry = sessions.get(token);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        sessions.put(token, new SessionEntry(entry.user(), Instant.now().plusSeconds(TTL_SECONDS)));
        cache.put(sessionKey(token), entry.user(), Duration.ofSeconds(TTL_SECONDS));
        return Optional.of(entry.user());
    }

    public void remove(String token) {
        sessions.remove(token);
        cache.evict(sessionKey(token));
    }

    private record SessionEntry(SessionUser user, Instant expiresAt) {
    }

    private String sessionKey(String token) {
        return "session:" + token;
    }
}
