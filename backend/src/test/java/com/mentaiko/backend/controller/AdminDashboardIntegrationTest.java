package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
class AdminDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmotionalEntryRepository emotionalEntryRepository;

    @Autowired
    private EmotionRepository emotionRepository;

    @Autowired
    private MicroActivityRepository microActivityRepository;

    @Autowired
    private ActivityRecommendationRepository recommendationRepository;

    @Autowired
    private EmotionActivityRuleRepository ruleRepository;

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
    void adminDashboardRequiresAdminRoleAndReturnsReadOnlyData() throws Exception {
        User user = saveUser("student-dashboard@example.com", Role.USER, true);
        User admin = saveUser("admin-dashboard@example.com", Role.ADMIN, true);
        Emotion emotion = saveEmotion("Calma");
        MicroActivity activity = saveActivity("Respirar");
        EmotionalEntry checkin = saveCheckin(user, emotion);
        saveRecommendation(user, checkin, activity, true);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.activeUsers").value(2))
                .andExpect(jsonPath("$.totalAdmins").value(1))
                .andExpect(jsonPath("$.totalCheckins").value(1))
                .andExpect(jsonPath("$.totalEmotions").value(1))
                .andExpect(jsonPath("$.totalMicroActivities").value(1))
                .andExpect(jsonPath("$.totalRecommendations").value(1))
                .andExpect(jsonPath("$.completedRecommendations").value(1));
    }

    @Test
    void adminUsersAndCheckinsNeverExposePasswordsOrPrivateNotes() throws Exception {
        User admin = saveUser("admin-list@example.com", Role.ADMIN, true);
        User user = saveUser("student-list@example.com", Role.USER, true);
        Emotion emotion = saveEmotion("Alegria");
        saveCheckin(user, emotion);

        MvcResult usersResult = mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andReturn();

        MvcResult checkinsResult = mockMvc.perform(get("/api/admin/checkins")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].note").doesNotExist())
                .andExpect(jsonPath("$.content[0].user.email").value("student-list@example.com"))
                .andReturn();

        assertThat(usersResult.getResponse().getContentAsString())
                .doesNotContain("password")
                .doesNotContain("passwordHash")
                .doesNotContain("Clave123");
        assertThat(checkinsResult.getResponse().getContentAsString())
                .doesNotContain("Nota privada")
                .doesNotContain("password")
                .doesNotContain("passwordHash");
    }

    private User saveUser(String email, Role role, boolean active) {
        User user = new User();
        user.setName("Usuario Dashboard");
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

    private EmotionalEntry saveCheckin(User user, Emotion emotion) {
        EmotionalEntry entry = new EmotionalEntry();
        entry.setUser(user);
        entry.setEmotion(emotion);
        entry.setIntensity(4);
        entry.setContext("Estudios");
        entry.setNote("Nota privada");
        entry.setCreatedAt(LocalDateTime.of(2026, 10, 1, 9, 0));
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
