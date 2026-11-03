package com.mentaiko.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mentaiko.backend.entity.PasswordResetToken;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.repository.PasswordResetTokenRepository;
import com.mentaiko.backend.repository.UserRepository;

@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(15);
    private static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(2);
    private static final String GENERIC_MESSAGE = "Si el correo esta registrado, recibiras instrucciones para restablecer tu contrasena.";

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;
    private final String appUrl;
    private final String sender;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
                                PasswordEncoder encoder, JavaMailSender mailSender,
                                @Value("${app.public-url:http://localhost:4200}") String appUrl,
                                @Value("${app.mail.from}") String sender) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mailSender = mailSender;
        this.appUrl = appUrl.replaceAll("/+$", "");
        this.sender = sender;
    }

    @Transactional
    public String requestReset(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmailIgnoreCase(normalized).orElse(null);
        if (user == null || !user.isActive()) return GENERIC_MESSAGE;

        Instant now = Instant.now();
        if (tokens.existsByUserIdAndUsedAtIsNullAndCreatedAtAfter(user.getId(), now.minus(REQUEST_COOLDOWN))) {
            return GENERIC_MESSAGE;
        }

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new PasswordResetToken(user, sha256(rawToken), now, now.plus(TOKEN_VALIDITY)));

        SimpleMailMessage emailMessage = new SimpleMailMessage();
        emailMessage.setFrom(sender);
        emailMessage.setTo(user.getEmail());
        emailMessage.setSubject("Mentaiko - Recupera tu contrasena");
        emailMessage.setText("Hola " + user.getName() + ",\n\n"
                + "Recibimos una solicitud para restablecer tu contrasena.\n"
                + "Abre el siguiente enlace (valido por 15 minutos y una sola vez):\n"
                + appUrl + "/restablecer-contrasena?token=" + rawToken + "\n\n"
                + "Si no solicitaste este cambio, ignora este mensaje.\n\nEquipo Mentaiko");
        // El envío ocurre dentro de la transacción: si falla, el token no queda guardado.
        try {
            mailSender.send(emailMessage);
        } catch (MailException error) {
            // Respuesta generica incluso si falla SMTP: no revelar si existe una cuenta.
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Password reset mail delivery failed", error);
            return GENERIC_MESSAGE;
        }
        log.info("Password reset email requested for user id={}", user.getId());
        return GENERIC_MESSAGE;
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.length() > 256) throw invalidToken();
        PasswordResetToken token = tokens.findByTokenHash(sha256(rawToken)).orElseThrow(this::invalidToken);
        if (token.getUsedAt() != null || !Instant.now().isBefore(token.getExpiresAt()) || !token.getUser().isActive()) {
            throw invalidToken();
        }
        User user = token.getUser();
        user.setPasswordHash(encoder.encode(newPassword));
        // Invalidar todos los enlaces pendientes del usuario después del cambio.
        for (PasswordResetToken pending : tokens.findAllByUserIdAndUsedAtIsNull(user.getId())) {
            pending.setUsedAt(Instant.now());
        }
        log.info("Password reset completed for user id={}", user.getId());
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace es invalido o ha vencido");
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
