package com.mentaiko.backend.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.repository.UserRepository;

@Service
public class DailyWellbeingService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final Clock clock;

    public record Sleep(LocalDate date, BigDecimal hours, int quality) {}
    public record Energy(LocalDate date, int level) {}

    public DailyWellbeingService(JdbcTemplate jdbc, UserRepository users, Clock clock) {
        this.jdbc = jdbc;
        this.users = users;
        this.clock = clock;
    }

    private User user(String email, boolean lock) {
        var found = lock ? users.findLockedByEmail(email) : users.findByEmailIgnoreCase(email);
        return found.filter(User::isActive).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));
    }

    public LocalDate today() { return LocalDate.now(clock.withZone(LIMA)); }

    private void validDate(LocalDate date) {
        if (date == null || date.isAfter(today()) || date.isBefore(today().minusYears(10))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha fuera del rango permitido");
        }
    }

    private void validRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)
                || to.isAfter(today()) || from.isBefore(today().minusYears(10))
                || from.plusYears(1).isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rango inválido (máximo un año)");
        }
    }

    @Transactional(readOnly = true)
    public List<Sleep> sleepHistory(String email, LocalDate from, LocalDate to) {
        validRange(from, to);
        var id = user(email, false).getId();
        return jdbc.query(
          "SELECT entry_date, hours, quality FROM sleep_entries WHERE user_id=? AND entry_date BETWEEN ? AND ? ORDER BY entry_date DESC",
          (rs, n) -> new Sleep(rs.getDate("entry_date").toLocalDate(),
                                rs.getBigDecimal("hours"), rs.getInt("quality")),
          id, from, to);
    }

    @Transactional
    public Sleep saveSleep(String email, LocalDate date, BigDecimal hours, int quality) {
        validDate(date);
        if (hours == null || hours.compareTo(BigDecimal.ZERO) < 0
                || hours.compareTo(new BigDecimal("24")) > 0
                || hours.multiply(new BigDecimal("4")).stripTrailingZeros().scale() > 0
                || quality < 1 || quality > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horas o calidad inválidas");
        }
        var id = user(email, true).getId();
        int updated = jdbc.update(
          "UPDATE sleep_entries SET hours=?, quality=?, updated_at=CURRENT_TIMESTAMP WHERE user_id=? AND entry_date=?",
          hours, quality, id, date);
        if (updated == 0) jdbc.update(
          "INSERT INTO sleep_entries(user_id,entry_date,hours,quality) VALUES(?,?,?,?)",
          id, date, hours, quality);
        return new Sleep(date, hours, quality);
    }

    @Transactional(readOnly = true)
    public List<Energy> energyHistory(String email, LocalDate from, LocalDate to) {
        validRange(from, to);
        var id = user(email, false).getId();
        return jdbc.query(
          "SELECT entry_date, level FROM energy_entries WHERE user_id=? AND entry_date BETWEEN ? AND ? ORDER BY entry_date DESC",
          (rs, n) -> new Energy(rs.getDate("entry_date").toLocalDate(), rs.getInt("level")),
          id, from, to);
    }

    @Transactional
    public Energy saveEnergy(String email, LocalDate date, int level) {
        validDate(date);
        if (level < 1 || level > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nivel de energía inválido");
        }
        var id = user(email, true).getId();
        int updated = jdbc.update(
          "UPDATE energy_entries SET level=?, updated_at=CURRENT_TIMESTAMP WHERE user_id=? AND entry_date=?",
          level, id, date);
        if (updated == 0) jdbc.update(
          "INSERT INTO energy_entries(user_id,entry_date,level) VALUES(?,?,?)",
          id, date, level);
        return new Energy(date, level);
    }
}
