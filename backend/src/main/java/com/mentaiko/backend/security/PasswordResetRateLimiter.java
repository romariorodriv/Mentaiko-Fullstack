package com.mentaiko.backend.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Bounded, per-process admission control. Persistent per-account cooldown is separate. */
@Component
public class PasswordResetRateLimiter {
    private record Window(Instant endsAt, int count) {}
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;

    public PasswordResetRateLimiter(Clock clock) { this.clock = clock; }

    public synchronized boolean acquire(String key, int limit, Duration duration) {
        Instant now = clock.instant();
        Window window = windows.get(key);
        if (window == null || !now.isBefore(window.endsAt())) {
            if (windows.size() >= 10_000) {
                windows.values().removeIf(w -> !now.isBefore(w.endsAt()));
                if (windows.size() >= 10_000 && window == null) return false;
            }
            windows.put(key, new Window(now.plus(duration), 1));
            return true;
        }
        if (window.count() >= limit) return false;
        windows.put(key, new Window(window.endsAt(), window.count() + 1));
        return true;
    }
}
