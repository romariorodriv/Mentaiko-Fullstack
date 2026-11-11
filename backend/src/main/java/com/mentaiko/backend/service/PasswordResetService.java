package com.mentaiko.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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
    private record Delivery(String address, String rawToken, String hash) {}
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;
    private final String appUrl;
    private final String sender;
    private final TaskExecutor executor;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
            PasswordEncoder encoder, JavaMailSender mailSender,
            @Value("${app.public-url:http://localhost:4200}") String appUrl,
            @Value("${app.mail.from}") String sender,
            @Qualifier("passwordResetExecutor") TaskExecutor executor,
            PlatformTransactionManager transactionManager, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mailSender = mailSender;
        this.appUrl = appUrl.replaceAll("/+$", "");
        this.sender = sender;
        this.executor = executor;
        this.transactions = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public String requestReset(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        try {
            // Lookup and SMTP never delay the public response based on account existence.
            executor.execute(() -> {
                try { deliver(normalized); }
                catch (RuntimeException error) {
                    // Provider/database exceptions can contain addresses or credentials.
                    log.error("Password reset processing failed ({})", error.getClass().getSimpleName());
                }
            });
        } catch (TaskRejectedException busy) {
            log.warn("Password reset queue is full");
        }
        return GENERIC_MESSAGE;
    }

    private void deliver(String email) {
        Delivery delivery = transactions.execute(status -> {
            User user = users.findLockedByEmail(email).orElse(null);
            if (user == null || !user.isActive()) return null;
            Instant now = clock.instant();
            if (user.getPasswordResetRequestedAt() != null
                    && now.isBefore(user.getPasswordResetRequestedAt().plus(REQUEST_COOLDOWN))) return null;
            // Persist cooldown even if SMTP fails; all workers lock the user first.
            user.setPasswordResetRequestedAt(now);
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            String hash = sha256(raw);
            tokens.save(new PasswordResetToken(user, hash, now, now.plus(TOKEN_VALIDITY)));
            return new Delivery(user.getEmail(), raw, hash);
        });
        if (delivery == null) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(delivery.address());
        message.setSubject("Mentaiko - Recupera tu contrasena");
        message.setText("Recibimos una solicitud para restablecer tu contrasena.\n"
                + "Abre el siguiente enlace (valido por 15 minutos y una sola vez):\n"
                + appUrl + "/restablecer-contrasena?token=" + delivery.rawToken()
                + "\n\nSi no solicitaste este cambio, ignora este mensaje.\nEquipo Mentaiko");
        // Token is committed before sending. No database connection is held during SMTP.
        try {
            mailSender.send(message);
        } catch (MailException failure) {
            transactions.executeWithoutResult(status -> tokens.deleteByTokenHash(delivery.hash()));
            log.error("Password reset mail delivery failed ({})", failure.getClass().getSimpleName());
        }
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 8
                || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usa al menos 8 caracteres y como maximo 72 bytes UTF-8");
        }
        if (rawToken == null || !rawToken.matches("[A-Za-z0-9_-]{43}")) throw invalidToken();
        String hash = sha256(rawToken);
        Long userId = tokens.findUserIdByTokenHash(hash).orElseThrow(this::invalidToken);
        // One lock order serializes resets using different links for the same user too.
        User user = users.findLockedById(userId).orElseThrow(this::invalidToken);
        PasswordResetToken token = tokens.findByTokenHash(hash).orElseThrow(this::invalidToken);
        Instant now = clock.instant();
        if (token.getUsedAt() != null || !now.isBefore(token.getExpiresAt()) || !user.isActive()) throw invalidToken();
        user.setPasswordHash(encoder.encode(newPassword));
        user.invalidateSessions();
        for (PasswordResetToken pending : tokens.findAllByUserIdAndUsedAtIsNull(userId)) pending.setUsedAt(now);
        log.info("Password reset completed for user id={}", userId);
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace es invalido o ha vencido");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
