package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.mentaiko.backend.config.TestMailConfiguration;
import com.mentaiko.backend.entity.ActivityRecommendation;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.MicroActivity;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.ActivityRecommendationRepository;
import com.mentaiko.backend.repository.EmotionRepository;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.FocusSessionRepository;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.security.JwtService;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:focus_sessions_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Import(TestMailConfiguration.class)
class FocusSessionIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private FocusSessionRepository sessions;
    @Autowired private ActivityRecommendationRepository recommendations;
    @Autowired private MicroActivityRepository activities;
    @Autowired private EmotionalEntryRepository checkins;
    @Autowired private EmotionRepository emotions;
    @Autowired private UserRepository users;
    @Autowired private JwtService jwt;

    private User owner;
    private MicroActivity activity;

    @BeforeEach
    void setUp() {
        sessions.deleteAll();
        recommendations.deleteAll();
        checkins.deleteAll();
        activities.deleteAll();
        users.deleteAll();
        owner = saveUser("focus-owner@example.invalid");
        activity = saveActivity("Pausa breve", true);
    }

    @Test
    void retrievesTheGuideForAnActiveActivityAndRequiresAuthentication() throws Exception {
        var inactive = saveActivity("Pausa desactivada", false);
        mvc.perform(get("/api/micro-activities/{id}", activity.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/micro-activities/{id}", activity.getId()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(2))
                .andExpect(jsonPath("$.steps.length()").value(3))
                .andExpect(jsonPath("$.steps[1].instruction").value(activity.getDescription()));
        mvc.perform(get("/api/micro-activities/{id}", inactive.getId()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/focus-sessions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        assertThat(sessions.count()).isZero();
    }

    @Test
    void confirmsAnIndependentSessionAndReusesTheRecordedResultOnRetry() throws Exception {
        UUID requestId = UUID.randomUUID();
        mvc.perform(confirm(owner, requestId, activity.getId(), null, 60)).andExpect(status().isConflict());
        assertThat(sessions.count()).isZero();

        mvc.perform(confirm(owner, requestId, activity.getId(), null, 120))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityId").value(activity.getId()))
                .andExpect(jsonPath("$.durationSeconds").value(120))
                .andExpect(jsonPath("$.completedAt").exists());
        var saved = sessions.findAll().getFirst();
        mvc.perform(confirm(owner, requestId, activity.getId(), null, 120))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId()));
        mvc.perform(confirm(owner, requestId, saveActivity("Otra pausa", true).getId(), null, 120))
                .andExpect(status().isConflict());
        assertThat(sessions.count()).isEqualTo(1);
    }

    @Test
    void completesOnlyAnOwnedMatchingRecommendationAndKeepsTheSessionAfterCheckinDeletion() throws Exception {
        var checkin = new EmotionalEntry();
        checkin.setUser(owner);
        checkin.setEmotion(emotions.findAll().getFirst());
        checkin.setIntensity(3);
        checkin.setContext("Estudio");
        checkins.save(checkin);
        var recommendation = new ActivityRecommendation();
        recommendation.setUser(owner);
        recommendation.setCheckin(checkin);
        recommendation.setActivity(activity);
        recommendation.setReason("Para una pausa breve");
        recommendations.save(recommendation);

        mvc.perform(confirm(saveUser("focus-other@example.invalid"), UUID.randomUUID(), activity.getId(), recommendation.getId(), 120))
                .andExpect(status().isNotFound());
        mvc.perform(confirm(owner, UUID.randomUUID(), saveActivity("Actividad distinta", true).getId(), recommendation.getId(), 120))
                .andExpect(status().isBadRequest());
        assertThat(recommendations.findById(recommendation.getId()).orElseThrow().isCompleted()).isFalse();
        assertThat(sessions.count()).isZero();

        mvc.perform(confirm(owner, UUID.randomUUID(), activity.getId(), recommendation.getId(), 120))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recommendationId").value(recommendation.getId()));
        assertThat(recommendations.findById(recommendation.getId()).orElseThrow().isCompleted()).isTrue();

        mvc.perform(delete("/api/checkins/{id}", checkin.getId()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());
        assertThat(sessions.findAll().getFirst().getRecommendationId()).isNull();
    }

    private MockHttpServletRequestBuilder confirm(User user, UUID requestId, Long activityId, Long recommendationId, int seconds) {
        return post("/api/focus-sessions").header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"requestId":"%s","activityId":%d,"recommendationId":%s,"durationSeconds":%d}
                        """.formatted(requestId, activityId, recommendationId, seconds));
    }

    private User saveUser(String email) {
        var user = new User();
        user.setName("Prueba guiada");
        user.setEmail(email);
        user.setPasswordHash("unused");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setUniversity("Universidad");
        user.setCareer("Sistemas");
        user.setRole(Role.USER);
        return users.save(user);
    }

    private MicroActivity saveActivity(String title, boolean active) {
        var item = new MicroActivity();
        item.setTitle(title);
        item.setNormalizedTitle(title);
        item.setDescription("Respira con calma a tu propio ritmo.");
        item.setDurationMinutes(2);
        item.setActive(active);
        return activities.save(item);
    }

    private String bearer(User user) {
        return "Bearer " + jwt.generateToken(user);
    }
}
