package com.mentaiko.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;

class JwtServiceTest {

    private static final String SECRET = "test-secret-for-mentaiko-auth-integration-tests-minimum-32-bytes";

    @Test
    void generatesAndValidatesToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        User user = user();

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractSubject(token)).isEqualTo(user.getEmail());
        assertThat(jwtService.extractRole(token)).isEqualTo(Role.USER);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void rejectsInvalidSignature() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken(user()) + "x";

        assertThat(jwtService.isTokenValid(token, user())).isFalse();
        assertThatThrownBy(() -> jwtService.extractSubject(token)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwtService = new JwtService(SECRET, -1_000);
        String token = jwtService.generateToken(user());

        assertThat(jwtService.isTokenValid(token, user())).isFalse();
        assertThatThrownBy(() -> jwtService.extractSubject(token)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void legacyTokensAreAcceptedUntilFirstResetAndProfileChangesDoNotRevokeTokens() {
        JwtService service = new JwtService(SECRET, 60_000);
        User user = user();
        String legacy = io.jsonwebtoken.Jwts.builder().subject(user.getEmail())
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();
        String current = service.generateToken(user);
        user.setName("Perfil actualizado");
        assertThat(service.isTokenValid(current, user)).isTrue();
        assertThat(service.isTokenValid(legacy, user)).isTrue();
        user.invalidateSessions();
        assertThat(service.isTokenValid(legacy, user)).isFalse();
        assertThat(service.isTokenValid(current, user)).isFalse();
        assertThat(service.isTokenValid(service.generateToken(user), user)).isTrue();
    }

    private User user() {
        User user = new User();
        user.setName("Usuario Test");
        user.setEmail("usuario@example.com");
        user.setPasswordHash("hash");
        user.setBirthDate(LocalDate.of(2000, 5, 20));
        user.setUniversity("UPC");
        user.setCareer("Ingenieria de Software");
        user.setRole(Role.USER);
        user.setActive(true);
        return user;
    }
}
