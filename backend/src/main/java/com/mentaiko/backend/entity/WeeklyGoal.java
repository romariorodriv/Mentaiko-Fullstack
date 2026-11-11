package com.mentaiko.backend.entity;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "weekly_goals", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "week_start"}))
public class WeeklyGoal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;
    @Column(name = "target_days", nullable = false)
    private int targetDays;
    public Long getId() {return id;}
    public User getUser() {return user;}
    public void setUser(User value) {user=value;}
    public LocalDate getWeekStart() {return weekStart;}
    public void setWeekStart(LocalDate value) {weekStart=value;}
    public int getTargetDays() {return targetDays;}
    public void setTargetDays(int value) {targetDays=value;}
}
