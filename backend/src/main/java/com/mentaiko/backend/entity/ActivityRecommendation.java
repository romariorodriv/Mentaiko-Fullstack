package com.mentaiko.backend.entity;

import java.time.LocalDateTime;

import com.mentaiko.backend.enums.RecommendationDiscardReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "activity_recommendations",
        indexes = @Index(name = "ix_activity_recommendations_checkin", columnList = "checkin_id")
)
public class ActivityRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checkin_id", nullable = false)
    private EmotionalEntry checkin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private MicroActivity activity;

    @Column(nullable = false, length = 240)
    private String reason;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "discarded_at")
    private LocalDateTime discardedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "discard_reason", length = 30)
    private RecommendationDiscardReason discardReason;

    @Column(name = "helpfulness_rating")
    private Integer helpfulnessRating;

    @Column(name = "feedback_comment", length = 300)
    private String feedbackComment;

    @Column(name = "post_intensity")
    private Integer postIntensity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public EmotionalEntry getCheckin() {
        return checkin;
    }

    public void setCheckin(EmotionalEntry checkin) {
        this.checkin = checkin;
    }

    public MicroActivity getActivity() {
        return activity;
    }

    public void setActivity(MicroActivity activity) {
        this.activity = activity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public boolean isCompleted() {
        return completed;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getDiscardedAt() {
        return discardedAt;
    }

    public RecommendationDiscardReason getDiscardReason() {
        return discardReason;
    }

    public void discard(RecommendationDiscardReason reason) {
        discardedAt = LocalDateTime.now();
        discardReason = reason;
    }

    public void complete() {
        if (!completed) {
            completed = true;
            completedAt = LocalDateTime.now();
        }
    }

    public Integer getPostIntensity() {
        return postIntensity;
    }

    /** Registra la intensidad posterior (1 a 5) que percibe la persona; es opcional. */
    public void recordPostIntensity(Integer postIntensity) {
        this.postIntensity = postIntensity;
    }

    public Integer getHelpfulnessRating() {
        return helpfulnessRating;
    }

    public String getFeedbackComment() {
        return feedbackComment;
    }

    /** Guarda la valoración de utilidad; solo tiene sentido sobre una actividad completada. */
    public void rate(int rating, String comment) {
        this.helpfulnessRating = rating;
        this.feedbackComment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
