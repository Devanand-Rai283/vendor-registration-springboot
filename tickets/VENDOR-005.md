# VENDOR-005: Vendor Rejection Reason Persistence

**Priority:** MUST HAVE
**Status:** READY
**Dependencies:** None
**Unblocks:** VENDOR-004 Task 4 (Admin Reject Vendor Endpoint)

---

## Problem Statement

The `POST /api/admin/vendors/{id}/reject` endpoint (VENDOR-004 Task 4) requires
persisting a rejection reason when an admin rejects a vendor application.
Currently, neither the `Vendor` entity nor the `vendors` database table have
a field/column for storing this reason.

---

## Scope

This ticket covers ONLY the infrastructure for rejection reason persistence:

- [x] Add `rejection_reason` column to `vendors` table via Flyway migration
- [x] Add `rejectionReason` field to `Vendor` entity
- [x] Add `rejectionReason` field to `VendorResponse` DTO (for return to client)
- [x] Add tests for the migration and entity changes

---

## Out of Scope

- Admin reject endpoint (`POST /api/admin/vendors/{id}/reject`)
- Admin approve endpoint changes
- Any other VENDOR-004 functionality
- Audit logging
- Rejection reason validation beyond `@NotBlank` (already exists in `RejectVendorRequest`)

---

## Implementation Plan

### 1. Flyway Migration — `V7__add_rejection_reason.sql`

Create a new migration file that adds the column:

```sql
ALTER TABLE vendors ADD COLUMN rejection_reason TEXT NULL;
```

- Column is nullable (not all vendors are rejected).
- Uses `TEXT` type to accommodate arbitrary-length reasons.
- Must NOT modify the existing `V5__create_vendors.sql`.

### 2. Vendor Entity — `Vendor.java`

Add field and accessors:

```java
@Column(name = "rejection_reason", columnDefinition = "text")
private String rejectionReason;
```

- Use `@Column` annotation consistent with existing fields (e.g. `description`).
- Add getter/setter following the existing pattern.
- No constructor changes; null by default.

### 3. VendorResponse DTO — `VendorResponse.java`

Add field:

```java
String rejectionReason
```

- Follows existing record style.
- When null, Jackson will omit it (or include as null — consistent with existing
  `@JsonInclude(NON_NULL)` on `ApiResponse`; the record itself has no such
  annotation, so null will serialize as `null`).

### 4. Tests

#### Schema Integration Test

Extend `VendorSchemaTest` (or create a new test class) to verify:

- `rejection_reason` column exists in the `vendors` table.

#### Entity Unit Test

Verify:

- New Vendor has `rejectionReason` as `null`.
- After setting rejection reason, getter returns the expected value.

---

## Files to Create

| File | Description |
|---|---|
| `src/main/resources/db/migration/V7__add_rejection_reason.sql` | Flyway migration |

## Files to Modify

| File | Change |
|---|---|
| `src/main/java/.../vendor/entity/Vendor.java` | Add `rejectionReason` field + getter/setter |
| `src/main/java/.../vendor/dto/VendorResponse.java` | Add `String rejectionReason` field |
| `src/test/java/.../integration/VendorSchemaTest.java` | Add column assertion |

---

## Dependency Chain

```
VENDOR-005 (this ticket)
        ↓
VENDOR-004 Task 4 (Reject Endpoint — uses rejection_reason column + RejectVendorRequest)
        ↓
Remaining VENDOR-004 tasks
```

---

## Verification Checklist

- [ ] `mvn compile` passes
- [ ] `V7__add_rejection_reason.sql` exists and is correct
- [ ] `Vendor.java` has `rejectionReason` field with getter/setter
- [ ] `VendorResponse.java` includes `rejectionReason`
- [ ] Schema test confirms `rejection_reason` column exists
- [ ] No existing migrations modified
- [ ] No unrelated functionality introduced
