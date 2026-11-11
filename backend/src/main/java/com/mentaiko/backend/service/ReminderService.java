package com.mentaiko.backend.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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
public class ReminderService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final Clock clock;

    public record Preference(boolean enabled, LocalTime time, List<Integer> days, boolean due) {}
    public record Update(boolean enabled, LocalTime time, List<Integer> days) {}
    private record Row(boolean enabled, LocalTime time, String days, LocalDate acknowledged) {}

    public ReminderService(JdbcTemplate jdbc, UserRepository users, Clock clock) {
        this.jdbc = jdbc; this.users = users; this.clock = clock;
    }

    private User user(String email, boolean lock) {
        var found = lock ? users.findLockedByEmail(email) : users.findByEmailIgnoreCase(email);
        return found.filter(User::isActive).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));
    }

    private LocalDate today() {return LocalDate.now(clock.withZone(LIMA));}
    private LocalTime timeNow() {return LocalTime.now(clock.withZone(LIMA));}

    private Row row(Long userId) {
        List<Row> rows=jdbc.query("""
            SELECT enabled, local_time, days_of_week, last_acknowledged_date
            FROM reminder_preferences WHERE user_id=?
            """, (rs, i) -> new Row(rs.getBoolean(1), rs.getTime(2).toLocalTime(), rs.getString(3),
                rs.getDate(4)==null?null:rs.getDate(4).toLocalDate()), userId);
        return rows.isEmpty()?new Row(false,LocalTime.of(20,0),"1111111",null):rows.get(0);
    }

    private boolean due(Row row) {
        if(!row.enabled || today().equals(row.acknowledged)) return false;
        DayOfWeek day=today().getDayOfWeek();
        return row.days.charAt(day.getValue()-1)=='1' && !timeNow().isBefore(row.time);
    }

    private Preference response(Row row) {
        var selected=new java.util.ArrayList<Integer>();
        for(int i=0;i<7;i++) if(row.days.charAt(i)=='1')selected.add(i+1);
        return new Preference(row.enabled,row.time,selected,due(row));
    }

    @Transactional(readOnly=true)
    public Preference get(String email) {
        return response(row(user(email,false).getId()));
    }

    @Transactional
    public Preference update(String email, Update request) {
        if(request==null || request.time()==null || request.days()==null
          || request.days().isEmpty() || request.days().size()>7
          || request.days().stream().anyMatch(d -> d==null || d<1 || d>7)
          || request.days().stream().distinct().count()!=request.days().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona horario y días válidos");
        }
        var user=user(email,true);
        var bits=new StringBuilder("0000000");
        request.days().forEach(d->bits.setCharAt(d-1,'1'));
        jdbc.update("""
            INSERT INTO reminder_preferences(user_id,enabled,local_time,days_of_week)
            VALUES(?,?,?,?)
            ON CONFLICT(user_id) DO UPDATE
            SET enabled=EXCLUDED.enabled,local_time=EXCLUDED.local_time,days_of_week=EXCLUDED.days_of_week
            """, user.getId(),request.enabled(),java.sql.Time.valueOf(request.time()),bits.toString());
        return response(row(user.getId()));
    }

    @Transactional
    public Preference acknowledge(String email) {
        var user=user(email,true);
        var current=row(user.getId());
        if(due(current)) {
            jdbc.update("UPDATE reminder_preferences SET last_acknowledged_date=? WHERE user_id=?",
                today(),user.getId());
        }
        return response(row(user.getId()));
    }
}
