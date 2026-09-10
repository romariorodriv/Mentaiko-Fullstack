package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Test
    void authenticatedUserListsOnlyOwnCheckinsOrderedByNewestFirst() throws Exception {
        User owner = saveUser("owner@example.com", Role.USER);
        User other = saveUser("other@example.com", Role.USER);
        Emotion calm = saveEmotion("Calma", true);
        Emotion anxiety = saveEmotion("Ansiedad", true);
        saveEntry(owner, calm, 2, "Estudios", "Primero", LocalDateTime.of(2026, 9, 7, 9, 0));
        saveEntry(other, anxiety, 5, "Trabajo", "Ajeno", LocalDateTime.of(2026, 9, 8, 12, 0));
        saveEntry(owner, anxiety, 4, "Trabajo", "Ultimo", LocalDateTime.of(2026, 9, 8, 18, 30));
        String token = jwtService.generateToken(owner);

        mockMvc.perform(get("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].note", contains("Ultimo", "Primero")))
                .andExpect(jsonPath("$.content[0].emotion.name").value("Ansiedad"))
                .andExpect(jsonPath("$.content[0].user").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void listWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/checkins"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listReturnsEmptyPageWhenUserHasNoCheckins() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void listSupportsRealPagination() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        Emotion emotion = saveEmotion("Calma", true);
        saveEntry(user, emotion, 1, "Estudios", "Uno", LocalDateTime.of(2026, 9, 6, 8, 0));
        saveEntry(user, emotion, 2, "Estudios", "Dos", LocalDateTime.of(2026, 9, 7, 8, 0));
        saveEntry(user, emotion, 3, "Estudios", "Tres", LocalDateTime.of(2026, 9, 8, 8, 0));
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].note").value("Uno"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    void listFiltersByFromDateToDateAndInclusiveRange() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        Emotion emotion = saveEmotion("Calma", true);
        saveEntry(user, emotion, 1, "Estudios", "Agosto", LocalDateTime.of(2026, 8, 31, 23, 59));
        saveEntry(user, emotion, 2, "Estudios", "Inicio", LocalDateTime.of(2026, 9, 1, 0, 0));
        saveEntry(user, emotion, 3, "Estudios", "Final", LocalDateTime.of(2026, 9, 8, 23, 59, 59));
        saveEntry(user, emotion, 4, "Estudios", "Despues", LocalDateTime.of(2026, 9, 9, 0, 0));
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("from", "2026-09-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].note", contains("Despues", "Final", "Inicio")));

        mockMvc.perform(get("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("to", "2026-09-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].note", contains("Final", "Inicio", "Agosto")));

        mockMvc.perform(get("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].note", contains("Final", "Inicio")));
    }

    @Test
    void listFiltersByContextAndCombinesFilters() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        Emotion emotion = saveEmotion("Calma", true);
        saveEntry(user, emotion, 2, "Estudios", "Viejo", LocalDateTime.of(2026, 9, 1, 10, 0));
        saveEntry(user, emotion, 3, "Estudios grupales", "Coincide", LocalDateTime.of(2026, 9, 8, 10, 0));
        saveEntry(user, emotion, 4, "Trabajo", "No coincide", LocalDateTime.of(2026, 9, 8, 11, 0));
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("context", "estudios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].note", contains("Coincide", "Viejo")));

        mockMvc.perform(get("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("from", "2026-09-08")
                        .param("to", "2026-09-08")
                        .param("context", "estudios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].note", contains("Coincide")));
    }

    @Test
    void listRejectsInvalidDateRangeAndPagination() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("from", "2026-09-09")
                        .param("to", "2026-09-08"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha inicial no puede ser posterior a la fecha final"));

        mockMvc.perform(get("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El numero de pagina no puede ser negativo"));

        mockMvc.perform(get("/api/checkins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El tamano de pagina debe estar entre 1 y 50"));
    }

    @Test
    void authenticatedUserUpdatesOwnCheckinAndKeepsOwnerAndCreatedAt() throws Exception {
        User owner = saveUser("owner@example.com", Role.USER);
        Emotion oldEmotion = saveEmotion("Ansiedad", true);
        Emotion newEmotion = saveEmotion("Tranquilidad", true);
        LocalDateTime originalCreatedAt = LocalDateTime.of(2026, 9, 8, 18, 30);
        EmotionalEntry entry = saveEntry(owner, oldEmotion, 2, "Estudios", "Nota inicial", originalCreatedAt);
        String token = jwtService.generateToken(owner);

        mockMvc.perform(put("/api/checkins/{id}", entry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emotionId": %d,
                                  "intensity": 4,
                                  "context": "  Practicas  profesionales  ",
                                  "note": "  Avance mejor de lo esperado  "
                                }
                                """.formatted(newEmotion.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entry.getId()))
                .andExpect(jsonPath("$.emotion.id").value(newEmotion.getId()))
                .andExpect(jsonPath("$.emotion.name").value("Tranquilidad"))
                .andExpect(jsonPath("$.emotion.active").doesNotExist())
                .andExpect(jsonPath("$.intensity").value(4))
                .andExpect(jsonPath("$.context").value("Practicas profesionales"))
                .andExpect(jsonPath("$.note").value("Avance mejor de lo esperado"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-08T18:30:00"))
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        EmotionalEntry updated = emotionalEntryRepository.findById(entry.getId()).orElseThrow();
        assertThat(updated.getUser().getId()).isEqualTo(owner.getId());
        assertThat(updated.getEmotion().getId()).isEqualTo(newEmotion.getId());
        assertThat(updated.getIntensity()).isEqualTo(4);
        assertThat(updated.getContext()).isEqualTo("Practicas profesionales");
        assertThat(updated.getNote()).isEqualTo("Avance mejor de lo esperado");
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    void updateCheckinWithoutTokenReturns401() throws Exception {
        mockMvc.perform(put("/api/checkins/{id}", 1)
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
    void updateReturns404ForMissingId() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        Emotion emotion = saveEmotion("Calma", true);
        String token = jwtService.generateToken(user);

        mockMvc.perform(put("/api/checkins/{id}", 999)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(emotion.getId(), 3, "Estudios", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Check-in no encontrado"));
    }

    @Test
    void updateReturns404ForOtherUserCheckinAndDoesNotModifyIt() throws Exception {
        User owner = saveUser("owner@example.com", Role.USER);
        User other = saveUser("other@example.com", Role.USER);
        Emotion emotion = saveEmotion("Calma", true);
        Emotion requestedEmotion = saveEmotion("Motivacion", true);
        EmotionalEntry otherEntry = saveEntry(other, emotion, 2, "Trabajo", "Ajeno original", LocalDateTime.of(2026, 9, 8, 9, 0));
        String ownerToken = jwtService.generateToken(owner);

        mockMvc.perform(put("/api/checkins/{id}", otherEntry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(requestedEmotion.getId(), 5, "Estudios", "Intento ajeno")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Check-in no encontrado"));

        EmotionalEntry unchanged = emotionalEntryRepository.findById(otherEntry.getId()).orElseThrow();
        assertThat(unchanged.getUser().getId()).isEqualTo(other.getId());
        assertThat(unchanged.getEmotion().getId()).isEqualTo(emotion.getId());
        assertThat(unchanged.getIntensity()).isEqualTo(2);
        assertThat(unchanged.getContext()).isEqualTo("Trabajo");
        assertThat(unchanged.getNote()).isEqualTo("Ajeno original");
    }

    @Test
    void updateRejectsMissingAndInactiveEmotion() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        Emotion active = saveEmotion("Calma", true);
        Emotion inactive = saveEmotion("Tristeza", false);
        EmotionalEntry entry = saveEntry(user, active, 3, "Estudios", "Original", LocalDateTime.of(2026, 9, 8, 9, 0));
        String token = jwtService.generateToken(user);

        mockMvc.perform(put("/api/checkins/{id}", entry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(999L, 3, "Estudios", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Emocion no encontrada"));

        mockMvc.perform(put("/api/checkins/{id}", entry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(inactive.getId(), 3, "Estudios", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La emocion seleccionada no esta activa"));
    }

    @Test
    void updateRejectsIntensityOutsideRangeAndInvalidFields() throws Exception {
        User user = saveUser("usuario@example.com", Role.USER);
        Emotion emotion = saveEmotion("Calma", true);
        EmotionalEntry entry = saveEntry(user, emotion, 3, "Estudios", "Original", LocalDateTime.of(2026, 9, 8, 9, 0));
        String token = jwtService.generateToken(user);
        String longContext = "a".repeat(101);
        String longNote = "b".repeat(501);

        mockMvc.perform(put("/api/checkins/{id}", entry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(emotion.getId(), 0, "Estudios", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.intensity").exists());

        mockMvc.perform(put("/api/checkins/{id}", entry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(emotion.getId(), 6, "Estudios", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.intensity").exists());

        mockMvc.perform(put("/api/checkins/{id}", entry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(emotion.getId(), 3, "", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.context").exists());

        mockMvc.perform(put("/api/checkins/{id}", entry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(emotion.getId(), 3, longContext, longNote)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.context").exists())
                .andExpect(jsonPath("$.errors.note").exists());
    }

    @Test
    void updateIgnoresUserIdFromRequestAndAllowsEmptyNote() throws Exception {
        User owner = saveUser("owner@example.com", Role.USER);
        User other = saveUser("other@example.com", Role.USER);
        Emotion emotion = saveEmotion("Motivacion", true);
        EmotionalEntry entry = saveEntry(owner, emotion, 2, "Trabajo", "Original", LocalDateTime.of(2026, 9, 8, 9, 0));
        String token = jwtService.generateToken(owner);

        mockMvc.perform(put("/api/checkins/{id}", entry.getId())
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.note").doesNotExist());

        EmotionalEntry updated = emotionalEntryRepository.findById(entry.getId()).orElseThrow();
        assertThat(updated.getUser().getId()).isEqualTo(owner.getId());
        assertThat(updated.getUser().getId()).isNotEqualTo(other.getId());
        assertThat(updated.getNote()).isNull();
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

    private EmotionalEntry saveEntry(
            User user,
            Emotion emotion,
            int intensity,
            String context,
            String note,
            LocalDateTime createdAt
    ) {
        EmotionalEntry entry = new EmotionalEntry();
        entry.setUser(user);
        entry.setEmotion(emotion);
        entry.setIntensity(intensity);
        entry.setContext(context);
        entry.setNote(note);
        entry.setCreatedAt(createdAt);
        return emotionalEntryRepository.save(entry);
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
