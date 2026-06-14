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

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

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
    void shouldUpdateUpdatedAtAndKeepCreatedAtUnchangedOnModify() throws InterruptedException {
        TestAuditableEntity entity = new TestAuditableEntity("Initial");
        TestAuditableEntity saved = repository.saveAndFlush(entity);

        assertNotNull(saved.getId(), "ID should be set after persist");

        Instant originalCreatedAt = saved.getCreatedAt();
        Instant originalUpdatedAt = saved.getUpdatedAt();

        // Detach the entity so the next save() is a true merge(), causing
        // AuditingEntityListener to fire @LastModifiedDate.
        // Without detach(), the entity remains managed and save()+flush()
        // does not re-invoke the auditing listener.
        entityManager.detach(saved);

        // Ensure at least 1 ms passes so Instant.isAfter() is reliable
        // on machines where the JPA clock and system clock share millisecond resolution.
        Thread.sleep(1);

        saved.setName("Modified");

        TestAuditableEntity result = repository.save(saved);
        repository.flush();

        assertEquals(originalCreatedAt, result.getCreatedAt(), "createdAt should remain unchanged after update");
        assertTrue(result.getUpdatedAt().isAfter(originalUpdatedAt), "updatedAt should be after update");
    }
}
