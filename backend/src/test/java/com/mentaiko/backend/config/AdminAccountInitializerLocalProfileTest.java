package com.mentaiko.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.datasource.url=jdbc:h2:mem:admin_local_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=test-secret-for-mentaiko-auth-integration-tests-minimum-32-bytes"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminAccountInitializerLocalProfileTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminAccountInitializer initializer;

    @Test
    void localProfileCreatesExactlyOneAdminWithBcryptAndIsIdempotent() {
        assertThat(userRepository.countByRole(Role.ADMIN)).isEqualTo(1);

        User admin = userRepository.findByEmailIgnoreCase("administrador@mentaiko.com").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getPasswordHash()).isNotEqualTo("Mentaiko123454$");
        assertThat(admin.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches("Mentaiko123454$", admin.getPasswordHash())).isTrue();

        initializer.bootstrapAdminIfNeeded();

        assertThat(userRepository.countByRole(Role.ADMIN)).isEqualTo(1);
        assertThat(userRepository.count()).isEqualTo(1);
    }
}
