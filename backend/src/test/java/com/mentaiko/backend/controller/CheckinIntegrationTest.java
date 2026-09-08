package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

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
import com.mentaiko.backend.entity.EmotionalEntry;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.EmotionRepository;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.security.JwtService;
import com.mentaiko.backend.service.EmotionService;

@SpringBootTest
@AutoConfigureMockMvc
class CheckinIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmotionalEntryRepository emotionalEntryRepository;

    @Autowired
    private EmotionRepository emotionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmotionService emotionService;

    @BeforeEach
    void setUp() {
        emotionalEntryRepository.deleteAll();
        emotionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createsCheckinForAuthenticatedUserAndReturns201() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        Emotion emotion = saveEmotion("Ansiedad", true);
        String token = jwtService.generateToken(user);

        mockMvc.perform(post("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emotionId": %d,
                                  "intensity": 4,
                                  "context": "  Estudios  ",
                                  "note": "  Tuve una presentacion  "
                                }
                                """.formatted(emotion.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.emotion.id").value(emotion.getId()))
                .andExpect(jsonPath("$.emotion.name").value("Ansiedad"))
                .andExpect(jsonPath("$.emotion.active").doesNotExist())
                .andExpect(jsonPath("$.intensity").value(4))
                .andExpect(jsonPath("$.context").value("Estudios"))
                .andExpect(jsonPath("$.note").value("Tuve una presentacion"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        List<EmotionalEntry> entries = emotionalEntryRepository.findAll();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getUser().getId()).isEqualTo(user.getId());
        assertThat(entries.get(0).getEmotion().getId()).isEqualTo(emotion.getId());
    }

    @Test
    void createCheckinWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/checkins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emotionId": 1,
                                  "intensity": 3,
                                  "context": "Estudios"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMissingEmotion() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        String token = jwtService.generateToken(user);

        mockMvc.perform(post("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emotionId": 999,
                                  "intensity": 3,
                                  "context": "Estudios"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Emocion no encontrada"));
    }

    @Test
    void rejectsInactiveEmotion() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        Emotion emotion = saveEmotion("Tristeza", false);
        String token = jwtService.generateToken(user);

        mockMvc.perform(post("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(emotion.getId(), 3, "Estudios", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La emocion seleccionada no esta activa"));
    }

    @Test
    void rejectsIntensityOutsideRange() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        Emotion emotion = saveEmotion("Calma", true);
        String token = jwtService.generateToken(user);

        mockMvc.perform(post("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(emotion.getId(), 0, "Estudios", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.intensity").exists());

        mockMvc.perform(post("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(emotion.getId(), 6, "Estudios", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.intensity").exists());
    }

    @Test
    void rejectsInvalidOrTooLongFields() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        Emotion emotion = saveEmotion("Calma", true);
        String token = jwtService.generateToken(user);
        String longContext = "a".repeat(101);
        String longNote = "b".repeat(501);

        mockMvc.perform(post("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(emotion.getId(), 3, "", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.context").exists());

        mockMvc.perform(post("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(emotion.getId(), 3, longContext, longNote)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.context").exists())
                .andExpect(jsonPath("$.errors.note").exists());
    }

    @Test
    void ignoresUserIdFromRequestAndUsesAuthenticatedUser() throws Exception {
        User owner = saveUser("owner@example.com", Role.USER);
        User other = saveUser("other@example.com", Role.USER);
        Emotion emotion = saveEmotion("Motivacion", true);
        String token = jwtService.generateToken(owner);

        mockMvc.perform(post("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "emotionId": %d,
                                  "intensity": 5,
                                  "context": "Trabajo",
                                  "note": ""
                                }
                                """.formatted(other.getId(), emotion.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user").doesNotExist());

        EmotionalEntry saved = emotionalEntryRepository.findAll().get(0);
        assertThat(saved.getUser().getId()).isEqualTo(owner.getId());
        assertThat(saved.getUser().getId()).isNotEqualTo(other.getId());
        assertThat(saved.getNote()).isNull();
    }

    @Test
    void previousAuthAndEmotionEndpointsStillWork() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        Emotion emotion = saveEmotion("Calma", true);
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("usuario@example.com"));

        mockMvc.perform(get("/api/emotions")
                        .param("activeOnly", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(emotion.getId()))
                .andExpect(jsonPath("$[0].name").value("Calma"));
    }

    private Emotion saveEmotion(String name, boolean active) {
        Emotion emotion = new Emotion();
        emotion.setName(name);
        emotion.setNormalizedName(emotionService.normalizeForUniqueness(name));
        emotion.setActive(active);
        return emotionRepository.save(emotion);
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setName("Usuario Test");
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(passwordEncoder.encode("Clave123"));
        user.setBirthDate(LocalDate.of(2000, 5, 20));
        user.setUniversity("UPC");
        user.setCareer("Ingenieria de Software");
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }

    private String validJson(Long emotionId, int intensity, String context, String note) {
        String noteJson = note == null ? "null" : "\"%s\"".formatted(note);
        return """
                {
                  "emotionId": %d,
                  "intensity": %d,
                  "context": "%s",
                  "note": %s
                }
                """.formatted(emotionId, intensity, context, noteJson);
    }
}
