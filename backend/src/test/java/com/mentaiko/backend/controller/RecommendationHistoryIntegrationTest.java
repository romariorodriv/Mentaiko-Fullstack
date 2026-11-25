package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

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

import com.mentaiko.backend.entity.ActivityRecommendation;
import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.MicroActivity;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.RecommendationDiscardReason;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.ActivityRecommendationRepository;
import com.mentaiko.backend.repository.EmotionActivityRuleRepository;
import com.mentaiko.backend.repository.EmotionRepository;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.FavoriteMicroActivityRepository;
import com.mentaiko.backend.repository.FocusSessionRepository;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.repository.PasswordResetTokenRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.repository.WeeklyGoalRepository;
import com.mentaiko.backend.security.JwtService;
import com.mentaiko.backend.service.EmotionService;
import com.mentaiko.backend.service.MicroActivityService;

/** HU-29: GET /api/recommendations/me/history (CA01) y repeticion como sesion nueva (CA03). */
@org.springframework.context.annotation.Import(com.mentaiko.backend.config.TestMailConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RecommendationHistoryIntegrationTest {

    private static final String URL = "/api/recommendations/me/history";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FavoriteMicroActivityRepository favoriteRepository;
    @Autowired private FocusSessionRepository focusSessionRepository;
    @Autowired private ActivityRecommendationRepository recommendationRepository;
    @Autowired private EmotionActivityRuleRepository ruleRepository;
    @Autowired private EmotionalEntryRepository emotionalEntryRepository;
    @Autowired private WeeklyGoalRepository weeklyGoalRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private MicroActivityRepository microActivityRepository;
    @Autowired private EmotionRepository emotionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EmotionService emotionService;
    @Autowired private MicroActivityService microActivityService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private Emotion emotion;
    private MicroActivity breathe;

    @BeforeEach
    void setUp() {
        // Otras clases de prueba comparten la BD H2: limpiar en orden de dependencias antes de borrar usuarios.
        // focus_sessions referencia a recomendaciones, por eso va antes que ellas.
        jdbc.update("DELETE FROM activity_preferences");
        favoriteRepository.deleteAll();
        focusSessionRepository.deleteAll();
        recommendationRepository.deleteAll();
        ruleRepository.deleteAll();
        emotionalEntryRepository.deleteAll();
        weeklyGoalRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        microActivityRepository.deleteAll();
        emotionRepository.deleteAll();
        userRepository.deleteAll();

        emotion = saveEmotion("Ansiedad");
        breathe = saveActivity("Respiracion guiada", true);
    }

    @Test
    void historyRequiresToken() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void historyOnlyContainsCompletedRecommendationsOfTheAuthenticatedUser() throws Exception {
        User userA = saveUser("hu29-user-a@example.com");
        User userB = saveUser("hu29-user-b@example.com");
        ActivityRecommendation completedA = saveCompleted(userA, breathe, LocalDateTime.of(2026, 11, 10, 9, 0));
        saveRecommendation(userA, breathe); // pendiente
        ActivityRecommendation discardedA = saveRecommendation(userA, breathe);
        discardedA.discard(RecommendationDiscardReason.POCO_TIEMPO);
        recommendationRepository.save(discardedA);
        ActivityRecommendation completedB = saveCompleted(userB, breathe, LocalDateTime.of(2026, 11, 11, 9, 0));

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(completedA.getId()))
                .andExpect(jsonPath("$[0].completedAt").exists())
                .andExpect(jsonPath("$[0].activity.title").value("Respiracion guiada"))
                .andExpect(jsonPath("$[0].activity.durationMinutes").value(5));

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(completedB.getId().intValue())));
    }

    @Test
    void userIdParameterIsIgnoredAndNeverExposesAnotherUsersHistory() throws Exception {
        User userA = saveUser("hu29-owner@example.com");
        User userB = saveUser("hu29-intruder@example.com");
        saveCompleted(userA, breathe, LocalDateTime.of(2026, 11, 10, 9, 0));

        mockMvc.perform(get(URL)
                        .param("userId", String.valueOf(userA.getId()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void historyIsOrderedByCompletedAtDescending() throws Exception {
        User user = saveUser("hu29-order@example.com");
        ActivityRecommendation oldest = saveCompleted(user, breathe, LocalDateTime.of(2026, 9, 1, 8, 0));
        ActivityRecommendation newest = saveCompleted(user, breathe, LocalDateTime.of(2026, 11, 20, 18, 30));
        ActivityRecommendation middle = saveCompleted(user, breathe, LocalDateTime.of(2026, 10, 15, 12, 0));

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(
                        newest.getId().intValue(), middle.getId().intValue(), oldest.getId().intValue())));
    }

    @Test
    void dateFilterUsesCompletedAtWithInclusiveBorders() throws Exception {
        User user = saveUser("hu29-range@example.com");
        saveCompleted(user, breathe, LocalDateTime.of(2026, 10, 31, 23, 59, 59));
        ActivityRecommendation firstDay = saveCompleted(user, breathe, LocalDateTime.of(2026, 11, 1, 0, 0, 0));
        ActivityRecommendation middle = saveCompleted(user, breathe, LocalDateTime.of(2026, 11, 15, 12, 0));
        ActivityRecommendation lastDay = saveCompleted(user, breathe, LocalDateTime.of(2026, 11, 30, 23, 59, 59));
        saveCompleted(user, breathe, LocalDateTime.of(2026, 12, 1, 0, 0, 0));

        mockMvc.perform(get(URL)
                        .param("from", "2026-11-01")
                        .param("to", "2026-11-30")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(
                        lastDay.getId().intValue(), middle.getId().intValue(), firstDay.getId().intValue())));

        mockMvc.perform(get(URL)
                        .param("from", "2026-11-30")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get(URL)
                        .param("to", "2026-11-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get(URL)
                        .param("from", "2026-11-15")
                        .param("to", "2026-11-15")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(middle.getId().intValue())));
    }

    @Test
    void fromAfterToReturns400WithGlobalErrorFormat() throws Exception {
        User user = saveUser("hu29-invalid@example.com");

        mockMvc.perform(get(URL)
                        .param("from", "2026-11-30")
                        .param("to", "2026-11-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("La fecha inicial no puede ser posterior a la fecha final"))
                .andExpect(jsonPath("$.path").value(URL));
    }

    @Test
    void historyKeepsRatingAndActivityStateForTheClient() throws Exception {
        User user = saveUser("hu29-rating@example.com");
        MicroActivity retired = saveActivity("Estiramiento retirado", false);
        ActivityRecommendation rated = saveCompleted(user, breathe, LocalDateTime.of(2026, 11, 20, 10, 0));
        rated.rate(4, "Me ayudo");
        recommendationRepository.save(rated);
        saveCompleted(user, retired, LocalDateTime.of(2026, 11, 19, 10, 0));

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].helpfulnessRating").value(4))
                .andExpect(jsonPath("$[0].activity.active").value(true))
                .andExpect(jsonPath("$[1].helpfulnessRating").doesNotExist())
                .andExpect(jsonPath("$[1].activity.active").value(false));
    }

    @Test
    void repeatingWithoutRecommendationIdIsANewSessionAndLeavesTheOriginalUntouched() throws Exception {
        User user = saveUser("hu29-repeat@example.com");
        ActivityRecommendation original = saveCompleted(user, breathe, LocalDateTime.of(2026, 11, 20, 10, 0));
        original.rate(3, "Bien");
        recommendationRepository.save(original);

        mockMvc.perform(post("/api/focus-sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + UUID.randomUUID() + "\",\"activityId\":" + breathe.getId()
                                + ",\"durationSeconds\":" + breathe.getDurationMinutes() * 60 + "}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.recommendationId").doesNotExist());

        ActivityRecommendation after = recommendationRepository.findById(original.getId()).orElseThrow();
        assertThat(after.getCompletedAt()).isEqualTo(LocalDateTime.of(2026, 11, 20, 10, 0));
        assertThat(after.getHelpfulnessRating()).isEqualTo(3);
        assertThat(after.getFeedbackComment()).isEqualTo("Bien");
        assertThat(focusSessionRepository.findAll()).singleElement()
                .satisfies(session -> assertThat(session.getRecommendationId()).isNull());

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].helpfulnessRating").value(3));
    }

    private ActivityRecommendation saveCompleted(User user, MicroActivity activity, LocalDateTime completedAt) {
        ActivityRecommendation recommendation = saveRecommendation(user, activity);
        recommendation.complete();
        recommendationRepository.saveAndFlush(recommendation);
        // complete() usa la hora actual: se fija una fecha concreta para probar el orden y los bordes del rango.
        jdbc.update("UPDATE activity_recommendations SET completed_at = ? WHERE id = ?",
                Timestamp.valueOf(completedAt), recommendation.getId());
        return recommendationRepository.findById(recommendation.getId()).orElseThrow();
    }

    private ActivityRecommendation saveRecommendation(User user, MicroActivity activity) {
        ActivityRecommendation recommendation = new ActivityRecommendation();
        recommendation.setUser(user);
        recommendation.setCheckin(saveCheckin(user));
        recommendation.setActivity(activity);
        recommendation.setReason("Recomendacion de prueba");
        recommendation.setFallbackUsed(false);
        return recommendationRepository.save(recommendation);
    }

    private EmotionalEntry saveCheckin(User user) {
        EmotionalEntry entry = new EmotionalEntry();
        entry.setUser(user);
        entry.setEmotion(emotion);
        entry.setIntensity(4);
        entry.setContext("Estudios");
        entry.setNote("Nota");
        return emotionalEntryRepository.save(entry);
    }

    private User saveUser(String email) {
        User user = new User();
        user.setName("Usuario Historial");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Clave123"));
        user.setBirthDate(LocalDate.of(2000, 5, 20));
        user.setUniversity("UPC");
        user.setCareer("Ingenieria de Software");
        user.setRole(Role.USER);
        user.setActive(true);
        return userRepository.save(user);
    }

    private Emotion saveEmotion(String name) {
        Emotion saved = new Emotion();
        saved.setName(name);
        saved.setNormalizedName(emotionService.normalizeForUniqueness(name));
        saved.setActive(true);
        return emotionRepository.save(saved);
    }

    private MicroActivity saveActivity(String title, boolean active) {
        MicroActivity activity = new MicroActivity();
        activity.setTitle(title);
        activity.setNormalizedTitle(microActivityService.normalizeForUniqueness(title));
        activity.setDescription("Descripcion de " + title);
        activity.setDurationMinutes(5);
        activity.setActive(active);
        return microActivityRepository.save(activity);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
