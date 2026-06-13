package com.streetvendor.integration;

import com.streetvendor.common.audit.TestAuditableEntity;
import com.streetvendor.common.audit.TestAuditableEntityRepository;
import com.streetvendor.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("audit-test")
@Transactional
class AuditableEntityPersistenceTest extends AbstractIntegrationTest {

    @Autowired
    private TestAuditableEntityRepository repository;

    @Test
    void shouldPopulateCreatedAtAndUpdatedAtOnPersist() {
        TestAuditableEntity entity = new TestAuditableEntity("Test Item");

        TestAuditableEntity saved = repository.saveAndFlush(entity);

        assertNotNull(saved.getCreatedAt(), "createdAt should not be null after persist");
        assertNotNull(saved.getUpdatedAt(), "updatedAt should not be null after persist");
    }

    @Test
    void shouldSetCreatedAtToUtcTimestamp() {
        Instant before = Instant.now();
        TestAuditableEntity entity = new TestAuditableEntity("Timestamp Test");

        TestAuditableEntity saved = repository.saveAndFlush(entity);
        Instant after = Instant.now();

        assertNotNull(saved.getCreatedAt());
        assertFalse(saved.getCreatedAt().isBefore(before), "createdAt should not be before test started");
        assertFalse(saved.getCreatedAt().isAfter(after), "createdAt should not be after test ended");
    }

    @Test
    void shouldSetUpdatedAtToUtcTimestamp() {
        Instant before = Instant.now();
        TestAuditableEntity entity = new TestAuditableEntity("Timestamp Test");

        TestAuditableEntity saved = repository.saveAndFlush(entity);
        Instant after = Instant.now();

        assertNotNull(saved.getUpdatedAt());
        assertFalse(saved.getUpdatedAt().isBefore(before), "updatedAt should not be before test started");
        assertFalse(saved.getUpdatedAt().isAfter(after), "updatedAt should not be after test ended");
    }

    @Test
    void shouldUpdateUpdatedAtAndKeepCreatedAtUnchangedOnModify() {
        TestAuditableEntity entity = new TestAuditableEntity("Initial");
        TestAuditableEntity saved = repository.saveAndFlush(entity);

        Instant originalCreatedAt = saved.getCreatedAt();
        Instant originalUpdatedAt = saved.getUpdatedAt();

        saved.setName("Modified");
        TestAuditableEntity updated = repository.saveAndFlush(saved);

        assertEquals(originalCreatedAt, updated.getCreatedAt(), "createdAt should remain unchanged after update");
        assertNotEquals(originalUpdatedAt, updated.getUpdatedAt(), "updatedAt should change after update");
        assertTrue(updated.getUpdatedAt().isAfter(originalUpdatedAt) || updated.getUpdatedAt().compareTo(originalUpdatedAt) == 0,
                "updatedAt should be after or equal to original");
    }
}
