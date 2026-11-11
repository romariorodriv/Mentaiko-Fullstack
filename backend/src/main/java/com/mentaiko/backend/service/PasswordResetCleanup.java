package com.mentaiko.backend.service;

import java.time.Clock;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.mentaiko.backend.repository.PasswordResetTokenRepository;

@Component
public class PasswordResetCleanup {
    private final PasswordResetTokenRepository tokens;
    private final Clock clock;
    public PasswordResetCleanup(PasswordResetTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }
    @Scheduled(initialDelay = 86_400_000, fixedDelay = 86_400_000)
    @Transactional
    public void removeOldTokens() {
        tokens.deleteByExpiresAtBefore(clock.instant().minus(Duration.ofDays(7)));
    }
}
