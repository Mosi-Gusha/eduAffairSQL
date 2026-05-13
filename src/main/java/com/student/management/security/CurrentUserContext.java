package com.student.management.security;

import java.util.Optional;

public final class CurrentUserContext {
    private static final ThreadLocal<SessionUser> CURRENT = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(SessionUser user) {
        CURRENT.set(user);
    }

    public static Optional<SessionUser> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
