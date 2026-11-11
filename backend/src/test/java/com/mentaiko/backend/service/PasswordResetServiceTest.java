package com.mentaiko.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.mentaiko.backend.entity.PasswordResetToken;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.repository.PasswordResetTokenRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.security.JwtService;

@SpringBootTest(properties = {
    "spring.datasource.url=${password-reset.test-url:jdbc:h2:mem:password_reset_service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.driver-class-name=${password-reset.test-driver:org.h2.Driver}",
    "spring.datasource.username=${password-reset.test-user:sa}"
})
class PasswordResetServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");
    private static final String PASSWORD = "NuevaClave123!";
    @Autowired PasswordResetService service;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @MockitoBean JavaMailSender mail;
    @MockitoBean(name = "passwordResetExecutor") TaskExecutor executor;
    @MockitoBean Clock clock;
    private User user;

    @BeforeEach
    void setup() {
        tokens.deleteAll();
        users.deleteAll();
        reset(mail, executor);
        when(clock.instant()).thenReturn(NOW);
        doAnswer(invocation -> { invocation.getArgument(0, Runnable.class).run(); return null; }).when(executor).execute(any(Runnable.class));
        user = new User();
        user.setName("Prueba"); user.setEmail("recovery@example.invalid");
        user.setPasswordHash(encoder.encode("Anterior123!"));
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setUniversity("UPC"); user.setCareer("Software");
        user = users.saveAndFlush(user);
    }

    @AfterEach
    void clean() { tokens.deleteAll(); users.deleteAll(); }

    @Test
    void generatesRandomHashedTokensWithExactExpiryAndPublicLink() throws Exception {
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(tokens.count()).isEqualTo(1); // committed BEFORE SMTP
            return null;
        }).when(mail).send(any(SimpleMailMessage.class));
        service.requestReset(" RECOVERY@EXAMPLE.INVALID ");
        String raw = deliveredToken();
        assertThat(raw).matches("[A-Za-z0-9_-]{43}");
        assertThat(tokens.findUserIdByTokenHash(hash(raw))).contains(user.getId());
        PasswordResetToken saved = tokens.findAll().getFirst();
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(saved.getUsedAt()).isNull();
        doNothing().when(mail).send(any(SimpleMailMessage.class));
        when(clock.instant()).thenReturn(NOW.plusSeconds(121));
        service.requestReset(user.getEmail());
        ArgumentCaptor<SimpleMailMessage> messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, times(2)).send(messages.capture());
        assertThat(messages.getAllValues().get(0).getText()).isNotEqualTo(messages.getAllValues().get(1).getText());
    }

    @Test
    void doesNotPerformLookupOrSmtpBeforeQueuedWorkRuns() {
        doNothing().when(executor).execute(any(Runnable.class));
        String response = service.requestReset(user.getEmail());
        assertThat(response).startsWith("Si el correo");
        assertThat(tokens.count()).isZero();
        verifyNoInteractions(mail);
        ArgumentCaptor<Runnable> work = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(work.capture());
        work.getValue().run();
        verify(mail).send(any(SimpleMailMessage.class));
    }

    @Test
    void boundedQueueRejectionStillReturnsGenericMessage() {
        doThrow(new TaskRejectedException("full")).when(executor).execute(any(Runnable.class));
        assertThat(service.requestReset(user.getEmail())).startsWith("Si el correo");
        assertThat(tokens.count()).isZero();
        verifyNoInteractions(mail);
    }

    @Test
    void unknownAndInactiveAccountsHaveSameResponseAndNoMail() {
        String unknown = service.requestReset("unknown@example.invalid");
        user.setActive(false); users.saveAndFlush(user);
        assertThat(service.requestReset(user.getEmail())).isEqualTo(unknown);
        assertThat(tokens.count()).isZero();
        verifyNoInteractions(mail);
    }

    @Test
    void cooldownSurvivesSmtpFailureAndFailedTokenIsRemoved() {
        doThrow(new MailSendException("simulated provider failure")).when(mail).send(any(SimpleMailMessage.class));
        String response = service.requestReset(user.getEmail());
        assertThat(tokens.count()).isZero();
        assertThat(users.findById(user.getId()).orElseThrow().getPasswordResetRequestedAt()).isEqualTo(NOW);
        assertThat(service.requestReset(user.getEmail())).isEqualTo(response);
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
        when(clock.instant()).thenReturn(NOW.plusSeconds(120));
        service.requestReset(user.getEmail());
        verify(mail, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void resetsPasswordConsumesAllLinksAndRevokesOldJwt() throws Exception {
        String oldJwt = jwt.generateToken(user);
        String first = seed("a", NOW.plusSeconds(900));
        String other = seed("b", NOW.plusSeconds(900));
        service.reset(first, PASSWORD);
        User updated = users.findById(user.getId()).orElseThrow();
        assertThat(encoder.matches(PASSWORD, updated.getPasswordHash())).isTrue();
        assertThat(updated.getAuthVersion()).isEqualTo(1);
        assertThat(jwt.isTokenValid(oldJwt, updated)).isFalse();
        assertThat(jwt.isTokenValid(jwt.generateToken(updated), updated)).isTrue();
        assertInvalid(first);
        assertInvalid(other);
        assertThat(users.findById(user.getId()).orElseThrow().getAuthVersion()).isEqualTo(1);
    }

    @Test
    void rejectsExpiredExactlyAtExpiryMalformedUnknownAndInactiveTokens() throws Exception {
        String expired = seed("a", NOW.minusSeconds(1));
        String boundary = seed("b", NOW);
        assertInvalid(expired); assertInvalid(boundary); assertInvalid("bad"); assertInvalid(null); assertInvalid("z".repeat(43));
        String active = seed("c", NOW.plusSeconds(900));
        user.setActive(false); users.saveAndFlush(user);
        assertInvalid(active);
        assertThat(encoder.matches("Anterior123!", users.findById(user.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void rejectsOverlongUtf8WithoutConsumingLinkAndAccepts72Bytes() throws Exception {
        String raw = seed("a", NOW.plusSeconds(900));
        assertThatThrownBy(() -> service.reset(raw, "é".repeat(37))).isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(400));
        service.reset(raw, "é".repeat(36));
        assertThat(encoder.matches("é".repeat(36), users.findById(user.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void concurrentRequestsSendOnlyOneMail() throws Exception {
        runTogether(() -> { service.requestReset(user.getEmail()); return 1; },
                    () -> { service.requestReset(user.getEmail()); return 1; });
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
        assertThat(tokens.count()).isEqualTo(1);
    }

    @Test
    void concurrentUseOfSameTokenSucceedsOnlyOnce() throws Exception {
        String raw = seed("a", NOW.plusSeconds(900));
        assertThat(runTogether(() -> resetResult(raw), () -> resetResult(raw))).containsExactlyInAnyOrder(200, 400);
    }

    @Test
    void concurrentUseOfDifferentTokensForSameUserSucceedsOnlyOnce() throws Exception {
        String first = seed("a", NOW.plusSeconds(900));
        String second = seed("b", NOW.plusSeconds(900));
        assertThat(runTogether(() -> resetResult(first), () -> resetResult(second))).containsExactlyInAnyOrder(200, 400);
    }

    private int resetResult(String raw) {
        try { service.reset(raw, PASSWORD); return 200; }
        catch (ResponseStatusException invalid) { return invalid.getStatusCode().value(); }
    }

    private List<Integer> runTogether(Callable<Integer> first, Callable<Integer> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> a = pool.submit(() -> { ready.countDown(); go.await(); return first.call(); });
            Future<Integer> b = pool.submit(() -> { ready.countDown(); go.await(); return second.call(); });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); go.countDown();
            return List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
        }
    }

    private void assertInvalid(String raw) {
        assertThatThrownBy(() -> service.reset(raw, PASSWORD)).isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(400));
    }

    private String seed(String letter, Instant expiry) throws Exception {
        String raw = letter.repeat(43);
        tokens.saveAndFlush(new PasswordResetToken(user, hash(raw), NOW, expiry));
        return raw;
    }

    private String deliveredToken() {
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly(user.getEmail());
        var matcher = Pattern.compile("https://mentaiko-app.duckdns.org/restablecer-contrasena\\?token=([A-Za-z0-9_-]{43})").matcher(message.getValue().getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String hash(String raw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    }
}
