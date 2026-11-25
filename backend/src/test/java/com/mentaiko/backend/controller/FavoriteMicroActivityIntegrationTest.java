package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
import com.mentaiko.backend.repository.FavoriteMicroActivityRepository;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.security.JwtService;

@Import(TestMailConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FavoriteMicroActivityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired FavoriteMicroActivityRepository favorites;
    @Autowired MicroActivityRepository activities;
    @Autowired UserRepository users;
    @Autowired ActivityRecommendationRepository recommendations;
    @Autowired EmotionActivityRuleRepository rules;
    @Autowired EmotionalEntryRepository checkins;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void clean() {
        favorites.deleteAll();
        recommendations.deleteAll();
        rules.deleteAll();
        checkins.deleteAll();
        activities.deleteAll();
        users.deleteAll();
    }

    @Test
    void favoriteRequiresJwtAndRejectsInactiveActivity() throws Exception {
        MicroActivity inactive = activity("Pausa", false);
        mvc.perform(post("/api/micro-activities/{id}/favorite", inactive.getId()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/micro-activities/{id}/favorite", inactive.getId())
                .header(HttpHeaders.AUTHORIZATION, token(user("favorite1@example.com"))))
                .andExpect(status().isNotFound());
        assertThat(favorites.count()).isZero();
    }

    @Test
    void repeatedFavoriteIsIdempotentAndUnfavoriteReturns204() throws Exception {
        User owner = user("favorite-owner@example.com");
        MicroActivity item = activity("Caminar", true);
        String bearer = token(owner);
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/micro-activities/{id}/favorite", item.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(item.getId()))
                    .andExpect(jsonPath("$.title").value("Caminar"));
        }
        assertThat(favorites.count()).isEqualTo(1);
        mvc.perform(delete("/api/micro-activities/{id}/favorite", item.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
        assertThat(favorites.count()).isZero();
        mvc.perform(delete("/api/micro-activities/{id}/favorite", item.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
    }

    @Test
    void anotherUserCannotRemoveOwnersFavorite() throws Exception {
        User owner = user("original@example.com");
        User other = user("other@example.com");
        MicroActivity item = activity("Respirar", true);
        mvc.perform(post("/api/micro-activities/{id}/favorite", item.getId())
                .header(HttpHeaders.AUTHORIZATION, token(owner))).andExpect(status().isOk());
        mvc.perform(delete("/api/micro-activities/{id}/favorite", item.getId())
                .header(HttpHeaders.AUTHORIZATION, token(other))).andExpect(status().isNoContent());
        assertThat(favorites.existsByUser_IdAndActivity_Id(owner.getId(), item.getId())).isTrue();
    }

    @Test
    void listingOnlyReturnsCurrentUsersFavorites() throws Exception {
        User first = user("first-favorites@example.com");
        User second = user("second-favorites@example.com");
        MicroActivity own = activity("Dormir", true);
        MicroActivity other = activity("Pasear", true);
        mvc.perform(post("/api/micro-activities/{id}/favorite", own.getId())
                .header(HttpHeaders.AUTHORIZATION, token(first))).andExpect(status().isOk());
        mvc.perform(post("/api/micro-activities/{id}/favorite", other.getId())
                .header(HttpHeaders.AUTHORIZATION, token(second))).andExpect(status().isOk());

        mvc.perform(get("/api/micro-activities/favorites"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/micro-activities/favorites")
                .header(HttpHeaders.AUTHORIZATION, token(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(own.getId()))
                .andExpect(jsonPath("$[0].title").value("Dormir"));
        mvc.perform(get("/api/micro-activities/favorites")
                .header(HttpHeaders.AUTHORIZATION, token(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(other.getId()));
    }

    private MicroActivity activity(String title, boolean active) {
        MicroActivity a = new MicroActivity();
        a.setTitle(title);
        a.setNormalizedTitle(title.toLowerCase());
        a.setDescription("Descripcion de actividad");
        a.setDurationMinutes(5);
        a.setActive(active);
        return activities.save(a);
    }

    private User user(String email) {
        User u = new User();
        u.setName("Favorite test");
        u.setEmail(email);
        u.setPasswordHash(encoder.encode("Clave123"));
        u.setBirthDate(LocalDate.of(2000, 5, 20));
        u.setUniversity("UPC");
        u.setCareer("Sistemas");
        u.setRole(Role.USER);
        u.setActive(true);
        return users.save(u);
    }

    private String token(User user) {
        return "Bearer " + jwt.generateToken(user);
    }
}
