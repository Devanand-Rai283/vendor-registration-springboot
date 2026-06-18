package com.streetvendor.rating;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RatingFlywayMigrationTest {

    @Test
    void shouldHaveV18MigrationFile() {
        Path file = findMigrationFile("V18__create_ratings.sql");
        assertTrue(Files.exists(file), "V18 migration file should exist");
    }

    @Test
    void shouldContainCreateTableStatement() throws IOException {
        String content = readMigrationFile();
        assertTrue(content.contains("CREATE TABLE ratings"),
                "Migration should contain CREATE TABLE ratings");
    }

    @Test
    void shouldContainAllRequiredColumns() throws IOException {
        String content = readMigrationFile();
        assertTrue(content.contains("id UUID PRIMARY KEY"));
        assertTrue(content.contains("order_id UUID NOT NULL"));
        assertTrue(content.contains("customer_id UUID NOT NULL"));
        assertTrue(content.contains("vendor_id UUID NOT NULL"));
        assertTrue(content.contains("stars INTEGER NOT NULL"));
        assertTrue(content.contains("review_text TEXT"));
        assertTrue(content.contains("created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"));
    }

    @Test
    void shouldContainForeignKeyAndUniqueConstraints() throws IOException {
        String content = readMigrationFile();
        assertTrue(content.contains("CONSTRAINT fk_ratings_order_id FOREIGN KEY (order_id) REFERENCES orders(id)"));
        assertTrue(content.contains("CONSTRAINT fk_ratings_customer_id FOREIGN KEY (customer_id) REFERENCES customers(id)"));
        assertTrue(content.contains("CONSTRAINT fk_ratings_vendor_id FOREIGN KEY (vendor_id) REFERENCES vendors(id)"));
        assertTrue(content.contains("CONSTRAINT uk_ratings_order_id UNIQUE (order_id)"));
        assertTrue(content.contains("CONSTRAINT chk_ratings_stars CHECK (stars BETWEEN 1 AND 5)"));
    }

    @Test
    void shouldContainRequiredIndexes() throws IOException {
        String content = readMigrationFile();
        assertTrue(content.contains("CREATE INDEX idx_ratings_customer_id ON ratings(customer_id);"));
        assertTrue(content.contains("CREATE INDEX idx_ratings_vendor_id ON ratings(vendor_id);"));
    }

    private String readMigrationFile() throws IOException {
        Path file = findMigrationFile("V18__create_ratings.sql");
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
