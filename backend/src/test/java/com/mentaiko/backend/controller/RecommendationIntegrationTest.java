package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionActivityRule;
import com.mentaiko.backend.entity.EmotionalEntry;
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

    @BeforeEach
    void setUp() {
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

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
