package com.mentaiko.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "favorite_micro_activities",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorite_micro_activity_user_activity",
                columnNames = {"user_id", "micro_activity_id"}))
public class FavoriteMicroActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "micro_activity_id", nullable = false)
    private MicroActivity activity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FavoriteMicroActivity() {
    }

    public FavoriteMicroActivity(User user, MicroActivity activity) {
        this.user = user;
        this.activity = activity;
    }

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

    public MicroActivity getActivity() {
        return activity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
