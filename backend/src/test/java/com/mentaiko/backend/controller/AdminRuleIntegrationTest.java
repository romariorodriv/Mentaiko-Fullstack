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
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.mentaiko.backend.entity.ActivityRecommendation;
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
import com.mentaiko.backend.repository.FavoriteMicroActivityRepository;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.repository.PasswordResetTokenRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.repository.WeeklyGoalRepository;
import com.mentaiko.backend.security.JwtService;
import com.mentaiko.backend.service.EmotionService;
import com.mentaiko.backend.service.MicroActivityService;

/** HU-33: CA01 (crear/editar con validaciones), CA02 (activar/desactivar sin afectar historial) y CA03 (403 a USER). */
@org.springframework.context.annotation.Import(com.mentaiko.backend.config.TestMailConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdminRuleIntegrationTest {

    private static final String URL = "/api/admin/rules";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FavoriteMicroActivityRepository favoriteRepository;
    @Autowired private ActivityRecommendationRepository recommendationRepository;
    @Autowired private EmotionActivityRuleRepository ruleRepository;
    @Autowired private EmotionalEntryRepository emotionalEntryRepository;
    @Autowired private WeeklyGoalRepository weeklyGoalRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private MicroActivityRepository microActivityRepository;
    @Autowired private EmotionRepository emotionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EmotionService emotionService;
    @Autowired private MicroActivityService microActivityService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Otras clases de prueba comparten la BD H2: limpiar en orden de dependencias antes de borrar usuarios.
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

    // ---------- CA03: solo ADMIN ----------

    @Test
    void rulesRequireToken() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void userRoleGets403OnEveryRuleEndpointAndNothingChanges() throws Exception {
        String userToken = bearer(saveUser("hu33-student@example.com", Role.USER));
        Emotion emotion = saveEmotion("Ansiedad", true);
        MicroActivity activity = saveActivity("Respirar", true);
        EmotionActivityRule rule = saveRule(emotion, activity, 1, true);

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(emotion.getId(), saveActivity("Caminar", true).getId(), 2)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(URL + "/{id}", rule.getId())
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());

        assertThat(ruleRepository.findAll()).hasSize(1);
        assertThat(ruleRepository.findById(rule.getId()).orElseThrow().isActive()).isTrue();
    }

    // ---------- CA01: crear (201) y editar (200) con validaciones ----------

    @Test
    void adminCreatesRuleForActiveEmotionAndActivityWith201() throws Exception {
        String admin = bearer(saveUser("hu33-admin-create@example.com", Role.ADMIN));
        Emotion emotion = saveEmotion("Ansiedad", true);
        MicroActivity activity = saveActivity("Respiracion guiada", true);

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(emotion.getId(), activity.getId(), 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.emotionId").value(emotion.getId()))
                .andExpect(jsonPath("$.emotionName").value("Ansiedad"))
                .andExpect(jsonPath("$.activityId").value(activity.getId()))
                .andExpect(jsonPath("$.activityTitle").value("Respiracion guiada"))
                .andExpect(jsonPath("$.priority").value(3))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").exists());

        assertThat(ruleRepository.findAll()).singleElement().satisfies(rule -> {
            assertThat(rule.getPriority()).isEqualTo(3);
            assertThat(rule.isActive()).isTrue();
        });
    }

    @Test
    void adminEditsPriorityAndStateWith200() throws Exception {
        String admin = bearer(saveUser("hu33-admin-edit@example.com", Role.ADMIN));
        EmotionActivityRule rule = saveRule(saveEmotion("Estres", true), saveActivity("Caminar", true), 5, true);

        mockMvc.perform(put(URL + "/{id}", rule.getId())
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value(1))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(put(URL + "/{id}", rule.getId())
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":2,\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value(2))
                .andExpect(jsonPath("$.active").value(false));

        EmotionActivityRule saved = ruleRepository.findById(rule.getId()).orElseThrow();
        assertThat(saved.getPriority()).isEqualTo(2);
        assertThat(saved.isActive()).isFalse();
    }

    @Test
    void adminListsRulesOrderedByEmotionAndPriority() throws Exception {
        String admin = bearer(saveUser("hu33-admin-list@example.com", Role.ADMIN));
        Emotion stress = saveEmotion("Estres", true);
        Emotion anxiety = saveEmotion("Ansiedad", true);
        MicroActivity walk = saveActivity("Caminar", true);
        MicroActivity breathe = saveActivity("Respirar", true);
        saveRule(stress, walk, 1, true);
        saveRule(anxiety, walk, 2, false);
        saveRule(anxiety, breathe, 1, true);

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].emotionName", contains("Ansiedad", "Ansiedad", "Estres")))
                .andExpect(jsonPath("$[*].activityTitle", contains("Respirar", "Caminar", "Caminar")))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    void priorityMustBeAtLeastOne() throws Exception {
        String admin = bearer(saveUser("hu33-admin-priority@example.com", Role.ADMIN));
        Emotion emotion = saveEmotion("Ansiedad", true);
        MicroActivity activity = saveActivity("Respirar", true);

        for (String priority : new String[] {"0", "-3", "null"}) {
            mockMvc.perform(post(URL)
                            .header(HttpHeaders.AUTHORIZATION, admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"emotionId\":" + emotion.getId() + ",\"activityId\":" + activity.getId()
                                    + ",\"priority\":" + priority + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.priority").exists());
        }

        EmotionActivityRule rule = saveRule(emotion, activity, 4, true);
        mockMvc.perform(put(URL + "/{id}", rule.getId())
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.priority")
                        .value("La prioridad debe ser un numero mayor o igual a 1"));

        assertThat(ruleRepository.findById(rule.getId()).orElseThrow().getPriority()).isEqualTo(4);
    }

    @Test
    void emotionAndActivityMustExistAndBeActive() throws Exception {
        String admin = bearer(saveUser("hu33-admin-active@example.com", Role.ADMIN));
        Emotion activeEmotion = saveEmotion("Ansiedad", true);
        Emotion inactiveEmotion = saveEmotion("Aburrimiento", false);
        MicroActivity activeActivity = saveActivity("Respirar", true);
        MicroActivity inactiveActivity = saveActivity("Inactiva", false);

        expectCreateRejected(admin, createBody(999_999L, activeActivity.getId(), 1), "La emocion seleccionada no existe");
        expectCreateRejected(admin, createBody(inactiveEmotion.getId(), activeActivity.getId(), 1),
                "La emocion seleccionada esta inactiva");
        expectCreateRejected(admin, createBody(activeEmotion.getId(), 999_999L, 1),
                "La microactividad seleccionada no existe");
        expectCreateRejected(admin, createBody(activeEmotion.getId(), inactiveActivity.getId(), 1),
                "La microactividad seleccionada esta inactiva");

        assertThat(ruleRepository.findAll()).isEmpty();
    }

    @Test
    void duplicateEmotionActivityPairIsRejectedWith409() throws Exception {
        String admin = bearer(saveUser("hu33-admin-duplicate@example.com", Role.ADMIN));
        Emotion emotion = saveEmotion("Ansiedad", true);
        MicroActivity activity = saveActivity("Respirar", true);
        saveRule(emotion, activity, 1, false);

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(emotion.getId(), activity.getId(), 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una regla para esa emocion y microactividad"));

        assertThat(ruleRepository.findAll()).hasSize(1);
    }

    @Test
    void updateRequiresFieldsAndExistingRule() throws Exception {
        String admin = bearer(saveUser("hu33-admin-update@example.com", Role.ADMIN));
        EmotionActivityRule rule = saveRule(saveEmotion("Calma", true), saveActivity("Estirar", true), 1, true);

        mockMvc.perform(put(URL + "/{id}", rule.getId())
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debes enviar prioridad o estado"));

        mockMvc.perform(put(URL + "/{id}", 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Regla no encontrada"));
    }

    // ---------- CA02: activar/desactivar sin afectar el historial; el fallback sigue funcionando ----------

    @Test
    void deactivatingRuleDoesNotAlterSavedRecommendationsAndFallbackStillWorks() throws Exception {
        String admin = bearer(saveUser("hu33-admin-history@example.com", Role.ADMIN));
        User student = saveUser("hu33-student-history@example.com", Role.USER);
        Emotion emotion = saveEmotion("Ansiedad", true);
        MicroActivity breathe = saveActivity("Respiracion guiada", true);
        MicroActivity walk = saveActivity("Caminar", true);
        Long ruleId = createRule(admin, emotion, breathe, 1);

        Long recommendationId = recommendationIdFrom(recommend(student, saveCheckin(student, emotion))
                .andExpect(jsonPath("$.activity.id").value(breathe.getId()))
                .andExpect(jsonPath("$.fallbackUsed").value(false))
                .andExpect(jsonPath("$.reason").value("Recomendacion asociada a la emocion Ansiedad.")));
        ActivityRecommendation before = recommendationRepository.findById(recommendationId).orElseThrow();

        mockMvc.perform(put(URL + "/{id}", ruleId)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // La recomendacion historica conserva actividad, motivo y fallback.
        ActivityRecommendation after = recommendationRepository.findById(recommendationId).orElseThrow();
        assertThat(after.getActivity().getId()).isEqualTo(before.getActivity().getId()).isEqualTo(breathe.getId());
        assertThat(after.getReason()).isEqualTo(before.getReason());
        assertThat(after.isFallbackUsed()).isFalse();
        assertThat(after.getCreatedAt()).isEqualTo(before.getCreatedAt());

        mockMvc.perform(get("/api/recommendations/me").header(HttpHeaders.AUTHORIZATION, bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(recommendationId))
                .andExpect(jsonPath("$[0].activity.id").value(breathe.getId()))
                .andExpect(jsonPath("$[0].fallbackUsed").value(false));

        // Sin reglas activas, un check-in nuevo usa el fallback del catalogo activo (primera por titulo).
        recommend(student, saveCheckin(student, emotion))
                .andExpect(jsonPath("$.activity.id").value(walk.getId()))
                .andExpect(jsonPath("$.fallbackUsed").value(true));
    }

    @Test
    void reactivatingRuleAppliesAgainToNewRecommendations() throws Exception {
        String admin = bearer(saveUser("hu33-admin-reactivate@example.com", Role.ADMIN));
        User student = saveUser("hu33-student-reactivate@example.com", Role.USER);
        Emotion emotion = saveEmotion("Tristeza", true);
        saveActivity("Caminar", true);
        MicroActivity writing = saveActivity("Diario de gratitud", true);
        EmotionActivityRule rule = saveRule(emotion, writing, 1, false);

        recommend(student, saveCheckin(student, emotion))
                .andExpect(jsonPath("$.fallbackUsed").value(true));

        mockMvc.perform(put(URL + "/{id}", rule.getId())
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk());

        recommend(student, saveCheckin(student, emotion))
                .andExpect(jsonPath("$.activity.id").value(writing.getId()))
                .andExpect(jsonPath("$.fallbackUsed").value(false));
    }

    @Test
    void changingPriorityChangesNewRecommendationsWithoutCodeChanges() throws Exception {
        String admin = bearer(saveUser("hu33-admin-reorder@example.com", Role.ADMIN));
        User student = saveUser("hu33-student-reorder@example.com", Role.USER);
        Emotion emotion = saveEmotion("Estres", true);
        MicroActivity walk = saveActivity("Caminar", true);
        MicroActivity breathe = saveActivity("Respirar", true);
        Long walkRule = createRule(admin, emotion, walk, 1);
        createRule(admin, emotion, breathe, 2);

        Long firstId = recommendationIdFrom(recommend(student, saveCheckin(student, emotion))
                .andExpect(jsonPath("$.activity.id").value(walk.getId())));

        mockMvc.perform(put(URL + "/{id}", walkRule)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":3}"))
                .andExpect(status().isOk());

        recommend(student, saveCheckin(student, emotion))
                .andExpect(jsonPath("$.activity.id").value(breathe.getId()))
                .andExpect(jsonPath("$.fallbackUsed").value(false));

        assertThat(recommendationRepository.findById(firstId).orElseThrow().getActivity().getId())
                .isEqualTo(walk.getId());
    }

    // ---------- helpers ----------

    private void expectCreateRejected(String admin, String body, String message) throws Exception {
        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    private Long createRule(String admin, Emotion emotion, MicroActivity activity, int priority) throws Exception {
        String response = mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(emotion.getId(), activity.getId(), priority)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(response);
    }

    private Long idOf(String responseBody) {
        return ((Number) JsonPath.read(responseBody, "$.id")).longValue();
    }

    private ResultActions recommend(User user, EmotionalEntry checkin) throws Exception {
        return mockMvc.perform(post("/api/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkinId\":" + checkin.getId() + "}"))
                .andExpect(status().isCreated());
    }

    private Long recommendationIdFrom(ResultActions result) throws Exception {
        return idOf(result.andReturn().getResponse().getContentAsString());
    }

    private String createBody(Long emotionId, Long activityId, int priority) {
        return "{\"emotionId\":" + emotionId + ",\"activityId\":" + activityId + ",\"priority\":" + priority + "}";
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setName("Usuario Reglas");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Clave123"));
        user.setBirthDate(LocalDate.of(2000, 5, 20));
        user.setUniversity("UPC");
        user.setCareer("Ingenieria de Software");
        user.setRole(role);
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
