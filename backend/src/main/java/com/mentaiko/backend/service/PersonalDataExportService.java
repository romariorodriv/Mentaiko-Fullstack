package com.mentaiko.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.repository.UserRepository;

@Service
public class PersonalDataExportService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public PersonalDataExportService(JdbcTemplate jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(String email, LocalDate from, LocalDate to, boolean includeNotes) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha inicial no puede ser posterior a la fecha final");
        }
        Long userId = users.findByEmailIgnoreCase(email)
                .filter(user -> user.isActive())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autorizado"))
                .getId();

        StringBuilder checkinSql = new StringBuilder("""
                SELECT c.id, c.created_at, e.name AS emotion, c.intensity, c.context, c.note
                FROM checkins c JOIN emotions e ON e.id = c.emotion_id
                WHERE c.user_id = ?
                """);
        List<Object> checkinArgs = new ArrayList<>();
        checkinArgs.add(userId);
        appendDateFilter(checkinSql, checkinArgs, "c.created_at", from, to);
        checkinSql.append(" ORDER BY c.created_at ASC, c.id ASC");
        List<Map<String, Object>> checkins = jdbc.query(checkinSql.toString(), (rs, row) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("createdAt", rs.getTimestamp("created_at").toLocalDateTime());
            item.put("emotion", rs.getString("emotion"));
            item.put("intensity", rs.getInt("intensity"));
            item.put("context", rs.getString("context"));
            if (includeNotes) item.put("note", rs.getString("note"));
            return item;
        }, checkinArgs.toArray());

        StringBuilder activitySql = new StringBuilder("""
                SELECT r.id, r.created_at, r.completed_at, a.title, a.duration_minutes,
                       r.helpfulness_rating, r.feedback_comment
                FROM activity_recommendations r JOIN micro_activities a ON a.id = r.activity_id
                WHERE r.user_id = ?
                """);
        List<Object> activityArgs = new ArrayList<>();
        activityArgs.add(userId);
        appendDateFilter(activitySql, activityArgs, "r.created_at", from, to);
        activitySql.append(" ORDER BY r.created_at ASC, r.id ASC");
        List<Map<String, Object>> activities = jdbc.query(activitySql.toString(), (rs, row) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("createdAt", rs.getTimestamp("created_at").toLocalDateTime());
            var completed = rs.getTimestamp("completed_at");
            item.put("completedAt", completed == null ? null : completed.toLocalDateTime());
            item.put("activity", rs.getString("title"));
            item.put("durationMinutes", rs.getInt("duration_minutes"));
            item.put("helpfulnessRating", rs.getObject("helpfulness_rating", Integer.class));
            if (includeNotes) item.put("feedbackComment", rs.getString("feedback_comment"));
            return item;
        }, activityArgs.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from);
        result.put("to", to);
        result.put("checkins", checkins);
        result.put("activities", activities);
        return result;
    }

    private static void appendDateFilter(StringBuilder sql, List<Object> parameters,
                                         String timestampColumn, LocalDate from, LocalDate to) {
        if (from != null) {
            sql.append(" AND ").append(timestampColumn).append(" >= ?");
            parameters.add(from.atStartOfDay());
        }
        if (to != null) {
            sql.append(" AND ").append(timestampColumn).append(" < ?");
            parameters.add(to.plusDays(1).atStartOfDay());
        }
    }
}
