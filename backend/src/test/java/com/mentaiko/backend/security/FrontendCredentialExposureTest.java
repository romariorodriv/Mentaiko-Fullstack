package com.mentaiko.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class FrontendCredentialExposureTest {

    @Test
    void frontendSourceDoesNotContainLocalAdminCredentials() throws Exception {
        Path frontendSource = Path.of("..", "frontend", "src");
        List<Path> files;
        try (var stream = Files.walk(frontendSource)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.endsWith(".ts") || name.endsWith(".html") || name.endsWith(".css");
                    })
                    .toList();
        }

        for (Path file : files) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            assertThat(content)
                    .as("frontend file %s", file)
                    .doesNotContain("administrador@mentaiko.com")
                    .doesNotContain("Mentaiko123454$");
        }
    }
}
