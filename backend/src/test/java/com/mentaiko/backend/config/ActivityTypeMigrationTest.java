package com.mentaiko.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.mentaiko.backend.service.ActivityPreferenceService;

/**
 * HU-22: la migracion 012 debe permitir en ambas tablas exactamente los tipos que valida el backend.
 * Evita que ActivityPreferenceService.TYPES y los CHECK de PostgreSQL se desalineen.
 */
class ActivityTypeMigrationTest {

    private static final Path MIGRATION = Path.of("../database/012_activity_type_descanso_ocular.sql");

    @Test
    void migrationCheckConstraintsMatchBackendTypesOnBothTables() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(checkValues(sql, "ck_micro_activity_type"))
                .containsExactlyInAnyOrderElementsOf(ActivityPreferenceService.TYPES);
        assertThat(checkValues(sql, "ck_activity_preference_type"))
                .containsExactlyInAnyOrderElementsOf(ActivityPreferenceService.TYPES);
    }

    @Test
    void migrationSeedsAnExampleActivityForTheNewCategory() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains("INSERT INTO micro_activities")
                .contains("'DESCANSO_OCULAR', NOW(), NOW()")
                .contains("WHERE NOT EXISTS");
    }

    private static java.util.List<String> checkValues(String sql, String constraint) {
        Matcher matcher = Pattern.compile(
                "ADD CONSTRAINT " + constraint + "\\s+CHECK\\(activity_type IN \\(([^)]*)\\)\\)")
                .matcher(sql);
        assertThat(matcher.find()).as("CHECK %s presente en la migracion", constraint).isTrue();
        return Pattern.compile("'([A-Z_]+)'").matcher(matcher.group(1)).results()
                .map(result -> result.group(1))
                .toList();
    }
}
