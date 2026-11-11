package com.mentaiko.backend.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.entity.WeeklyGoal;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.repository.WeeklyGoalRepository;

@Service
public class WeeklyGoalService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final UserRepository users;
    private final WeeklyGoalRepository goals;
    private final EmotionalEntryRepository entries;
    private final Clock clock;

    public record Goal(LocalDate weekStart, Integer targetDays, int completedDays,
                       int percentage, String status) {}

    public WeeklyGoalService(UserRepository users, WeeklyGoalRepository goals,
                             EmotionalEntryRepository entries, Clock clock) {
        this.users = users;
        this.goals = goals;
        this.entries = entries;
        this.clock = clock;
    }

    private LocalDate weekStart() {
        return LocalDate.now(clock.withZone(LIMA))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    @Transactional(readOnly = true)
    public Goal get(String email) {
        User user = users.findByEmailIgnoreCase(email).filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));
        return response(user);
    }

    @Transactional
    public Goal save(String email, Integer targetDays) {
        if (targetDays == null || targetDays < 1 || targetDays > 7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La meta debe ser de 1 a 7 días");
        }
        User user = users.findLockedByEmail(email).filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));
        var monday = weekStart();
        var goal = goals.findByUserIdAndWeekStart(user.getId(), monday).orElseGet(WeeklyGoal::new);
        goal.setUser(user);
        goal.setWeekStart(monday);
        goal.setTargetDays(targetDays);
        goals.saveAndFlush(goal);
        return response(user);
    }

    private Goal response(User user) {
        var monday = weekStart();
        Integer target = goals.findByUserIdAndWeekStart(user.getId(), monday)
                .map(WeeklyGoal::getTargetDays).orElse(null);
        var distinct = new HashSet<LocalDate>();
        for (var entry : entries.findReportEntries(user, monday.atStartOfDay(),
                               monday.plusDays(7).atStartOfDay().minusNanos(1))) {
            distinct.add(entry.getCreatedAt().toLocalDate());
        }
        int days = distinct.size();
        int percentage = target == null ? 0 : Math.min(100, days * 100 / target);
        String status = target == null ? "SIN_META" : days == 0 ? "PENDIENTE"
                : days >= target ? "CUMPLIDA" : "EN_PROGRESO";
        return new Goal(monday, target, days, percentage, status);
    }
}
