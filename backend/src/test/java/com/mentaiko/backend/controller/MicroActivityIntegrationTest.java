package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInRelativeOrder;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.mentaiko.backend.entity.MicroActivity;
import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.MicroActivityRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.security.JwtService;
import com.mentaiko.backend.service.MicroActivityService;

@SpringBootTest
@AutoConfigureMockMvc
class MicroActivityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MicroActivityRepository microActivityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MicroActivityService microActivityService;

    @BeforeEach
    void setUp() {
        microActivityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void listWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/micro-activities"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentOnlySeesActiveActivitiesEvenWhenRequestingAll() throws Exception {
        MicroActivity active = saveActivity("Respirar", "Respira lentamente", 3, true);
        saveActivity("Pausa", "Descansa unos minutos", 5, false);
        String token = tokenFor(Role.USER);

        mockMvc.perform(get("/api/micro-activities")
                        .param("activeOnly", "false")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(active.getId()))
                .andExpect(jsonPath("$[0].title").value("Respirar"))
                .andExpect(jsonPath("$[0].description").value("Respira lentamente"))
                .andExpect(jsonPath("$[0].durationMinutes").value(3))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].normalizedTitle").doesNotExist());
    }

    @Test
    void adminCanListActiveAndInactiveActivitiesInStableOrder() throws Exception {
        saveActivity("Tomar agua", "Bebe un vaso de agua", 2, true);
        saveActivity("Caminar", "Camina con calma", 10, false);
        saveActivity("Respirar", "Respira lentamente", 3, true);
        String token = tokenFor(Role.ADMIN);

        mockMvc.perform(get("/api/micro-activities")
                        .param("activeOnly", "false")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].title", containsInRelativeOrder(
                        "Caminar",
                        "Respirar",
                        "Tomar agua"
                )));
    }

    @Test
    void createRequiresAdminRole() throws Exception {
        String userToken = tokenFor(Role.USER);
        String body = validJson("Respirar", "Respira lentamente", 3);

        mockMvc.perform(post("/api/admin/micro-activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/micro-activities")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesNormalizedActivityActiveByDefault() throws Exception {
        String token = tokenFor(Role.ADMIN);

        mockMvc.perform(post("/api/admin/micro-activities")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  Pausa   consciente  ",
                                  "description": "  Respira   de forma   lenta.  ",
                                  "durationMinutes": 4,
                                  "active": false,
                                  "id": 99
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Pausa consciente"))
                .andExpect(jsonPath("$.description").value("Respira de forma lenta."))
                .andExpect(jsonPath("$.durationMinutes").value(4))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.normalizedTitle").doesNotExist());

        assertThat(microActivityRepository.existsByNormalizedTitle("pausa consciente")).isTrue();
    }

    @Test
    void createRejectsInvalidFieldsAndDuplicateTitle() throws Exception {
        String token = tokenFor(Role.ADMIN);
        saveActivity("Pausa consciente", "Respira lentamente", 4, true);

        mockMvc.perform(post("/api/admin/micro-activities")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson("   ", "   ", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.durationMinutes").exists());

        mockMvc.perform(post("/api/admin/micro-activities")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson("  PAUSA   CONSCIENTE ", "Otra descripcion", 5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una microactividad con ese titulo"));
    }

    @Test
    void adminCanUpdateFieldsAndToggleStateWithoutDeletingRecord() throws Exception {
        MicroActivity activity = saveActivity("Respirar", "Respira lentamente", 3, true);
        String token = tokenFor(Role.ADMIN);

        mockMvc.perform(put("/api/admin/micro-activities/{id}", activity.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " Respiracion guiada ",
                                  "description": " Inhala y exhala con calma ",
                                  "durationMinutes": 6
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Respiracion guiada"))
                .andExpect(jsonPath("$.description").value("Inhala y exhala con calma"))
                .andExpect(jsonPath("$.durationMinutes").value(6))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(put("/api/admin/micro-activities/{id}", activity.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertThat(microActivityRepository.findById(activity.getId())).isPresent();
    }

    @Test
    void updateRejectsEmptyRequestMissingIdAndDuplicateTitle() throws Exception {
        MicroActivity first = saveActivity("Respirar", "Respira lentamente", 3, true);
        saveActivity("Caminar", "Camina con calma", 10, true);
        String token = tokenFor(Role.ADMIN);

        mockMvc.perform(put("/api/admin/micro-activities/{id}", first.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debes enviar al menos un campo modificable"));

        mockMvc.perform(put("/api/admin/micro-activities/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Microactividad no encontrada"));

        mockMvc.perform(put("/api/admin/micro-activities/{id}", first.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  CAMINAR  \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una microactividad con ese titulo"));
    }

    @Test
    void studentCannotUpdateActivity() throws Exception {
        MicroActivity activity = saveActivity("Respirar", "Respira lentamente", 3, true);
        String token = tokenFor(Role.USER);

        mockMvc.perform(put("/api/admin/micro-activities/{id}", activity.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    private MicroActivity saveActivity(
            String title,
            String description,
            int durationMinutes,
            boolean active
    ) {
        MicroActivity activity = new MicroActivity();
        activity.setTitle(title);
        activity.setNormalizedTitle(microActivityService.normalizeForUniqueness(title));
        activity.setDescription(description);
        activity.setDurationMinutes(durationMinutes);
        activity.setActive(active);
        return microActivityRepository.save(activity);
    }

    private String tokenFor(Role role) {
        User user = new User();
        user.setName("Usuario Test");
        user.setEmail(role.name().toLowerCase() + "-activities@example.com");
        user.setPasswordHash(passwordEncoder.encode("Clave123"));
        user.setBirthDate(LocalDate.of(2000, 5, 20));
        user.setUniversity("UPC");
        user.setCareer("Ingenieria de Software");
        user.setRole(role);
        user.setActive(true);
        return jwtService.generateToken(userRepository.save(user));
    }

    private String validJson(String title, String description, int durationMinutes) {
        return """
                {
                  "title": "%s",
                  "description": "%s",
                  "durationMinutes": %d
                }
                """.formatted(title, description, durationMinutes);
    }
}
