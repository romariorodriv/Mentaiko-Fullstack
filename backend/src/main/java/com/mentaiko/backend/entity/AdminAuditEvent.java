package com.mentaiko.backend.entity;

import java.time.OffsetDateTime;
import jakarta.persistence.*;

/** Schema mapping for Hibernate validation and isolated H2 integration tests. */
@Entity
@Table(name = "admin_audit_events")
public class AdminAuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(nullable = false, length = 12)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 24)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "before_summary", length = 500)
    private String beforeSummary;

    @Column(name = "after_summary", nullable = false, length = 500)
    private String afterSummary;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    protected AdminAuditEvent() {}
}
