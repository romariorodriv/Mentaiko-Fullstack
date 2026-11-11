package com.mentaiko.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import com.mentaiko.backend.MentaikoBackendApplication;

/** Opt-in, exclusively for a disposable PostgreSQL 17 database on loopback. */
@EnabledIfSystemProperty(named = "postgres.test.url", matches = "jdbc:postgresql://127\\.0\\.0\\.1:55439/mentaiko_reset_test")
class PostgresPasswordResetMigrationTest {
    @Test
    void additiveMigrationIsIdempotentAndProductionHibernateValidationStarts() throws Exception {
        String url = System.getProperty("postgres.test.url");
        String[] args = {
            "--spring.profiles.active=prod", "--server.port=0", "--server.address=127.0.0.1",
            "--spring.datasource.url=" + url, "--spring.datasource.username=postgres", "--spring.datasource.password=",
            "--spring.datasource.driver-class-name=org.postgresql.Driver", "--spring.jpa.show-sql=false",
            "--app.admin.bootstrap-enabled=false", "--spring.mail.host=127.0.0.1", "--spring.mail.port=1",
            "--app.mail.from=test@example.invalid", "--app.public-url=https://mentaiko-app.duckdns.org",
            "--jwt.secret=disposable-postgres-test-key-at-least-thirty-two-bytes",
            "--jwt.expiration=60000", "--spring.jpa.hibernate.ddl-auto=create"
        };
        try (var initial = new SpringApplicationBuilder(MentaikoBackendApplication.class).run(args)) {
            assertThat(initial.isActive()).isTrue();
        }
        try (var connection = DriverManager.getConnection(url, "postgres", ""); var sql = connection.createStatement()) {
            // Reconstruct the schema immediately before HU deployment.
            sql.execute("DROP TABLE password_reset_tokens");
            sql.execute("ALTER TABLE users DROP COLUMN auth_version, DROP COLUMN password_reset_requested_at");
            sql.execute(Files.readString(Path.of("../database/001_password_reset_tokens.sql")));
            String migration = Files.readString(Path.of("../database/002_password_reset_security.sql"));
            sql.execute(migration);
            sql.execute(migration);
        }
        args[args.length - 1] = "--spring.jpa.hibernate.ddl-auto=validate";
        try (var validated = new SpringApplicationBuilder(MentaikoBackendApplication.class).run(args)) {
            assertThat(validated.isActive()).isTrue();
            assertThat(validated.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
            String base = "http://127.0.0.1:" + validated.getEnvironment().getRequiredProperty("local.server.port");
            try (var client = java.net.http.HttpClient.newHttpClient()) {
                var health = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/api/health")).GET().build(),
                        java.net.http.HttpResponse.BodyHandlers.ofString());
                assertThat(health.statusCode()).isEqualTo(200);
                assertThat(health.body()).contains("\"status\":\"ok\"");
                for (int i = 0; i < 11; i++) {
                    assertThat(sendInvalidRequest(client, base, "198.51.100.10")).isEqualTo(i < 10 ? 400 : 429);
                }
                // Native Tomcat resolves client addresses only from the loopback proxy.
                assertThat(sendInvalidRequest(client, base, "198.51.100.11")).isEqualTo(400);
            }
        }
    }

    private int sendInvalidRequest(java.net.http.HttpClient client, String base, String clientIp) throws Exception {
        var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/api/auth/forgot-password"))
                .header("Content-Type", "application/json").header("X-Forwarded-For", clientIp)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"email\":\"bad\"}")).build();
        return client.send(request, java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
