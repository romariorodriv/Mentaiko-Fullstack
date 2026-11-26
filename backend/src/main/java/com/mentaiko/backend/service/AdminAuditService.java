package com.mentaiko.backend.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.repository.UserRepository;

/** HU-34: audit rows participate in the SAME transaction as the business change. */
@Service
public class AdminAuditService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public AdminAuditService(JdbcTemplate jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String resourceType, Long resourceId, String before, String after) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }
        Long actorId = users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado"))
                .getId();
        jdbc.update("INSERT INTO admin_audit_events(actor_user_id, action, resource_type, resource_id, before_summary, after_summary, occurred_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                actorId, action, resourceType, resourceId, before, after);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(int limit, int offset, String resourceType) {
        if (limit < 1 || limit > 100 || offset < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginacion invalida");
        }
        if (resourceType == null || resourceType.isBlank()) {
            return jdbc.queryForList("SELECT e.id, e.actor_user_id, u.name AS actor_name, e.action, e.resource_type, e.resource_id, e.before_summary, e.after_summary, e.occurred_at FROM admin_audit_events e LEFT JOIN users u ON u.id=e.actor_user_id ORDER BY e.occurred_at DESC, e.id DESC LIMIT ? OFFSET ?", limit, offset);
        }
        if (!List.of("EMOTION", "MICRO_ACTIVITY", "RULE").contains(resourceType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recurso invalido");
        }
        return jdbc.queryForList("SELECT e.id, e.actor_user_id, u.name AS actor_name, e.action, e.resource_type, e.resource_id, e.before_summary, e.after_summary, e.occurred_at FROM admin_audit_events e LEFT JOIN users u ON u.id=e.actor_user_id WHERE e.resource_type = ? ORDER BY e.occurred_at DESC, e.id DESC LIMIT ? OFFSET ?", resourceType, limit, offset);
    }
}
