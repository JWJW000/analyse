package com.ethicssra.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceLibraryContentMigrationTest {

    @Test
    void migrationEnrichesResourceLibrariesWithSubstantiveContent() throws IOException {
        Path migration = Path.of("src/main/resources/db/migration/V10__enrich_resource_library_content.sql");

        assertThat(migration).exists();
        String sql = Files.readString(migration);

        assertThat(sql).contains("CHAR_LENGTH(description) < 80");
        assertThat(sql).contains("CHAR_LENGTH(abstract_text) < 180");
        assertThat(sql).contains("教学应用");
        assertThat(sql).contains("讨论问题");
        assertThat(sql).contains("INSERT INTO literature");
    }
}
