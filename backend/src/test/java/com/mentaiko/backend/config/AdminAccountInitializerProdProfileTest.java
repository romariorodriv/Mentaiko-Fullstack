package com.mentaiko.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.profiles.active=prod",
        "spring.datasource.url=jdbc:h2:mem:admin_prod_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=test-secret-for-mentaiko-auth-integration-tests-minimum-32-bytes",
        "app.admin.bootstrap-enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminAccountInitializerProdProfileTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void prodProfileWithBootstrapDisabledDoesNotCreateAdmin() {
        assertThat(userRepository.countByRole(Role.ADMIN)).isZero();
        assertThat(userRepository.count()).isZero();
    }
}
