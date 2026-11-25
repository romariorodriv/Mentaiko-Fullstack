package com.mentaiko.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mentaiko.backend.entity.FocusSession;
import com.mentaiko.backend.entity.User;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
    Optional<FocusSession> findByUserAndRequestId(User user, UUID requestId);
}
