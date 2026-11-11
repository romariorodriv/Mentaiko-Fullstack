package com.mentaiko.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.entity.WeeklyGoal;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.repository.WeeklyGoalRepository;

@ExtendWith(MockitoExtension.class)
class WeeklyGoalServiceTest {
    @Mock UserRepository users;
    @Mock WeeklyGoalRepository goals;
    @Mock EmotionalEntryRepository entries;
    WeeklyGoalService service;
    User user;

    @BeforeEach
    void prepare() {
        Clock clock=Clock.fixed(Instant.parse("2026-11-11T14:00:00Z"),ZoneOffset.UTC);
        service=new WeeklyGoalService(users,goals,entries,clock);
        user=new User();user.setId(42L);user.setEmail("student@example.com");user.setActive(true);
    }

    @Test
    void countsDistinctCalendarDaysRatherThanMultipleEntriesOnSameDay() {
        var monday=LocalDate.of(2026,11,9);
        WeeklyGoal goal=new WeeklyGoal();goal.setTargetDays(2);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        when(goals.findByUserIdAndWeekStart(42L,monday)).thenReturn(Optional.of(goal));
        var first=new EmotionalEntry();first.setCreatedAt(LocalDateTime.of(2026,11,10,9,0));
        var second=new EmotionalEntry();second.setCreatedAt(LocalDateTime.of(2026,11,10,18,0));
        when(entries.findReportEntries(org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(first,second));
        var response=service.get("student@example.com");
        assertThat(response.completedDays()).isEqualTo(1);
        assertThat(response.targetDays()).isEqualTo(2);
        assertThat(response.percentage()).isEqualTo(50);
        assertThat(response.weekStart()).isEqualTo(monday);
    }
}
