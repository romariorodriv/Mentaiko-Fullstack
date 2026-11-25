package com.mentaiko.backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "focus_sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_focus_sessions_user_request", columnNames = {"user_id", "request_id"}),
        check = @CheckConstraint(name = "ck_focus_sessions_duration", constraint = "duration_seconds between 60 and 7200"))
public class FocusSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private MicroActivity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ActivityRecommendation recommendation;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    protected FocusSession() {
    }

    public FocusSession(User user, MicroActivity activity, ActivityRecommendation recommendation,
            UUID requestId, int durationSeconds) {
        this.user = user;
        this.activity = activity;
        this.recommendation = recommendation;
        this.requestId = requestId;
        this.durationSeconds = durationSeconds;
        this.completedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getActivityId() {
        return activity.getId();
    }

    public Long getRecommendationId() {
        return recommendation == null ? null : recommendation.getId();
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
