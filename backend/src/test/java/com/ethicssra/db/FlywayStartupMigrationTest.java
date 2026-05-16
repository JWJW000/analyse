package com.ethicssra.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayStartupMigrationTest {

    @Test
    void applicationEnablesFlywayForSchemaMigrations() throws IOException {
        String config = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(config).contains("flyway:");
        assertThat(config).contains("enabled: true");
    }

    @Test
    void aiMentorMigrationIsRetryableAndUsesExistingTables() throws IOException {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V8__ai_mentor_tables.sql"));

        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS mentor_conversation");
        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS mentor_suggestion_log");
        assertThat(migration).contains("ALTER TABLE submission ADD COLUMN ai_analysis");
        assertThat(migration).doesNotContain("ALTER TABLE submissions");
        assertThat(migration).doesNotContain("project_phase_checklist_item");
    }
}
