package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.mentaiko.backend.entity.ActivityRecommendation;
import com.mentaiko.backend.entity.Emotion;
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
class AdminIndicatorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActivityRecommendationRepository recommendationRepository;

    @Autowired
    private EmotionActivityRuleRepository ruleRepository;

    @Autowired
    private EmotionalEntryRepository emotionalEntryRepository;

    @Autowired
    private MicroActivityRepository microActivityRepository;

    @Autowired
    private EmotionRepository emotionRepository;

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
    void indicatorsRequireAdminRole() throws Exception {
        User user = saveUser("indicators-user@example.com", Role.USER, true);

        mockMvc.perform(get("/api/admin/indicators"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/indicators")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void emptyDatabaseReturnsZerosWithoutDivisionErrors() throws Exception {
        User admin = saveUser("empty-admin@example.com", Role.ADMIN, true);

        mockMvc.perform(get("/api/admin/indicators")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(1))
                .andExpect(jsonPath("$.activeUsers").value(1))
                .andExpect(jsonPath("$.totalCheckins").value(0))
                .andExpect(jsonPath("$.emotionDistribution", hasSize(0)))
                .andExpect(jsonPath("$.recommendationsGenerated").value(0))
                .andExpect(jsonPath("$.recommendationsCompleted").value(0))
                .andExpect(jsonPath("$.completionRate").value(0))
                .andExpect(jsonPath("$.mostFrequentEmotion").value("Sin datos"));
    }

    @Test
    void indicatorsReturnAggregatedAnonymousValues() throws Exception {
        User admin = saveUser("admin-private@example.com", Role.ADMIN, true);
        User activeUser = saveUser("student-private@example.com", Role.USER, true);
        User inactiveUser = saveUser("inactive-private@example.com", Role.USER, false);
        Emotion anxiety = saveEmotion("Ansiedad");
        Emotion calm = saveEmotion("Calma");
        MicroActivity activity = saveActivity("Respirar");

        EmotionalEntry first = saveCheckin(activeUser, anxiety, "Nota secreta uno");
        EmotionalEntry second = saveCheckin(activeUser, anxiety, "Nota secreta dos");
        EmotionalEntry third = saveCheckin(inactiveUser, calm, "Nota secreta tres");
        saveRecommendation(activeUser, first, activity, true);
        saveRecommendation(activeUser, second, activity, false);
        saveRecommendation(inactiveUser, third, activity, false);

        MvcResult result = mockMvc.perform(get("/api/admin/indicators")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(3))
                .andExpect(jsonPath("$.activeUsers").value(2))
                .andExpect(jsonPath("$.totalCheckins").value(3))
                .andExpect(jsonPath("$.emotionDistribution", hasSize(2)))
                .andExpect(jsonPath("$.emotionDistribution[0].label").value("Ansiedad"))
                .andExpect(jsonPath("$.emotionDistribution[0].count").value(2))
                .andExpect(jsonPath("$.emotionDistribution[0].percentage").value(66.7))
                .andExpect(jsonPath("$.recommendationsGenerated").value(3))
                .andExpect(jsonPath("$.recommendationsCompleted").value(1))
                .andExpect(jsonPath("$.completionRate").value(33.3))
                .andExpect(jsonPath("$.mostFrequentEmotion").value("Ansiedad"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .doesNotContain("student-private@example.com")
                .doesNotContain("inactive-private@example.com")
                .doesNotContain("Usuario Indicador")
                .doesNotContain("Nota secreta");
    }

    private User saveUser(String email, Role role, boolean active) {
        User user = new User();
        user.setName("Usuario Indicador");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Clave123"));
        user.setBirthDate(LocalDate.of(2000, 5, 20));
        user.setUniversity("UPC");
        user.setCareer("Ingenieria de Software");
        user.setRole(role);
        user.setActive(active);
        return userRepository.save(user);
    }

    private Emotion saveEmotion(String name) {
        Emotion emotion = new Emotion();
        emotion.setName(name);
        emotion.setNormalizedName(emotionService.normalizeForUniqueness(name));
        emotion.setActive(true);
        return emotionRepository.save(emotion);
    }

    private MicroActivity saveActivity(String title) {
        MicroActivity activity = new MicroActivity();
        activity.setTitle(title);
        activity.setNormalizedTitle(microActivityService.normalizeForUniqueness(title));
        activity.setDescription("Descripcion");
        activity.setDurationMinutes(5);
        activity.setActive(true);
        return microActivityRepository.save(activity);
    }

    private EmotionalEntry saveCheckin(User user, Emotion emotion, String note) {
        EmotionalEntry entry = new EmotionalEntry();
        entry.setUser(user);
        entry.setEmotion(emotion);
        entry.setIntensity(4);
        entry.setContext("Estudios");
        entry.setNote(note);
        entry.setCreatedAt(LocalDateTime.of(2026, 9, 30, 9, 0));
        return emotionalEntryRepository.save(entry);
    }

    private ActivityRecommendation saveRecommendation(
            User user,
            EmotionalEntry checkin,
            MicroActivity activity,
            boolean completed
    ) {
        ActivityRecommendation recommendation = new ActivityRecommendation();
        recommendation.setUser(user);
        recommendation.setCheckin(checkin);
        recommendation.setActivity(activity);
        recommendation.setReason("Razon");
        recommendation.setFallbackUsed(false);
        if (completed) {
            recommendation.complete();
        }
        return recommendationRepository.save(recommendation);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
