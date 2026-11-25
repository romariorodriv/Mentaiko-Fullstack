package com.mentaiko.backend.controller;

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

import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionalEntry;
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

@org.springframework.context.annotation.Import(com.mentaiko.backend.config.TestMailConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ReportIntegrationTest {

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
    void reportsRequireToken() throws Exception {
        mockMvc.perform(get("/api/reports/weekly").param("week", "2026-W40"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/reports/distribution"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void weeklyReturnsSevenEmptyDaysWhenUserHasNoCheckins() throws Exception {
        User user = saveUser("weekly-empty@example.com");

        mockMvc.perform(get("/api/reports/weekly")
                        .param("week", "2026-W40")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].count").value(0))
                .andExpect(jsonPath("$[0].averageIntensity").value(0));
    }

    @Test
    void weeklyAggregatesOnlyAuthenticatedUserAndRequestedWeek() throws Exception {
        User owner = saveUser("weekly-owner@example.com");
        User other = saveUser("weekly-other@example.com");
        Emotion anxiety = saveEmotion("Ansiedad");
        Emotion calm = saveEmotion("Calma");

        saveCheckin(owner, anxiety, 4, "Estudios", LocalDateTime.of(2026, 9, 28, 9, 0));
        saveCheckin(owner, calm, 2, "Trabajo", LocalDateTime.of(2026, 9, 28, 18, 0));
        saveCheckin(owner, anxiety, 5, "Estudios", LocalDateTime.of(2026, 9, 30, 10, 0));
        saveCheckin(owner, anxiety, 1, "Fuera", LocalDateTime.of(2026, 10, 5, 10, 0));
        saveCheckin(other, anxiety, 5, "Otro", LocalDateTime.of(2026, 9, 28, 11, 0));

        mockMvc.perform(get("/api/reports/weekly")
                        .param("week", "2026-W40")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].count").value(2))
                .andExpect(jsonPath("$[0].averageIntensity").value(3.0))
                .andExpect(jsonPath("$[2].count").value(1))
                .andExpect(jsonPath("$[2].averageIntensity").value(5.0));
    }

    @Test
    void distributionCalculatesCountsPercentagesAndDateFilters() throws Exception {
        User owner = saveUser("distribution-owner@example.com");
        User other = saveUser("distribution-other@example.com");
        Emotion anxiety = saveEmotion("Ansiedad");
        Emotion calm = saveEmotion("Calma");

        saveCheckin(owner, anxiety, 4, "Estudios", LocalDateTime.of(2026, 9, 28, 9, 0));
        saveCheckin(owner, anxiety, 3, "Estudios", LocalDateTime.of(2026, 9, 29, 9, 0));
        saveCheckin(owner, calm, 2, "Trabajo", LocalDateTime.of(2026, 9, 30, 9, 0));
        saveCheckin(owner, calm, 5, "Familia", LocalDateTime.of(2026, 10, 10, 9, 0));
        saveCheckin(other, calm, 5, "Otro", LocalDateTime.of(2026, 9, 29, 9, 0));

        mockMvc.perform(get("/api/reports/distribution")
                        .param("from", "2026-09-28")
                        .param("to", "2026-09-30")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotions", hasSize(2)))
                .andExpect(jsonPath("$.emotions[0].label").value("Ansiedad"))
                .andExpect(jsonPath("$.emotions[0].count").value(2))
                .andExpect(jsonPath("$.emotions[0].percentage").value(66.7))
                .andExpect(jsonPath("$.emotions[1].label").value("Calma"))
                .andExpect(jsonPath("$.emotions[1].count").value(1))
                .andExpect(jsonPath("$.emotions[1].percentage").value(33.3))
                .andExpect(jsonPath("$.contexts[0].label").value("Estudios"))
                .andExpect(jsonPath("$.contexts[0].count").value(2));
    }

    @Test
    void distributionRejectsInvalidDateRange() throws Exception {
        User user = saveUser("distribution-range@example.com");

        mockMvc.perform(get("/api/reports/distribution")
                        .param("from", "2026-10-01")
                        .param("to", "2026-09-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha inicial no puede ser posterior a la fecha final"));
    }

    @Test
    void compareRequiresToken() throws Exception {
        mockMvc.perform(get("/api/reports/compare")
                        .param("weekA", "2026-W39")
                        .param("weekB", "2026-W40"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void compareReturnsCountAverageAndPredominantEmotionForBothWeeksOnlyForOwner() throws Exception {
        User owner = saveUser("compare-owner@example.com");
        User other = saveUser("compare-other@example.com");
        Emotion anxiety = saveEmotion("Ansiedad");
        Emotion calm = saveEmotion("Calma");

        // 2026-W39: 21/09 al 27/09 -> 3 registros (Calma x2, Ansiedad x1)
        saveCheckin(owner, calm, 2, "Estudios", LocalDateTime.of(2026, 9, 21, 9, 0));
        saveCheckin(owner, calm, 4, "Estudios", LocalDateTime.of(2026, 9, 23, 9, 0));
        saveCheckin(owner, anxiety, 3, "Trabajo", LocalDateTime.of(2026, 9, 27, 20, 0));
        // 2026-W40: 28/09 al 04/10 -> 2 registros (Ansiedad x2)
        saveCheckin(owner, anxiety, 4, "Estudios", LocalDateTime.of(2026, 9, 28, 9, 0));
        saveCheckin(owner, anxiety, 5, "Estudios", LocalDateTime.of(2026, 10, 4, 22, 0));
        // Registros de otro usuario: no deben contarse
        saveCheckin(other, calm, 5, "Otro", LocalDateTime.of(2026, 9, 22, 9, 0));
        saveCheckin(other, calm, 5, "Otro", LocalDateTime.of(2026, 9, 29, 9, 0));

        mockMvc.perform(get("/api/reports/compare")
                        .param("weekA", "2026-W39")
                        .param("weekB", "2026-W40")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekA.week").value("2026-W39"))
                .andExpect(jsonPath("$.weekA.count").value(3))
                .andExpect(jsonPath("$.weekA.averageIntensity").value(3.0))
                .andExpect(jsonPath("$.weekA.predominantEmotion").value("Calma"))
                .andExpect(jsonPath("$.weekB.week").value("2026-W40"))
                .andExpect(jsonPath("$.weekB.count").value(2))
                .andExpect(jsonPath("$.weekB.averageIntensity").value(4.5))
                .andExpect(jsonPath("$.weekB.predominantEmotion").value("Ansiedad"));
    }

    @Test
    void compareReturnsZeroCountAndNullEmotionForEmptyWeek() throws Exception {
        User user = saveUser("compare-empty@example.com");
        Emotion calm = saveEmotion("Calma");
        saveCheckin(user, calm, 3, "Estudios", LocalDateTime.of(2026, 9, 28, 9, 0));

        mockMvc.perform(get("/api/reports/compare")
                        .param("weekA", "2026-W39")
                        .param("weekB", "2026-W40")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekA.count").value(0))
                .andExpect(jsonPath("$.weekA.averageIntensity").value(0))
                .andExpect(jsonPath("$.weekA.predominantEmotion").doesNotExist())
                .andExpect(jsonPath("$.weekB.count").value(1));
    }

    @Test
    void compareRejectsInvalidWeekFormat() throws Exception {
        User user = saveUser("compare-invalid@example.com");

        mockMvc.perform(get("/api/reports/compare")
                        .param("weekA", "2026-40")
                        .param("weekB", "2026-W40")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La semana debe tener formato YYYY-Www"));
    }

    private User saveUser(String email) {
        User user = new User();
        user.setName("Usuario Reporte");
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
        Emotion emotion = new Emotion();
        emotion.setName(name);
        emotion.setNormalizedName(emotionService.normalizeForUniqueness(name));
        emotion.setActive(true);
        return emotionRepository.save(emotion);
    }

    private EmotionalEntry saveCheckin(
            User user,
            Emotion emotion,
            int intensity,
            String context,
            LocalDateTime createdAt
    ) {
        EmotionalEntry entry = new EmotionalEntry();
        entry.setUser(user);
        entry.setEmotion(emotion);
        entry.setIntensity(intensity);
        entry.setContext(context);
        entry.setNote("Nota");
        entry.setCreatedAt(createdAt);
        return emotionalEntryRepository.save(entry);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
