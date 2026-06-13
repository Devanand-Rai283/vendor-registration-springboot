package com.streetvendor.auth;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RoleTest {

    @Test
    void shouldHaveThreeValues() {
        assertEquals(3, Role.values().length);
    }

    @Test
    void shouldContainCustomer() {
        assertNotNull(Role.CUSTOMER);
        assertEquals("CUSTOMER", Role.CUSTOMER.name());
    }

    @Test
    void shouldContainVendor() {
        assertNotNull(Role.VENDOR);
        assertEquals("VENDOR", Role.VENDOR.name());
    }

    @Test
    void shouldContainAdmin() {
        assertNotNull(Role.ADMIN);
        assertEquals("ADMIN", Role.ADMIN.name());
    }

    @Test
    void shouldParseFromString() {
        assertEquals(Role.CUSTOMER, Role.valueOf("CUSTOMER"));
        assertEquals(Role.VENDOR, Role.valueOf("VENDOR"));
        assertEquals(Role.ADMIN, Role.valueOf("ADMIN"));
    }
}
