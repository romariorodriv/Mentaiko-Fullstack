package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.repository.PasswordResetTokenRepository;
import com.mentaiko.backend.security.JwtService;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:password_reset_http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class PasswordResetIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @MockitoBean JavaMailSender mail;
    @MockitoBean(name = "passwordResetExecutor") TaskExecutor executor;
    private User user;

    @BeforeEach
    void setup() {
        tokens.deleteAll(); users.deleteAll(); reset(mail, executor);
        doAnswer(invocation -> { invocation.getArgument(0, Runnable.class).run(); return null; }).when(executor).execute(any(Runnable.class));
        user = new User(); user.setName("Prueba"); user.setEmail("http@example.invalid");
        user.setPasswordHash(encoder.encode("Anterior123!")); user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setUniversity("UPC"); user.setCareer("Software"); user = users.saveAndFlush(user);
    }
    @AfterEach void cleanup() { tokens.deleteAll(); users.deleteAll(); }

    @Test
    void anonymousRecoveryChangesLoginAndRevokesPreviouslyIssuedJwt() throws Exception {
        String previous = jwt.generateToken(user);
        mvc.perform(json("forgot-password", "{\"email\":\"http@example.invalid\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").isString());
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(message.capture());
        var match = Pattern.compile("token=([A-Za-z0-9_-]{43})").matcher(message.getValue().getText());
        assertThat(match.find()).isTrue(); String token = match.group(1);
        mvc.perform(json("reset-password", "{\"token\":\"" + token + "\",\"newPassword\":\"NuevaClave123!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").isString());
        mvc.perform(json("reset-password", "{\"token\":\"" + token + "\",\"newPassword\":\"OtraClave123!\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + previous)).andExpect(status().isUnauthorized());
        mvc.perform(json("login", "{\"email\":\"http@example.invalid\",\"password\":\"Anterior123!\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(json("login", "{\"email\":\"http@example.invalid\",\"password\":\"NuevaClave123!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
    }

    @Test
    void invalidInputsReturnValidation400IncludingUtf8ByteLimit() throws Exception {
        mvc.perform(json("reset-password", "{\"newPassword\":"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("El cuerpo JSON no es valido"));
        mvc.perform(json("forgot-password", "{\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.email").exists());
        mvc.perform(json("reset-password", "{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.token").exists());
        mvc.perform(json("reset-password", "{\"token\":\"" + "a".repeat(43) + "\",\"newPassword\":\"" + "é".repeat(37) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.newPassword").exists());
        mvc.perform(json("reset-password", "{\"token\":\"bad\",\"newPassword\":\"NuevaClave123!\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("El enlace es invalido o ha vencido"));
        verifyNoInteractions(mail);
    }

    @Test
    void throttlesAbuseWithoutTrustingForwardedFor() throws Exception {
        for (int i = 0; i < 10; i++) {
            mvc.perform(json("forgot-password", "{\"email\":\"missing@example.invalid\"}")
                    .with(request -> { request.setRemoteAddr("198.51.100.10"); return request; }))
                    .andExpect(status().isOk());
        }
        mvc.perform(json("forgot-password", "{\"email\":\"missing@example.invalid\"}")
                .with(request -> { request.setRemoteAddr("198.51.100.10"); return request; })
                .header("X-Forwarded-For", "203.0.113.50"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "900"));
        // Other auth endpoints are unaffected.
        mvc.perform(json("login", "{\"email\":\"http@example.invalid\",\"password\":\"Anterior123!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void corsAllowsBothExistingOriginsForRecovery() throws Exception {
        for (String origin : new String[]{"http://localhost:4200", "https://mentaiko-app.duckdns.org"}) {
            mvc.perform(options("/api/auth/forgot-password").header("Origin", origin)
                    .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "Content-Type"))
                    .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
    }

    private MockHttpServletRequestBuilder json(String endpoint, String body) {
        return post("/api/auth/" + endpoint).contentType("application/json").content(body)
                .with(request -> { request.setRemoteAddr(UUID.randomUUID().toString()); return request; });
    }
}
