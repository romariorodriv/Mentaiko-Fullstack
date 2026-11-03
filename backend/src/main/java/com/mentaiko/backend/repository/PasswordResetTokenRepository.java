package com.mentaiko.backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import com.mentaiko.backend.entity.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    boolean existsByUserIdAndUsedAtIsNullAndCreatedAtAfter(Long userId, java.time.Instant createdAfter);
    java.util.List<PasswordResetToken> findAllByUserIdAndUsedAtIsNull(Long userId);
}
