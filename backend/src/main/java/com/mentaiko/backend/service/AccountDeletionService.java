package com.mentaiko.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.repository.UserRepository;

/**
 * HU-32. Deletes only the authenticated account and its dependent data.
 * Keep the explicit deletion order in sync with new user-owned tables.
 */
@Service
public class AccountDeletionService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;

    public AccountDeletionService(UserRepository users, PasswordEncoder encoder, JdbcTemplate jdbc) {
        this.users = users;
        this.encoder = encoder;
        this.jdbc = jdbc;
    }

    @Transactional
    public void deleteOwnAccount(String authenticatedEmail, String password) {
        User user = users.findLockedByEmail(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));
        if (!user.isActive() || password == null || !encoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }
        Long userId = user.getId();
        // focus_sessions has NO ON DELETE CASCADE for user_id; delete it before recommendations.
        jdbc.update("DELETE FROM focus_sessions WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM activity_recommendations WHERE user_id = ?", userId);
        // checkin_factors is cascaded by checkins(id) in migration 006.
        jdbc.update("DELETE FROM checkins WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM user_favorite_activities WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM activity_preferences WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM reminder_preferences WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM weekly_goals WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM sleep_entries WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM energy_entries WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM password_reset_tokens WHERE user_id = ?", userId);
        // The JWT filter resolves the subject from the database on every request:
        // once this transaction commits, all previously issued tokens are rejected.
        users.delete(user);
        users.flush();
    }
}
