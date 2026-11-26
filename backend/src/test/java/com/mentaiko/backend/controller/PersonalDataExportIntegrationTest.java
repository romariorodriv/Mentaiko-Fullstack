package com.mentaiko.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.mentaiko.backend.config.TestMailConfiguration;
import com.mentaiko.backend.entity.ActivityRecommendation;
import com.mentaiko.backend.entity.Emotion;
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.MicroActivity;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.ActivityRecommendationRepository;
import com.mentaiko.backend.repository.EmotionRepository;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.security.JwtService;

@org.springframework.context.annotation.Import(TestMailConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PersonalDataExportIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired EmotionRepository emotions;
    @Autowired EmotionalEntryRepository checkins;
    @Autowired MicroActivityRepository activities;
    @Autowired ActivityRecommendationRepository recommendations;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService jwt;

    private User createUser() {
        User user = new User();
        user.setName("Export test");
        user.setEmail(UUID.randomUUID() + "@example.invalid");
        user.setPasswordHash(passwords.encode("password-for-tests"));
        user.setBirthDate(LocalDate.of(2001, 1, 1));
        user.setUniversity("UPC");
        user.setCareer("Software");
        user.setRole(Role.USER);
        user.setActive(true);
        return users.saveAndFlush(user);
    }

    @Test
    void exportsOnlyAuthenticatedUsersDataAndRequiresConsentForNotes() throws Exception {
        User owner = createUser();
        User other = createUser();
        Emotion emotion = new Emotion();
        emotion.setName("Export " + UUID.randomUUID());
        emotion.setNormalizedName(emotion.getName().toLowerCase());
        emotion.setActive(true);
        emotion = emotions.saveAndFlush(emotion);

        EmotionalEntry ownEntry = new EmotionalEntry();
        ownEntry.setUser(owner);
        ownEntry.setEmotion(emotion);
        ownEntry.setContext("Estudios");
        ownEntry.setNote("Nota privada");
        ownEntry.setIntensity(4);
        ownEntry = checkins.saveAndFlush(ownEntry);

        EmotionalEntry otherEntry = new EmotionalEntry();
        otherEntry.setUser(other);
        otherEntry.setEmotion(emotion);
        otherEntry.setContext("Otro usuario");
        otherEntry.setIntensity(2);
        checkins.saveAndFlush(otherEntry);

        MicroActivity activity = new MicroActivity();
        activity.setTitle("Export activity " + UUID.randomUUID());
        activity.setNormalizedTitle(activity.getTitle().toLowerCase());
        activity.setDescription("Pausa");
        activity.setDurationMinutes(5);
        activity = activities.saveAndFlush(activity);

        ActivityRecommendation recommendation = new ActivityRecommendation();
        recommendation.setUser(owner);
        recommendation.setCheckin(ownEntry);
        recommendation.setActivity(activity);
        recommendation.setReason("Sugerencia");
        recommendations.saveAndFlush(recommendation);

        String bearer = "Bearer " + jwt.generateToken(owner);
        mvc.perform(get("/api/users/me/export").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkins.length()").value(1))
                .andExpect(jsonPath("$.activities.length()").value(1))
                .andExpect(jsonPath("$.checkins[0].note").doesNotExist())
                .andExpect(jsonPath("$.activities[0].feedbackComment").doesNotExist());

        mvc.perform(get("/api/users/me/export").header(HttpHeaders.AUTHORIZATION, bearer)
                        .param("includeNotes", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkins[0].note").value("Nota privada"));

        mvc.perform(get("/api/users/me/export").header(HttpHeaders.AUTHORIZATION, bearer)
                        .param("from", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkins.length()").value(0))
                .andExpect(jsonPath("$.activities.length()").value(0));

        mvc.perform(get("/api/users/me/export").header(HttpHeaders.AUTHORIZATION, bearer)
                        .param("from", "2026-12-01")
                        .param("to", "2026-11-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnonymousExport() throws Exception {
        mvc.perform(get("/api/users/me/export")).andExpect(status().isUnauthorized());
    }
}
