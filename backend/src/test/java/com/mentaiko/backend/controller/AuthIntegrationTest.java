package com.mentaiko.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.EmotionalEntryRepository;
import com.mentaiko.backend.repository.UserRepository;
import com.mentaiko.backend.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmotionalEntryRepository emotionalEntryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        emotionalEntryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void healthIsPublicAndReturnsJson() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("mentaiko-backend"));
    }

    @Test
    void registerCreatesUserWithEncryptedPasswordAndUserRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Usuario de prueba",
                                  "email": "USUARIO@example.com",
                                  "password": "Clave123",
                                  "birthDate": "2000-05-20",
                                  "university": "UPC",
                                  "career": "Ingenieria de Software"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Usuario de prueba"))
                .andExpect(jsonPath("$.email").value("usuario@example.com"))
                .andExpect(jsonPath("$.birthDate").value("2000-05-20"))
                .andExpect(jsonPath("$.university").value("UPC"))
                .andExpect(jsonPath("$.career").value("Ingenieria de Software"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User saved = userRepository.findByEmailIgnoreCase("usuario@example.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getPasswordHash()).isNotEqualTo("Clave123");
        assertThat(passwordEncoder.matches("Clave123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void registerRejectsInvalidEmailDuplicateShortPasswordFutureDateAndRequiredFields() throws Exception {
        registerUser("duplicado@example.com", "Clave123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterJson("correo-invalido", "Clave123", "2000-05-20")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterJson("DUPLICADO@example.com", "Clave123", "2000-05-20")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El correo ya esta registrado"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterJson("corta@example.com", "123", "2000-05-20")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterJson("future@example.com", "Clave123", "2999-01-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.birthDate").exists());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.birthDate").exists())
                .andExpect(jsonPath("$.errors.university").exists())
                .andExpect(jsonPath("$.errors.career").exists());
    }

    @Test
    void loginReturnsTokenAndUserWithoutPasswordHash() throws Exception {
        registerUser("login@example.com", "Clave123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@example.com","password":"Clave123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void loginRejectsUnknownEmailWrongPasswordAndInactiveUser() throws Exception {
        registerUser("login@example.com", "Clave123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing@example.com","password":"Clave123"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@example.com","password":"Incorrecta123"}
                                """))
                .andExpect(status().isUnauthorized());

        User inactive = userRepository.findByEmailIgnoreCase("login@example.com").orElseThrow();
        inactive.setActive(false);
        userRepository.save(inactive);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@example.com","password":"Clave123"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void profileRequiresTokenAndAcceptsValidToken() throws Exception {
        User user = registerUser("perfil@example.com", "Clave123");
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("perfil@example.com"));
    }

    @Test
    void profileCanUpdateAllowedFieldsOnly() throws Exception {
        User user = registerUser("perfil@example.com", "Clave123");
        String token = jwtService.generateToken(user);

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Nombre actualizado",
                                  "university": "UPC",
                                  "career": "Ingenieria de Software"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nombre actualizado"))
                .andExpect(jsonPath("$.email").value("perfil@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        User updated = userRepository.findByEmailIgnoreCase("perfil@example.com").orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("perfil@example.com");
        assertThat(updated.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void inactiveUserCannotLoadProfile() throws Exception {
        User user = registerUser("inactivo@example.com", "Clave123");
        String token = jwtService.generateToken(user);
        user.setActive(false);
        userRepository.save(user);

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotAccessAdminRoutesButAdminCanPassAuthorization() throws Exception {
        User user = registerUser("usuario@example.com", "Clave123");
        String userToken = jwtService.generateToken(user);

        User admin = registerUser("admin@example.com", "Clave123");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        String adminToken = jwtService.generateToken(admin);

        mockMvc.perform(get("/api/admin/future").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/future").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void corsAllowsAngularLocalOriginAndAuthorizationHeader() throws Exception {
        mockMvc.perform(options("/api/users/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Content-Type")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, not("*")));
    }

    private User registerUser(String email, String password) {
        User user = new User();
        user.setName("Usuario Test");
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setBirthDate(LocalDate.of(2000, 5, 20));
        user.setUniversity("UPC");
        user.setCareer("Ingenieria de Software");
        user.setRole(Role.USER);
        user.setActive(true);
        return userRepository.save(user);
    }

    private String validRegisterJson(String email, String password, String birthDate) {
        return """
                {
                  "name": "Usuario de prueba",
                  "email": "%s",
                  "password": "%s",
                  "birthDate": "%s",
                  "university": "UPC",
                  "career": "Ingenieria de Software"
                }
                """.formatted(email, password, birthDate);
    }
}
