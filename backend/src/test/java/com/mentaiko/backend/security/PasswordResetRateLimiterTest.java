package com.mentaiko.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PasswordResetRateLimiterTest {
    @Test
    void limitsEachKeyAndAllowsAgainAtWindowBoundary() {
        Clock clock = mock(Clock.class);
        Instant now = Instant.parse("2026-11-11T12:00:00Z");
        when(clock.instant()).thenReturn(now);
        PasswordResetRateLimiter limiter = new PasswordResetRateLimiter(clock);
        assertThat(limiter.acquire("first", 2, Duration.ofMinutes(15))).isTrue();
        assertThat(limiter.acquire("first", 2, Duration.ofMinutes(15))).isTrue();
        assertThat(limiter.acquire("first", 2, Duration.ofMinutes(15))).isFalse();
        assertThat(limiter.acquire("second", 2, Duration.ofMinutes(15))).isTrue();
        when(clock.instant()).thenReturn(now.plusSeconds(900));
        assertThat(limiter.acquire("first", 2, Duration.ofMinutes(15))).isTrue();
    }
}
