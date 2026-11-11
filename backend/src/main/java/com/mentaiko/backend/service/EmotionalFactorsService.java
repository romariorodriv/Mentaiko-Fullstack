package com.mentaiko.backend.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.mentaiko.backend.repository.UserRepository;

@Service
public class EmotionalFactorsService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public record Factor(int id, String code, String label) {}
    public record FactorSummary(int id, String label, long count) {}
    public record FactorEmotion(String factor, String emotion, long count) {}
    public record Analysis(LocalDate from, LocalDate to, List<FactorSummary> factors,
                           List<FactorEmotion> associations) {}

    public EmotionalFactorsService(JdbcTemplate jdbc, UserRepository users) {
        this.jdbc=jdbc; this.users=users;
    }

    private Long userId(String email) {
        return users.findByEmailIgnoreCase(email).filter(u->u.isActive())
            .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sesión no válida")).getId();
    }

    private void checkinOwned(Long userId, Long checkinId) {
        Integer total=jdbc.queryForObject("SELECT COUNT(*) FROM checkins WHERE id=? AND user_id=?",
            Integer.class, checkinId, userId);
        if(total==null || total==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Check-in no encontrado");
    }

    @Transactional(readOnly=true)
    public List<Factor> catalog(String email) {
        userId(email);
        return jdbc.query("SELECT id,code,label FROM emotional_factors ORDER BY id",
            (rs,i)->new Factor(rs.getInt(1),rs.getString(2),rs.getString(3)));
    }

    @Transactional(readOnly=true)
    public List<Integer> selection(String email, Long checkinId) {
        Long user=userId(email);
        checkinOwned(user,checkinId);
        return jdbc.query("SELECT factor_id FROM checkin_factors WHERE checkin_id=? ORDER BY factor_id",
            (rs,i)->rs.getInt(1),checkinId);
    }

    @Transactional
    public List<Integer> replace(String email, Long checkinId, List<Integer> factorIds) {
        Long user=userId(email);
        if(factorIds==null || factorIds.size()>5 || new HashSet<>(factorIds).size()!=factorIds.size()
                || factorIds.stream().anyMatch(id->id==null || id<1 || id>32767)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona hasta cinco factores distintos");
        }
        // Lock the owning check-in before replacing its factors.
        List<Long> owned=jdbc.query("SELECT id FROM checkins WHERE id=? AND user_id=? FOR UPDATE",
            (rs,i)->rs.getLong(1),checkinId,user);
        if(owned.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Check-in no encontrado");
        for(Integer id:factorIds) {
            Integer exists=jdbc.queryForObject("SELECT COUNT(*) FROM emotional_factors WHERE id=?",
                Integer.class,id);
            if(exists==null || exists==0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Factor desconocido");
        }
        jdbc.update("DELETE FROM checkin_factors WHERE checkin_id=?",checkinId);
        for(Integer id:factorIds)
            jdbc.update("INSERT INTO checkin_factors(checkin_id,factor_id) VALUES(?,?)",checkinId,id);
        return selection(email,checkinId);
    }

    @Transactional(readOnly=true)
    public Analysis analysis(String email, LocalDate from, LocalDate to) {
        Long user=userId(email);
        if(from==null || to==null || from.isAfter(to) || from.plusYears(1).isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Rango inválido (máximo un año)");
        }
        List<FactorSummary> summary=jdbc.query("""
            SELECT f.id,f.label,COUNT(*) AS uses
            FROM checkin_factors cf
            JOIN checkins c ON c.id=cf.checkin_id
            JOIN emotional_factors f ON f.id=cf.factor_id
            WHERE c.user_id=? AND c.created_at>=? AND c.created_at<?
            GROUP BY f.id,f.label ORDER BY uses DESC,f.label ASC
            """, (rs,i)->new FactorSummary(rs.getInt(1),rs.getString(2),rs.getLong(3)),
            user,from.atStartOfDay(),to.plusDays(1).atStartOfDay());
        List<FactorEmotion> associations=jdbc.query("""
            SELECT f.label,e.name,COUNT(*) AS uses
            FROM checkin_factors cf
            JOIN checkins c ON c.id=cf.checkin_id
            JOIN emotional_factors f ON f.id=cf.factor_id
            JOIN emotions e ON e.id=c.emotion_id
            WHERE c.user_id=? AND c.created_at>=? AND c.created_at<?
            GROUP BY f.label,e.name ORDER BY uses DESC,f.label ASC,e.name ASC
            """, (rs,i)->new FactorEmotion(rs.getString(1),rs.getString(2),rs.getLong(3)),
            user,from.atStartOfDay(),to.plusDays(1).atStartOfDay());
        return new Analysis(from,to,summary,associations);
    }
}
