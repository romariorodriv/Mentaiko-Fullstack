package com.mentaiko.backend.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.enums.Role;
import com.mentaiko.backend.repository.UserRepository;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final AdminAccountProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public AdminAccountInitializer(
            AdminAccountProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Environment environment
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrapAdminIfNeeded();
    }

    @Transactional
    public void bootstrapAdminIfNeeded() {
        if (!properties.isBootstrapEnabled()) {
            log.info("Admin account bootstrap is disabled for profiles={}",
                    Arrays.toString(environment.getActiveProfiles()));
            return;
        }

        long adminCount = userRepository.countByRole(Role.ADMIN);
        if (adminCount > 1) {
            log.error("Admin account bootstrap detected multiple ADMIN users. No records were changed.");
            return;
        }
        if (adminCount == 1) {
            log.info("Admin account bootstrap skipped because an ADMIN already exists");
            return;
        }

        validateProperties();
        String email = properties.getEmail().trim().toLowerCase();
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new IllegalStateException("Configured admin email already exists without ADMIN role");
        });

        User admin = new User();
        admin.setName(properties.getName().trim());
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
        admin.setBirthDate(properties.getBirthDate());
        admin.setUniversity(properties.getUniversity().trim());
        admin.setCareer(properties.getCareer().trim());
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        User savedAdmin = userRepository.save(admin);
        log.info("Local admin account created with id={}", savedAdmin.getId());
    }

    private void validateProperties() {
        if (isBlank(properties.getName())
                || isBlank(properties.getEmail())
                || isBlank(properties.getPassword())
                || properties.getBirthDate() == null
                || isBlank(properties.getUniversity())
                || isBlank(properties.getCareer())) {
            throw new IllegalStateException("Admin bootstrap is enabled but admin configuration is incomplete");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
