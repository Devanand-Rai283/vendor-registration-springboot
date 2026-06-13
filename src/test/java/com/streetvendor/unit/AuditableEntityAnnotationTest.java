package com.streetvendor.unit;

import com.streetvendor.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuditableEntityAnnotationTest {

    @Test
    void shouldHaveMappedSuperclassAnnotation() {
        assertTrue(AuditableEntity.class.isAnnotationPresent(MappedSuperclass.class),
                "AuditableEntity should have @MappedSuperclass annotation");
    }

    @Test
    void shouldHaveEntityListenerAnnotation() {
        assertTrue(AuditableEntity.class.isAnnotationPresent(EntityListeners.class),
                "AuditableEntity should have @EntityListeners annotation");
    }

    @Test
    void shouldUseAuditingEntityListener() {
        EntityListeners listeners = AuditableEntity.class.getAnnotation(EntityListeners.class);
        assertNotNull(listeners);
        Class<?>[] value = listeners.value();
        assertEquals(1, value.length);
        assertEquals(AuditingEntityListener.class, value[0]);
    }

    @Test
    void shouldHaveCreatedAtField() throws NoSuchFieldException {
        Field createdAt = AuditableEntity.class.getDeclaredField("createdAt");
        assertNotNull(createdAt, "createdAt field should exist");

        assertTrue(createdAt.isAnnotationPresent(CreatedDate.class),
                "createdAt should have @CreatedDate annotation");

        assertTrue(createdAt.isAnnotationPresent(Column.class),
                "createdAt should have @Column annotation");

        Column column = createdAt.getAnnotation(Column.class);
        assertFalse(column.nullable(), "createdAt should not be nullable");
        assertFalse(column.updatable(), "createdAt should not be updatable");

        assertEquals(Instant.class, createdAt.getType(), "createdAt should be Instant type");
    }

    @Test
    void shouldHaveUpdatedAtField() throws NoSuchFieldException {
        Field updatedAt = AuditableEntity.class.getDeclaredField("updatedAt");
        assertNotNull(updatedAt, "updatedAt field should exist");

        assertTrue(updatedAt.isAnnotationPresent(LastModifiedDate.class),
                "updatedAt should have @LastModifiedDate annotation");

        assertTrue(updatedAt.isAnnotationPresent(Column.class),
                "updatedAt should have @Column annotation");

        Column column = updatedAt.getAnnotation(Column.class);
        assertFalse(column.nullable(), "updatedAt should not be nullable");

        assertEquals(Instant.class, updatedAt.getType(), "updatedAt should be Instant type");
    }

    @Test
    void shouldNotHaveIdField() {
        Field[] fields = AuditableEntity.class.getDeclaredFields();
        for (Field field : fields) {
            assertNotEquals("id", field.getName(), "AuditableEntity should not have an id field");
        }
    }

    @Test
    void shouldHaveGetters() {
        try {
            AuditableEntity.class.getMethod("getCreatedAt");
            AuditableEntity.class.getMethod("getUpdatedAt");
        } catch (NoSuchMethodException e) {
            fail("AuditableEntity should have getCreatedAt() and getUpdatedAt() methods");
        }
    }

    @Test
    void shouldBeAbstractClass() {
        assertTrue(java.lang.reflect.Modifier.isAbstract(AuditableEntity.class.getModifiers()),
                "AuditableEntity should be abstract");
    }
}
