package com.streetvendor.integration;

import com.streetvendor.auth.entity.AccountStatus;
import com.streetvendor.auth.entity.Role;
import com.streetvendor.auth.entity.User;
import com.streetvendor.vendor.entity.Vendor;
import com.streetvendor.vendor.enums.VendorStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VendorEntityTest {

    @Test
    void shouldDefaultRejectionReasonToNull() {
        User user = new User(UUID.randomUUID(), "test@example.com", "hash", Role.VENDOR, AccountStatus.ACTIVE);
        Vendor vendor = new Vendor(UUID.randomUUID(), user, "Test Business");

        assertNull(vendor.getRejectionReason());
    }

    @Test
    void shouldSetAndGetRejectionReason() {
        User user = new User(UUID.randomUUID(), "test@example.com", "hash", Role.VENDOR, AccountStatus.ACTIVE);
        Vendor vendor = new Vendor(UUID.randomUUID(), user, "Test Business");

        vendor.setRejectionReason("Expired FSSAI certificate");
        assertEquals("Expired FSSAI certificate", vendor.getRejectionReason());
    }
}
