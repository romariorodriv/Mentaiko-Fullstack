package com.mentaiko.backend.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/me/emotional-calendar")
public class EmotionalCalendarController {
    private final UserRepository users;
    private final EmotionalEntryRepository entries;

    public EmotionalCalendarController(UserRepository users, EmotionalEntryRepository entries) {
        this.users = users;
        this.entries = entries;
    }

    public record EmotionCount(Long emotionId, String emotion, long count) {}
    public record Day(LocalDate date, long total, List<EmotionCount> emotions) {}
    public record Month(int year, int month, List<Day> days) {}

    @GetMapping
    @Transactional(readOnly = true)
    public Month month(Authentication authentication, @RequestParam int year, @RequestParam int month) {
        if (year < 2000 || year > 2100 || month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mes o año fuera de rango");
        }
        User user = users.findByEmailIgnoreCase(authentication.getName())
                .filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));
        YearMonth period = YearMonth.of(year, month);
        var from = period.atDay(1).atStartOfDay();
        var to = period.plusMonths(1).atDay(1).atStartOfDay().minusNanos(1);
        Map<LocalDate, Map<Long, MutableCount>> grouped = new LinkedHashMap<>();
        for (EmotionalEntry entry : entries.findReportEntries(user, from, to)) {
            var day = entry.getCreatedAt().toLocalDate();
            var perEmotion = grouped.computeIfAbsent(day, ignored -> new LinkedHashMap<>());
            var count = perEmotion.computeIfAbsent(entry.getEmotion().getId(),
                    ignored -> new MutableCount(entry.getEmotion().getName()));
            count.count++;
        }
        List<Day> days = new ArrayList<>();
        grouped.forEach((date, values) -> {
            List<EmotionCount> counts = new ArrayList<>();
            long total = 0;
            for (var value : values.entrySet()) {
                counts.add(new EmotionCount(value.getKey(), value.getValue().name, value.getValue().count));
                total += value.getValue().count;
            }
            days.add(new Day(date, total, counts));
        });
        return new Month(year, month, days);
    }

    private static final class MutableCount {
        private final String name;
        private long count;
        private MutableCount(String name) { this.name = name; }
    }
}
