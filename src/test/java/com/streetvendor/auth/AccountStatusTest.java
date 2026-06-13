package com.streetvendor.auth;

import com.streetvendor.auth.entity.AccountStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AccountStatusTest {

    @Test
    void shouldHaveThreeValues() {
        assertEquals(3, AccountStatus.values().length);
    }

    @Test
    void shouldContainActive() {
        assertNotNull(AccountStatus.ACTIVE);
        assertEquals("ACTIVE", AccountStatus.ACTIVE.name());
    }

    @Test
    void shouldContainSuspended() {
        assertNotNull(AccountStatus.SUSPENDED);
        assertEquals("SUSPENDED", AccountStatus.SUSPENDED.name());
    }

    @Test
    void shouldContainDeleted() {
        assertNotNull(AccountStatus.DELETED);
        assertEquals("DELETED", AccountStatus.DELETED.name());
    }

    @Test
    void shouldParseFromString() {
        assertEquals(AccountStatus.ACTIVE, AccountStatus.valueOf("ACTIVE"));
        assertEquals(AccountStatus.SUSPENDED, AccountStatus.valueOf("SUSPENDED"));
        assertEquals(AccountStatus.DELETED, AccountStatus.valueOf("DELETED"));
    }
}
