package com.mentaiko.backend.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.mentaiko.backend.entity.WeeklyGoal;

public interface WeeklyGoalRepository extends JpaRepository<WeeklyGoal, Long> {
    Optional<WeeklyGoal> findByUserIdAndWeekStart(Long userId, LocalDate weekStart);
}
