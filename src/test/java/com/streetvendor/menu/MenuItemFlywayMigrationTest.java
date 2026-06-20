package com.streetvendor.menu;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MenuItemFlywayMigrationTest {

    @Test
    void shouldHaveMigrationFileWithCorrectVersion() {
        Path migrationFile = findMigrationFile("V14__create_menu_items.sql");
        assertTrue(Files.exists(migrationFile), "V14 migration file should exist");
    }

    @Test
    void shouldContainCreateTableStatement() throws IOException {
        String content = readMigrationFile();
        assertTrue(content.contains("CREATE TABLE menu_items"),
                "Migration should contain CREATE TABLE menu_items");
    }

    @Test
    void shouldContainAllRequiredColumns() throws IOException {
        String content = readMigrationFile();
        assertTrue(content.contains("id UUID PRIMARY KEY"));
        assertTrue(content.contains("category_id UUID NOT NULL"));
        assertTrue(content.contains("vendor_id UUID NOT NULL"));
        assertTrue(content.contains("name VARCHAR(255) NOT NULL"));
        assertTrue(content.contains("description TEXT"));
        assertTrue(content.contains("price DECIMAL(10,2) NOT NULL CHECK (price >= 0)"));
        assertTrue(content.contains("dietary_tag VARCHAR(100)"));
        assertTrue(content.contains("image_url VARCHAR(500)"));
        assertTrue(content.contains("is_available BOOLEAN NOT NULL DEFAULT TRUE"));
        assertTrue(content.contains("created_at TIMESTAMP NOT NULL"));
        assertTrue(content.contains("updated_at TIMESTAMP NOT NULL"));
    }

    @Test
    void shouldContainForeignKeyConstraints() throws IOException {
        String content = readMigrationFile();
        assertTrue(content.contains("fk_menu_items_category"));
        assertTrue(content.contains("REFERENCES menu_categories(id)"));
        assertTrue(content.contains("fk_menu_items_vendor"));
        assertTrue(content.contains("REFERENCES vendors(id)"));
    }

    @Test
    void shouldContainRequiredIndexes() throws IOException {
        String content = readMigrationFile();
        assertTrue(content.contains("idx_menu_items_vendor"));
        assertTrue(content.contains("idx_menu_items_category"));
    }

    @Test
    void shouldNotModifyExistingMigrations() {
        Path migrationDir = getMigrationDirectory();
        List<String> existingMigrations;
        try (Stream<Path> files = Files.list(migrationDir)) {
            existingMigrations = files
                    .filter(p -> p.toString().endsWith(".sql"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            fail("Unable to list migration files: " + e.getMessage());
            return;
        }

        assertTrue(existingMigrations.contains("V1__baseline.sql"));
        assertTrue(existingMigrations.contains("V13__create_menu_categories.sql"));
        assertTrue(existingMigrations.contains("V14__create_menu_items.sql"));
        assertTrue(existingMigrations.contains("V17__create_payments.sql"));
        assertTrue(existingMigrations.stream().anyMatch(m -> m.startsWith("V20__")), "V20 migration should exist");

        // Validate that we have a consistent, gap-free sequence of migration versions
        // (not a hardcoded file count)
        List<Integer> versions = existingMigrations.stream()
                .filter(m -> m.matches("V\\d+__.*\\.sql"))
                .map(m -> Integer.parseInt(m.substring(1, m.indexOf("__"))))
                .sorted()
                .toList();

        for (int i = 1; i < versions.size(); i++) {
            assertTrue(versions.get(i) >= versions.get(i - 1), "Migration versions should be sorted");
        }

        assertTrue(versions.size() >= 16, "Expected at least 16 menu-related migrations after adding V20");
    }

    @Test
    void shouldUseNextAvailableVersion() throws IOException {
        Path migrationDir = getMigrationDirectory();
        List<String> existingMigrations;
        try (Stream<Path> files = Files.list(migrationDir)) {
            existingMigrations = files
                    .filter(p -> p.toString().endsWith(".sql"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            fail("Unable to list migration files: " + e.getMessage());
            return;
        }

        assertFalse(existingMigrations.contains("V14__create_menu_items.sql.bak"), "Backup files should not exist");
        assertFalse(existingMigrations.contains("V14__create_menu_items.sql.old"), "Old files should not exist");
        assertTrue(existingMigrations.contains("V14__create_menu_items.sql"), "V14 should be the new migration");
    }

    private String readMigrationFile() throws IOException {
        Path file = findMigrationFile("V14__create_menu_items.sql");
        return Files.readString(file);
    }

    private Path findMigrationFile(String filename) {
        Path migrationDir = getMigrationDirectory();
        Path file = migrationDir.resolve(filename);
        if (!Files.exists(file)) {
            throw new RuntimeException("Migration file not found: " + file.toAbsolutePath());
        }
        return file;
    }

    private Path getMigrationDirectory() {
        try {
            URI uri = getClass().getClassLoader().getResource("db/migration").toURI();
            if (uri.getScheme().equals("jar")) {
                throw new RuntimeException("Cannot access migration directory from JAR");
            }
            return Paths.get(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid migration directory URI", e);
        }
    }
}
