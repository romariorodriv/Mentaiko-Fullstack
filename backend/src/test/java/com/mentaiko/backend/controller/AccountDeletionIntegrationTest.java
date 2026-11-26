package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.MicroActivity;
import com.mentaiko.backend.entity.FocusSession;
import com.mentaiko.backend.repository.EmotionRepository;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.repository.FocusSessionRepository;
import java.util.UUID;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.security.JwtService;

/** Tests against an isolated in-memory database; never removes production data. */
@org.springframework.context.annotation.Import(com.mentaiko.backend.config.TestMailConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AccountDeletionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EmotionRepository emotions;
    @Autowired EmotionalEntryRepository checkins;
    @Autowired MicroActivityRepository activities;
    @Autowired FocusSessionRepository sessions;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setupSqlOnlyTables() {
        // Hibernate creates entity tables in H2. These three are managed by explicit SQL in production.
        jdbc.execute("CREATE TABLE IF NOT EXISTS reminder_preferences(user_id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS sleep_entries(user_id BIGINT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS energy_entries(user_id BIGINT NOT NULL)");
    }

    private User user(String email) {
        User u = new User();
        u.setName("QA test");
        u.setEmail(email);
        u.setPasswordHash(encoder.encode("Password-qa-2026!"));
        u.setBirthDate(LocalDate.of(2000, 1, 1));
        u.setUniversity("QA University");
        u.setCareer("Software");
        u.setRole(Role.USER);
        u.setActive(true);
        return users.saveAndFlush(u);
    }

    @Test
    void deletesOnlyOwnAccountAfterPasswordConfirmationAndRevokesJwt() throws Exception {
        User owner = user("hu32-owner@example.invalid");
        User other = user("hu32-other@example.invalid");
        Emotion emotion = new Emotion();
        emotion.setName("HU32 emocional");
        emotion.setNormalizedName("hu32 emocional");
        emotion.setActive(true);
        emotion = emotions.saveAndFlush(emotion);
        EmotionalEntry ownCheckin = new EmotionalEntry();
        ownCheckin.setUser(owner);
        ownCheckin.setEmotion(emotion);
        ownCheckin.setIntensity(3);
        ownCheckin.setContext("QA");
        ownCheckin = checkins.saveAndFlush(ownCheckin);
        EmotionalEntry otherCheckin = new EmotionalEntry();
        otherCheckin.setUser(other);
        otherCheckin.setEmotion(emotion);
        otherCheckin.setIntensity(4);
        otherCheckin.setContext("QA");
        otherCheckin = checkins.saveAndFlush(otherCheckin);
        MicroActivity activity = new MicroActivity();
        activity.setTitle("HU32 pausa");
        activity.setNormalizedTitle("hu32 pausa");
        activity.setDescription("Prueba de integridad referencial");
        activity.setDurationMinutes(2);
        activity = activities.saveAndFlush(activity);
        FocusSession ownSession = sessions.saveAndFlush(new FocusSession(owner, activity, null, UUID.randomUUID(), 120));
        Long ownCheckinId = ownCheckin.getId();
        Long otherCheckinId = otherCheckin.getId();
        Long ownSessionId = ownSession.getId();
        String bearer = "Bearer " + jwt.generateToken(owner);

        mvc.perform(delete("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(users.existsById(owner.getId())).isTrue();

        mvc.perform(delete("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"Password-qa-2026!\"}"))
                .andExpect(status().isNoContent());

        assertThat(users.existsById(owner.getId())).isFalse();
        assertThat(users.existsById(other.getId())).isTrue();
        assertThat(checkins.existsById(ownCheckinId)).isFalse();
        assertThat(checkins.existsById(otherCheckinId)).isTrue();
        assertThat(sessions.existsById(ownSessionId)).isFalse();
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletionRejectsAnonymousRequests() throws Exception {
        mvc.perform(delete("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"Password-qa-2026!\"}"))
                .andExpect(status().isUnauthorized());
    }
}
