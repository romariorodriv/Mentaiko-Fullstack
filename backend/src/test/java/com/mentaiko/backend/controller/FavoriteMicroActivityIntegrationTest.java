package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.mentaiko.backend.config.TestMailConfiguration;
import com.mentaiko.backend.entity.MicroActivity;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.ActivityRecommendationRepository;
import com.mentaiko.backend.repository.EmotionActivityRuleRepository;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.EmotionRepository;
import com.mentaiko.backend.repository.FavoriteMicroActivityRepository;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.security.JwtService;

@org.springframework.context.annotation.Import(TestMailConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FavoriteMicroActivityIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired FavoriteMicroActivityRepository favorites;
    @Autowired MicroActivityRepository activities;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired ActivityRecommendationRepository recommendations;
    @Autowired EmotionActivityRuleRepository rules;
    @Autowired EmotionalEntryRepository entries;
    @Autowired EmotionRepository emotions;

    @BeforeEach
    @AfterEach
    void clean() {
        favorites.deleteAll();
        recommendations.deleteAll();
        rules.deleteAll();
        entries.deleteAll();
        activities.deleteAll();
        emotions.deleteAll();
        users.deleteAll();
    }

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/me/favorite-micro-activities"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/me/favorite-micro-activities/1"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/me/favorite-micro-activities/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanSaveListAndRemoveFavoriteWithoutDuplicates() throws Exception {
        User owner = user("favorite-owner@example.com");
        MicroActivity activity = activity("Respiracion", true);
        String token = bearer(owner);
        String path = "/api/me/favorite-micro-activities/" + activity.getId();

        mvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activity.getId()))
                .andExpect(jsonPath("$.title").value("Respiracion"))
                .andExpect(jsonPath("$.savedAt").exists());

        mvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        assertThat(favorites.findAll()).hasSize(1);

        mvc.perform(get("/api/me/favorite-micro-activities")
                .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(activity.getId()));

        mvc.perform(delete(path).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mvc.perform(delete(path).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        assertThat(favorites.findAll()).isEmpty();
    }

    @Test
    void favoritesBelongOnlyToAuthenticatedUser() throws Exception {
        User owner = user("owner-favorites@example.com");
        User other = user("other-favorites@example.com");
        MicroActivity activity = activity("Caminar", true);
        String path = "/api/me/favorite-micro-activities/" + activity.getId();

        mvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/me/favorite-micro-activities")
                .header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(delete(path).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNoContent());
        assertThat(favorites.findAll()).hasSize(1);
    }

    @Test
    void rejectsUnknownAndInactiveActivities() throws Exception {
        User owner = user("invalid-favorite@example.com");
        MicroActivity inactive = activity("Pausa", false);
        String token = bearer(owner);

        mvc.perform(put("/api/me/favorite-micro-activities/" + inactive.getId())
                .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/me/favorite-micro-activities/999999")
                .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    private User user(String email) {
        User user = new User();
        user.setName("Estudiante");
        user.setEmail(email);
        user.setPasswordHash(encoder.encode("Clave123"));
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setUniversity("UPC");
        user.setCareer("Ingenieria de Sistemas");
        user.setRole(Role.USER);
        user.setActive(true);
        return users.save(user);
    }

    private MicroActivity activity(String title, boolean active) {
        MicroActivity activity = new MicroActivity();
        activity.setTitle(title);
        activity.setNormalizedTitle(title.toLowerCase());
        activity.setDescription("Actividad de bienestar");
        activity.setDurationMinutes(5);
        activity.setActive(active);
        return activities.save(activity);
    }

    private String bearer(User user) {
        return "Bearer " + jwt.generateToken(user);
    }
}
