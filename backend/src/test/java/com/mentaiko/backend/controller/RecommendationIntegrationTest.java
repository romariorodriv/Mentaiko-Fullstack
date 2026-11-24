package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionActivityRule;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.ActivityRecommendation;
import com.mentaiko.backend.entity.MicroActivity;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.ActivityRecommendationRepository;
import com.mentaiko.backend.repository.EmotionActivityRuleRepository;
import com.mentaiko.backend.repository.EmotionRepository;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.security.JwtService;
import com.mentaiko.backend.service.EmotionService;
import com.mentaiko.backend.service.MicroActivityService;

@org.springframework.context.annotation.Import(com.mentaiko.backend.config.TestMailConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RecommendationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActivityRecommendationRepository recommendationRepository;

    @Autowired
    private EmotionActivityRuleRepository ruleRepository;

    @Autowired
    private EmotionalEntryRepository emotionalEntryRepository;

    @Autowired
    private EmotionRepository emotionRepository;

    @Autowired
    private MicroActivityRepository microActivityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmotionService emotionService;

    @Autowired
    private MicroActivityService microActivityService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM activity_preferences");
        recommendationRepository.deleteAll();
        ruleRepository.deleteAll();
        emotionalEntryRepository.deleteAll();
        microActivityRepository.deleteAll();
        emotionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void recommendRequiresToken() throws Exception {
        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkinId\":1}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/recommendations/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recommendationUsesMatchingActiveRuleForOwnCheckin() throws Exception {
        User user = saveUser("own-rule@example.com");
        Emotion emotion = saveEmotion("Ansiedad", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity activity = saveActivity("Respiracion guiada", true);
        saveRule(emotion, activity, 1, true);

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkinId\":" + checkin.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkinId").value(checkin.getId()))
                .andExpect(jsonPath("$.activity.id").value(activity.getId()))
                .andExpect(jsonPath("$.activity.title").value("Respiracion guiada"))
                .andExpect(jsonPath("$.fallbackUsed").value(false))
                .andExpect(jsonPath("$.reason").value("Recomendacion asociada a la emocion Ansiedad."))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.activity.normalizedTitle").doesNotExist());
    }

    @Test
    void recommendationFallsBackToFirstActiveActivityWhenNoRuleExists() throws Exception {
        User user = saveUser("fallback@example.com");
        Emotion emotion = saveEmotion("Tristeza", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        saveActivity("Z actividad inactiva", false);
        MicroActivity firstActive = saveActivity("Caminar", true);
        saveActivity("Respirar", true);

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkinId\":" + checkin.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activity.id").value(firstActive.getId()))
                .andExpect(jsonPath("$.fallbackUsed").value(true));
    }

    @Test
    void inactiveRuleActivityIsIgnored() throws Exception {
        User user = saveUser("inactive-rule@example.com");
        Emotion emotion = saveEmotion("Estres", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity inactive = saveActivity("Inactiva", false);
        MicroActivity activeFallback = saveActivity("Activa", true);
        saveRule(emotion, inactive, 1, true);

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkinId\":" + checkin.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activity.id").value(activeFallback.getId()))
                .andExpect(jsonPath("$.fallbackUsed").value(true));
    }

    @Test
    void cannotRecommendFromOtherUsersCheckin() throws Exception {
        User owner = saveUser("owner-rec@example.com");
        User other = saveUser("other-rec@example.com");
        Emotion emotion = saveEmotion("Calma", true);
        EmotionalEntry checkin = saveCheckin(other, emotion);
        saveActivity("Respirar", true);

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkinId\":" + checkin.getId() + "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Check-in no encontrado"));
    }

    @Test
    void recommendationIsIdempotentForSameCheckinAndListIsUserScoped() throws Exception {
        User owner = saveUser("owner-list@example.com");
        User other = saveUser("other-list@example.com");
        Emotion emotion = saveEmotion("Alegria", true);
        EmotionalEntry ownCheckin = saveCheckin(owner, emotion);
        EmotionalEntry otherCheckin = saveCheckin(other, emotion);
        saveActivity("Respirar", true);

        String body = "{\"checkinId\":" + ownCheckin.getId() + "}";
        String token = bearer(owner);

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkinId\":" + otherCheckin.getId() + "}"))
                .andExpect(status().isCreated());

        assertThat(recommendationRepository.findAll()).hasSize(2);

        mockMvc.perform(get("/api/recommendations/me")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].checkinId").value(ownCheckin.getId()));
    }

    @Test
    void recommendationFailsWhenThereAreNoActiveActivities() throws Exception {
        User user = saveUser("no-activities@example.com");
        Emotion emotion = saveEmotion("Cansancio", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        saveActivity("Inactiva", false);

        mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkinId\":" + checkin.getId() + "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No hay microactividades activas disponibles"));
    }

    @Test
    void completeRecommendationRequiresToken() throws Exception {
        mockMvc.perform(post("/api/recommendations/1/complete"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanCompleteRecommendationAndOperationIsIdempotent() throws Exception {
        User user = saveUser("complete-owner@example.com");
        Emotion emotion = saveEmotion("Ansiedad", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity activity = saveActivity("Respirar", true);
        ActivityRecommendation recommendation = saveRecommendation(user, checkin, activity);

        mockMvc.perform(post("/api/recommendations/{id}/complete", recommendation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recommendation.getId()))
                .andExpect(jsonPath("$.completedAt").exists());

        java.time.LocalDateTime firstCompletedAt = recommendationRepository.findById(recommendation.getId())
                .orElseThrow()
                .getCompletedAt();

        mockMvc.perform(post("/api/recommendations/{id}/complete", recommendation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").exists());

        java.time.LocalDateTime secondCompletedAt = recommendationRepository.findById(recommendation.getId())
                .orElseThrow()
                .getCompletedAt();

        assertThat(secondCompletedAt).isEqualTo(firstCompletedAt);
    }

    @Test
    void cannotCompleteOtherUsersRecommendation() throws Exception {
        User owner = saveUser("complete-real-owner@example.com");
        User other = saveUser("complete-other@example.com");
        Emotion emotion = saveEmotion("Calma", true);
        EmotionalEntry checkin = saveCheckin(owner, emotion);
        MicroActivity activity = saveActivity("Respirar", true);
        ActivityRecommendation recommendation = saveRecommendation(owner, checkin, activity);

        mockMvc.perform(post("/api/recommendations/{id}/complete", recommendation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recomendacion no encontrada"));

        assertThat(recommendationRepository.findById(recommendation.getId()).orElseThrow().getCompletedAt())
                .isNull();
    }

    @Test
    void completeWithPostIntensityStoresItNextToTheCheckinIntensity() throws Exception {
        User user = saveUser("post-intensity-owner@example.com");
        Emotion emotion = saveEmotion("Ansiedad", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity activity = saveActivity("Respirar", true);
        ActivityRecommendation recommendation = saveRecommendation(user, checkin, activity);

        mockMvc.perform(post("/api/recommendations/{id}/complete", recommendation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postIntensity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").exists())
                .andExpect(jsonPath("$.preIntensity").value(4))
                .andExpect(jsonPath("$.postIntensity").value(2));

        ActivityRecommendation saved = recommendationRepository.findById(recommendation.getId()).orElseThrow();
        assertThat(saved.getPostIntensity()).isEqualTo(2);

        mockMvc.perform(get("/api/recommendations/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].preIntensity").value(4))
                .andExpect(jsonPath("$[0].postIntensity").value(2));
    }

    @Test
    void completeWithoutPostIntensityIsStillAllowed() throws Exception {
        User user = saveUser("post-intensity-omitted@example.com");
        Emotion emotion = saveEmotion("Calma", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity activity = saveActivity("Estirar", true);
        ActivityRecommendation withEmptyJson = saveRecommendation(user, checkin, activity);

        mockMvc.perform(post("/api/recommendations/{id}/complete", withEmptyJson.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").exists())
                .andExpect(jsonPath("$.preIntensity").value(4))
                .andExpect(jsonPath("$.postIntensity").doesNotExist());

        assertThat(recommendationRepository.findById(withEmptyJson.getId()).orElseThrow().getPostIntensity())
                .isNull();
    }

    @Test
    void postIntensityOutsideOneToFiveIsRejectedAndActivityStaysPending() throws Exception {
        User user = saveUser("post-intensity-invalid@example.com");
        Emotion emotion = saveEmotion("Estres", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity activity = saveActivity("Caminar", true);
        ActivityRecommendation recommendation = saveRecommendation(user, checkin, activity);

        for (String body : new String[] {"{\"postIntensity\":0}", "{\"postIntensity\":6}"}) {
            mockMvc.perform(post("/api/recommendations/{id}/complete", recommendation.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        ActivityRecommendation saved = recommendationRepository.findById(recommendation.getId()).orElseThrow();
        assertThat(saved.getCompletedAt()).isNull();
        assertThat(saved.getPostIntensity()).isNull();
    }

    @Test
    void postIntensityCanBeAddedLaterWithoutChangingCompletionTime() throws Exception {
        User user = saveUser("post-intensity-later@example.com");
        Emotion emotion = saveEmotion("Tristeza", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity activity = saveActivity("Escribir", true);
        ActivityRecommendation recommendation = saveCompletedRecommendation(user, checkin, activity);
        java.time.LocalDateTime completedAt = recommendationRepository.findById(recommendation.getId())
                .orElseThrow()
                .getCompletedAt();

        mockMvc.perform(post("/api/recommendations/{id}/complete", recommendation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postIntensity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postIntensity").value(3));

        ActivityRecommendation saved = recommendationRepository.findById(recommendation.getId()).orElseThrow();
        assertThat(saved.getPostIntensity()).isEqualTo(3);
        assertThat(saved.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void otherUserCannotStorePostIntensity() throws Exception {
        User owner = saveUser("post-intensity-real-owner@example.com");
        User other = saveUser("post-intensity-other@example.com");
        Emotion emotion = saveEmotion("Calma", true);
        EmotionalEntry checkin = saveCheckin(owner, emotion);
        MicroActivity activity = saveActivity("Respirar", true);
        ActivityRecommendation recommendation = saveRecommendation(owner, checkin, activity);

        mockMvc.perform(post("/api/recommendations/{id}/complete", recommendation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postIntensity\":2}"))
                .andExpect(status().isNotFound());

        assertThat(recommendationRepository.findById(recommendation.getId()).orElseThrow().getPostIntensity())
                .isNull();
    }

    @Test
    void feedbackRequiresToken() throws Exception {
        mockMvc.perform(post("/api/recommendations/1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"helpfulnessRating\":4}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanRateCompletedRecommendationWithOptionalComment() throws Exception {
        User user = saveUser("feedback-owner@example.com");
        Emotion emotion = saveEmotion("Ansiedad", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity activity = saveActivity("Respirar", true);
        ActivityRecommendation recommendation = saveCompletedRecommendation(user, checkin, activity);

        mockMvc.perform(post("/api/recommendations/{id}/feedback", recommendation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"helpfulnessRating\":5,\"feedbackComment\":\"  Me ayudo a calmarme  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recommendation.getId()))
                .andExpect(jsonPath("$.helpfulnessRating").value(5))
                .andExpect(jsonPath("$.feedbackComment").value("Me ayudo a calmarme"));

        ActivityRecommendation saved = recommendationRepository.findById(recommendation.getId()).orElseThrow();
        assertThat(saved.getHelpfulnessRating()).isEqualTo(5);
        assertThat(saved.getFeedbackComment()).isEqualTo("Me ayudo a calmarme");

        mockMvc.perform(get("/api/recommendations/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].helpfulnessRating").value(5));
    }

    @Test
    void ratingWithoutCommentIsAccepted() throws Exception {
        User user = saveUser("feedback-no-comment@example.com");
        Emotion emotion = saveEmotion("Calma", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity activity = saveActivity("Estirar", true);
        ActivityRecommendation recommendation = saveCompletedRecommendation(user, checkin, activity);

        mockMvc.perform(post("/api/recommendations/{id}/feedback", recommendation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"helpfulnessRating\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.helpfulnessRating").value(3))
                .andExpect(jsonPath("$.feedbackComment").doesNotExist());
    }

    @Test
    void cannotRatePendingRecommendation() throws Exception {
        User user = saveUser("feedback-pending@example.com");
        Emotion emotion = saveEmotion("Estres", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity activity = saveActivity("Caminar", true);
        ActivityRecommendation recommendation = saveRecommendation(user, checkin, activity);

        mockMvc.perform(post("/api/recommendations/{id}/feedback", recommendation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"helpfulnessRating\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Primero debes realizar la actividad para poder valorarla"));

        assertThat(recommendationRepository.findById(recommendation.getId()).orElseThrow().getHelpfulnessRating())
                .isNull();
    }

    @Test
    void invalidRatingOrTooLongCommentIsRejected() throws Exception {
        User user = saveUser("feedback-invalid@example.com");
        Emotion emotion = saveEmotion("Tristeza", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity activity = saveActivity("Escribir", true);
        ActivityRecommendation recommendation = saveCompletedRecommendation(user, checkin, activity);

        for (String body : new String[] {
                "{\"helpfulnessRating\":0}",
                "{\"helpfulnessRating\":6}",
                "{}",
                "{\"helpfulnessRating\":4,\"feedbackComment\":\"" + "a".repeat(301) + "\"}"
        }) {
            mockMvc.perform(post("/api/recommendations/{id}/feedback", recommendation.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/recommendations/{id}/feedback", recommendation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"helpfulnessRating\":4,\"feedbackComment\":\"" + "a".repeat(300) + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void cannotRateOtherUsersRecommendation() throws Exception {
        User owner = saveUser("feedback-real-owner@example.com");
        User other = saveUser("feedback-other@example.com");
        Emotion emotion = saveEmotion("Calma", true);
        EmotionalEntry checkin = saveCheckin(owner, emotion);
        MicroActivity activity = saveActivity("Respirar", true);
        ActivityRecommendation recommendation = saveCompletedRecommendation(owner, checkin, activity);

        mockMvc.perform(post("/api/recommendations/{id}/feedback", recommendation.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"helpfulnessRating\":5}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recomendacion no encontrada"));

        assertThat(recommendationRepository.findById(recommendation.getId()).orElseThrow().getHelpfulnessRating())
                .isNull();
    }

    // HU-22 CA02: con varias reglas activas aplicables, se priorizan las categorias preferidas.

    @Test
    void preferredCategoryIsPrioritizedAmongActiveRulesForTheEmotion() throws Exception {
        User user = saveUser("hu22-prefers-breathing@example.com");
        Emotion emotion = saveEmotion("Ansiedad", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity walk = saveActivity("Caminar", true, "MOVIMIENTO");
        MicroActivity breathe = saveActivity("Respiracion 4-7-8", true, "RESPIRACION");
        saveRule(emotion, walk, 1, true);
        saveRule(emotion, breathe, 2, true);
        savePreferences(user, "[\"RESPIRACION\"]");

        recommend(user, checkin)
                .andExpect(jsonPath("$.activity.id").value(breathe.getId()))
                .andExpect(jsonPath("$.fallbackUsed").value(false));
    }

    @Test
    void withoutPreferencesTheHighestPriorityRuleIsKept() throws Exception {
        User user = saveUser("hu22-no-preferences@example.com");
        Emotion emotion = saveEmotion("Ansiedad", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity walk = saveActivity("Caminar", true, "MOVIMIENTO");
        MicroActivity breathe = saveActivity("Respiracion 4-7-8", true, "RESPIRACION");
        saveRule(emotion, walk, 1, true);
        saveRule(emotion, breathe, 2, true);

        recommend(user, checkin)
                .andExpect(jsonPath("$.activity.id").value(walk.getId()))
                .andExpect(jsonPath("$.fallbackUsed").value(false));
    }

    @Test
    void preferenceWithoutMatchingRuleKeepsPriorityOrder() throws Exception {
        User user = saveUser("hu22-no-match@example.com");
        Emotion emotion = saveEmotion("Estres", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity walk = saveActivity("Caminar", true, "MOVIMIENTO");
        MicroActivity breathe = saveActivity("Respiracion 4-7-8", true, "RESPIRACION");
        saveRule(emotion, walk, 1, true);
        saveRule(emotion, breathe, 2, true);
        savePreferences(user, "[\"DESCANSO_OCULAR\"]");

        recommend(user, checkin)
                .andExpect(jsonPath("$.activity.id").value(walk.getId()))
                .andExpect(jsonPath("$.fallbackUsed").value(false));
    }

    @Test
    void inactivePreferredActivityIsNotRecommended() throws Exception {
        User user = saveUser("hu22-inactive-preferred@example.com");
        Emotion emotion = saveEmotion("Cansancio", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        MicroActivity walk = saveActivity("Caminar", true, "MOVIMIENTO");
        MicroActivity eyes = saveActivity("Regla 20-20-20", false, "DESCANSO_OCULAR");
        saveRule(emotion, walk, 2, true);
        saveRule(emotion, eyes, 1, true);
        savePreferences(user, "[\"DESCANSO_OCULAR\"]");

        recommend(user, checkin)
                .andExpect(jsonPath("$.activity.id").value(walk.getId()))
                .andExpect(jsonPath("$.fallbackUsed").value(false));
    }

    @Test
    void preferredCategoryIsPrioritizedInActiveCatalogFallback() throws Exception {
        User user = saveUser("hu22-fallback@example.com");
        Emotion emotion = saveEmotion("Tristeza", true);
        EmotionalEntry checkin = saveCheckin(user, emotion);
        saveActivity("Caminar", true, "MOVIMIENTO");
        MicroActivity writing = saveActivity("Diario de gratitud", true, "ESCRITURA");
        savePreferences(user, "[\"ESCRITURA\"]");

        recommend(user, checkin)
                .andExpect(jsonPath("$.activity.id").value(writing.getId()))
                .andExpect(jsonPath("$.fallbackUsed").value(true));
    }

    private org.springframework.test.web.servlet.ResultActions recommend(User user, EmotionalEntry checkin)
            throws Exception {
        return mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkinId\":" + checkin.getId() + "}"))
                .andExpect(status().isCreated());
    }

    private void savePreferences(User user, String selectedJson) throws Exception {
        mockMvc.perform(put("/api/me/activity-preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selected\":" + selectedJson + "}"))
                .andExpect(status().isOk());
    }

    private MicroActivity saveActivity(String title, boolean active, String activityType) {
        MicroActivity activity = new MicroActivity();
        activity.setTitle(title);
        activity.setNormalizedTitle(microActivityService.normalizeForUniqueness(title));
        activity.setDescription("Descripcion de " + title);
        activity.setDurationMinutes(5);
        activity.setActive(active);
        activity.setActivityType(activityType);
        return microActivityRepository.save(activity);
    }

    private ActivityRecommendation saveCompletedRecommendation(
            User user,
            EmotionalEntry checkin,
            MicroActivity activity
    ) {
        ActivityRecommendation recommendation = saveRecommendation(user, checkin, activity);
        recommendation.complete();
        return recommendationRepository.save(recommendation);
    }

    private User saveUser(String email) {
        User user = new User();
        user.setName("Usuario Recomendacion");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Clave123"));
        user.setBirthDate(LocalDate.of(2000, 5, 20));
        user.setUniversity("UPC");
        user.setCareer("Ingenieria de Software");
        user.setRole(Role.USER);
        user.setActive(true);
        return userRepository.save(user);
    }

    private Emotion saveEmotion(String name, boolean active) {
        Emotion emotion = new Emotion();
        emotion.setName(name);
        emotion.setNormalizedName(emotionService.normalizeForUniqueness(name));
        emotion.setActive(active);
        return emotionRepository.save(emotion);
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

    private EmotionActivityRule saveRule(Emotion emotion, MicroActivity activity, int priority, boolean active) {
        EmotionActivityRule rule = new EmotionActivityRule();
        rule.setEmotion(emotion);
        rule.setActivity(activity);
        rule.setPriority(priority);
        rule.setActive(active);
        return ruleRepository.save(rule);
    }

    private EmotionalEntry saveCheckin(User user, Emotion emotion) {
        EmotionalEntry entry = new EmotionalEntry();
        entry.setUser(user);
        entry.setEmotion(emotion);
        entry.setIntensity(4);
        entry.setContext("Estudios");
        entry.setNote("Nota");
        return emotionalEntryRepository.save(entry);
    }

    private ActivityRecommendation saveRecommendation(
            User user,
            EmotionalEntry checkin,
            MicroActivity activity
    ) {
        ActivityRecommendation recommendation = new ActivityRecommendation();
        recommendation.setUser(user);
        recommendation.setCheckin(checkin);
        recommendation.setActivity(activity);
        recommendation.setReason("Recomendacion de prueba");
        recommendation.setFallbackUsed(false);
        return recommendationRepository.save(recommendation);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
