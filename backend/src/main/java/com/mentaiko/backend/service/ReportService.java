package com.mentaiko.backend.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.dto.report.DistributionItemResponse;
import com.mentaiko.backend.dto.report.DistributionResponse;
import com.mentaiko.backend.dto.report.WeeklyPointResponse;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.exception.UserInactiveException;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.UserRepository;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private static final Pattern WEEK_PATTERN = Pattern.compile("^(\\d{4})-W(\\d{2})$");
    private static final WeekFields ISO_WEEK = WeekFields.ISO;

    private final EmotionalEntryRepository emotionalEntryRepository;
    private final UserRepository userRepository;

    public ReportService(EmotionalEntryRepository emotionalEntryRepository, UserRepository userRepository) {
        this.emotionalEntryRepository = emotionalEntryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<WeeklyPointResponse> weekly(String userEmail, String week) {
        User user = findActiveUser(userEmail);
        LocalDate start = startOfWeek(week);
        List<EmotionalEntry> entries = emotionalEntryRepository.findReportEntries(
                user,
                start.atStartOfDay(),
                start.plusDays(6).atTime(LocalTime.MAX)
        );
        log.debug("Weekly report entries loaded for user id={} week={} count={}",
                user.getId(), week, entries.size());

        List<WeeklyPointResponse> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            List<EmotionalEntry> dayEntries = entries.stream()
                    .filter(entry -> entry.getCreatedAt().toLocalDate().equals(day))
                    .toList();
            double average = dayEntries.isEmpty()
                    ? 0
                    : dayEntries.stream().mapToInt(EmotionalEntry::getIntensity).average().orElse(0);
            result.add(new WeeklyPointResponse(labelFor(day.getDayOfWeek()), dayEntries.size(), roundOne(average)));
        }
        log.info("Weekly report generated for user id={} week={}", user.getId(), week);
        return result;
    }

    @Transactional(readOnly = true)
    public DistributionResponse distribution(String userEmail, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            log.warn("Distribution report rejected because date range is invalid");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha inicial no puede ser posterior a la fecha final");
        }

        User user = findActiveUser(userEmail);
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.atTime(LocalTime.MAX);
        List<EmotionalEntry> entries = emotionalEntryRepository.findReportEntries(user, fromDateTime, toDateTime);
        log.info("Distribution report generated for user id={} from={} to={} entries={}",
                user.getId(), from, to, entries.size());

        return new DistributionResponse(
                distributionFor(entries, entry -> entry.getEmotion().getName()),
                distributionFor(entries, EmotionalEntry::getContext)
        );
    }

    private LocalDate startOfWeek(String value) {
        Matcher matcher = WEEK_PATTERN.matcher(value == null ? "" : value);
        if (!matcher.matches()) {
            log.warn("Weekly report rejected because week format is invalid");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La semana debe tener formato YYYY-Www");
        }

        int year = Integer.parseInt(matcher.group(1));
        int week = Integer.parseInt(matcher.group(2));
        if (week < 1 || week > 53) {
            log.warn("Weekly report rejected because week number={} is outside allowed range", week);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La semana debe estar entre 01 y 53");
        }

        return LocalDate.of(year, 1, 4)
                .with(ISO_WEEK.weekOfWeekBasedYear(), week)
                .with(DayOfWeek.MONDAY);
    }

    private List<DistributionItemResponse> distributionFor(
            List<EmotionalEntry> entries,
            java.util.function.Function<EmotionalEntry, String> classifier
    ) {
        if (entries.isEmpty()) {
            return List.of();
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        for (EmotionalEntry entry : entries) {
            String label = classifier.apply(entry);
            counts.put(label, counts.getOrDefault(label, 0L) + 1);
        }

        long total = entries.size();
        return counts.entrySet().stream()
                .map(entry -> new DistributionItemResponse(
                        entry.getKey(),
                        entry.getValue(),
                        roundOne((entry.getValue() * 100.0) / total)
                ))
                .sorted(Comparator
                        .comparingLong(DistributionItemResponse::count).reversed()
                        .thenComparing(DistributionItemResponse::label))
                .toList();
    }

    private String labelFor(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-PE"));
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private User findActiveUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Report operation rejected because authenticated user was not found");
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
                });
        if (!user.isActive()) {
            log.warn("Report operation rejected because user id={} is inactive", user.getId());
            throw new UserInactiveException("El usuario esta desactivado");
        }
        return user;
    }
}