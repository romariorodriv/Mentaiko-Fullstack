package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.ActivityRecommendationRepository;
import com.mentaiko.backend.repository.EmotionActivityRuleRepository;
import com.mentaiko.backend.repository.EmotionRepository;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.FavoriteMicroActivityRepository;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.repository.PasswordResetTokenRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.repository.WeeklyGoalRepository;
import com.mentaiko.backend.security.JwtService;
import com.mentaiko.backend.service.ActivityPreferenceService;

/** HU-22: CA01 (guardar preferencias) y CA03 (solo el propietario consulta o edita). */
@org.springframework.context.annotation.Import(com.mentaiko.backend.config.TestMailConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ActivityPreferenceIntegrationTest {

    private static final String URL = "/api/me/activity-preferences";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private FavoriteMicroActivityRepository favoriteRepository;

    @Autowired
    private ActivityRecommendationRepository recommendationRepository;

    @Autowired
    private EmotionActivityRuleRepository ruleRepository;

    @Autowired
    private EmotionalEntryRepository emotionalEntryRepository;

    @Autowired
    private WeeklyGoalRepository weeklyGoalRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private MicroActivityRepository microActivityRepository;

    @Autowired
    private EmotionRepository emotionRepository;

    @BeforeEach
    void setUp() {
        // Otras clases de prueba comparten la BD H2: limpiar primero todo lo que referencia a users.
        jdbc.update("DELETE FROM activity_preferences");
        favoriteRepository.deleteAll();
        recommendationRepository.deleteAll();
        ruleRepository.deleteAll();
        emotionalEntryRepository.deleteAll();
        weeklyGoalRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        microActivityRepository.deleteAll();
        emotionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void preferencesRequireToken() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selected\":[\"RESPIRACION\"]}"))
                .andExpect(status().isUnauthorized());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity_preferences", Integer.class)).isZero();
    }

    @Test
    void authenticatedStudentSavesPreferredTypesAndGets200() throws Exception {
        User student = saveUser("hu22-save@example.com");

        mockMvc.perform(put(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selected\":[\"RESPIRACION\",\"DESCANSO_OCULAR\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected", contains("DESCANSO_OCULAR", "RESPIRACION")))
                .andExpect(jsonPath("$.available", hasSize(ActivityPreferenceService.TYPES.size())))
                .andExpect(jsonPath("$.available", contains(ActivityPreferenceService.TYPES.toArray())));

        mockMvc.perform(get(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected", contains("DESCANSO_OCULAR", "RESPIRACION")));

        assertThat(typesOf(student)).containsExactly("DESCANSO_OCULAR", "RESPIRACION");
    }

    @Test
    void savingReplacesPreviousSelectionAndEmptyListClearsIt() throws Exception {
        User student = saveUser("hu22-replace@example.com");
        savePreferences(student, "[\"RESPIRACION\",\"ESCRITURA\"]");

        savePreferences(student, "[\"MOVIMIENTO\"]");
        assertThat(typesOf(student)).containsExactly("MOVIMIENTO");

        mockMvc.perform(put(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selected\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected", empty()));
        assertThat(typesOf(student)).isEmpty();
    }

    @Test
    void invalidTypesAreRejectedAndPreviousPreferencesStayIntact() throws Exception {
        User student = saveUser("hu22-invalid@example.com");
        savePreferences(student, "[\"RELAJACION\"]");

        for (String body : new String[] {
                "{\"selected\":[\"NO_EXISTE\"]}",
                "{\"selected\":[\"RESPIRACION\",\"RESPIRACION\"]}",
                "{}"
        }) {
            mockMvc.perform(put(URL)
                            .header(HttpHeaders.AUTHORIZATION, bearer(student))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        assertThat(typesOf(student)).containsExactly("RELAJACION");
    }

    @Test
    void userCannotSeeOrModifyAnotherUsersPreferences() throws Exception {
        User userA = saveUser("hu22-user-a@example.com");
        User userB = saveUser("hu22-user-b@example.com");
        savePreferences(userA, "[\"RESPIRACION\"]");

        // B consulta: solo ve sus propias preferencias (ninguna), no las de A.
        mockMvc.perform(get(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected", empty()));

        // B edita: el cambio solo afecta a B.
        savePreferences(userB, "[\"MOVIMIENTO\"]");

        mockMvc.perform(get(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected", contains("RESPIRACION")));

        mockMvc.perform(get(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected", contains("MOVIMIENTO")));

        // B vacía sus preferencias: las de A siguen intactas.
        savePreferences(userB, "[]");

        assertThat(typesOf(userA)).containsExactly("RESPIRACION");
        assertThat(typesOf(userB)).isEmpty();
    }

    private void savePreferences(User user, String selectedJson) throws Exception {
        mockMvc.perform(put(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selected\":" + selectedJson + "}"))
                .andExpect(status().isOk());
    }

    private List<String> typesOf(User user) {
        return jdbc.queryForList(
                "SELECT activity_type FROM activity_preferences WHERE user_id=? ORDER BY activity_type",
                String.class, user.getId());
    }

    private User saveUser(String email) {
        User user = new User();
        user.setName("Usuario Preferencias");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Clave123"));
        user.setBirthDate(LocalDate.of(2000, 5, 20));
        user.setUniversity("UPC");
        user.setCareer("Ingenieria de Software");
        user.setRole(Role.USER);
        user.setActive(true);
        return userRepository.save(user);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
