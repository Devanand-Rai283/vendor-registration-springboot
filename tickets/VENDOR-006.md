# VENDOR-006: Audit Log Infrastructure

**Priority:** MUST HAVE
**Status:** READY
**Dependencies:** None
**Unblocks:** VENDOR-004 Task 6 (Audit Logging for Approve/Reject)

---

## Problem Statement

VENDOR-004 Task 6 requires writing `VENDOR_APPROVED` and `VENDOR_REJECTED` audit
events whenever an admin approves or rejects a vendor application. Currently, no
audit logging infrastructure exists — no `AuditLog` entity, no audit repository,
no audit service, no Flyway migration for an audit table, and no event publishing
mechanism.

This ticket creates the foundational infrastructure so that VENDOR-004 Task 6 can
integrate audit logging into the existing approve/reject workflows.

---

## Scope

This ticket covers ONLY the audit logging infrastructure:

- [ ] Create `V8__create_audit_log_table.sql` Flyway migration
- [ ] Create `AuditEventType` enum (`VENDOR_APPROVED`, `VENDOR_REJECTED` + reserved space)
- [ ] Create `AuditLog` entity extending `AuditableEntity`
- [ ] Create `AuditLogRepository`
- [ ] Create `AuditService` with `logEvent(AuditEventType, UUID vendorId, UUID adminId, String details)`
- [ ] Add tests for entity, repository, migration, and service

---

## Out of Scope

- Integrating audit logging into approve/reject workflows (VENDOR-004 Task 6)
- Any other VENDOR-004 functionality
- IP address capture (requires request context infrastructure)
- GET audit log retrieval endpoints
- Audit log retention policies
- Admin user resolution beyond storing `admin_user_id`

---

## Implementation Plan

### 1. Flyway Migration — `V8__create_audit_log_table.sql`

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    vendor_id UUID NOT NULL,
    admin_user_id UUID,
    details VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    CONSTRAINT fk_audit_admin FOREIGN KEY (admin_user_id) REFERENCES users(id)
);
```

- `admin_user_id` is nullable — covers the case where the admin user ID may not
  be available in all contexts.
- `details` is nullable and capped at 500 chars — stores rejection reason for
  reject events, NULL for approve events.
- `created_at`/`updated_at` handled by `AuditableEntity`.
- Foreign keys to `vendors` and `users` tables for referential integrity.

### 2. AuditEventType Enum — `AuditEventType.java`

Package: `com.streetvendor.common.audit`

```java
public enum AuditEventType {
    VENDOR_APPROVED,
    VENDOR_REJECTED
}
```

Kept in `common.audit` alongside existing `AuditableEntity` and `AuditConfig`.

### 3. AuditLog Entity — `AuditLog.java`

Package: `com.streetvendor.common.audit`

```java
@Entity
@Table(name = "audit_logs")
public class AuditLog extends AuditableEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "admin_user_id")
    private UUID adminUserId;

    @Column(length = 500)
    private String details;

    // Constructor, getters, setters
}
```

- Uses `@Enumerated(EnumType.STRING)` for readable DB values.
- UUID primary key generated in constructor (consistent with `Vendor.java` pattern).
- `AuditableEntity` provides `createdAt`/`updatedAt`.

### 4. AuditLogRepository — `AuditLogRepository.java`

```java
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
```

Minimal repository — no query methods needed for writing events.

### 5. AuditService — `AuditService.java`

```java
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logEvent(AuditEventType eventType, UUID vendorId, UUID adminUserId, String details) {
        AuditLog auditLog = new AuditLog(
            UUID.randomUUID(),
            eventType,
            vendorId,
            adminUserId,
            details
        );
        auditLogRepository.save(auditLog);
    }
}
```

- Single `logEvent()` method for writing events.
- `@Transactional` integration: when called from within an existing transaction
  (e.g., `VendorServiceImpl.approveVendor`), participates in the same transactional
  boundary by default.

### 6. Tests

#### Migration Test
Verify `audit_logs` table exists with expected columns via schema validation.

#### Entity Test
- New `AuditLog` has all fields set correctly.
- `AuditableEntity` timestamps are null before persist.

#### Repository Test
- Save and retrieve an audit log entry.
- Verify foreign key constraint works with a real Vendor + User.

#### Service Test
- `logEvent()` saves the expected event.
- Idempotent: calling multiple times creates multiple entries.

---

## Files to Create

| File | Description |
|---|---|
| `src/main/resources/db/migration/V8__create_audit_log_table.sql` | Flyway migration |
| `src/main/java/.../common/audit/AuditEventType.java` | Enum for event types |
| `src/main/java/.../common/audit/AuditLog.java` | JPA entity |
| `src/main/java/.../common/audit/AuditLogRepository.java` | Spring Data JPA repository |
| `src/main/java/.../common/audit/AuditService.java` | Service layer for writing events |
| `src/test/java/.../integration/AuditLogSchemaTest.java` | Schema validation test |
| `src/test/java/.../common/audit/AuditLogEntityTest.java` | Entity unit test |
| `src/test/java/.../common/audit/AuditLogRepositoryTest.java` | Repository test |
| `src/test/java/.../common/audit/AuditServiceTest.java` | Service unit test |

## Files to Modify

None.

---

## Dependency Chain

```
VENDOR-006 (this ticket)
        ↓
VENDOR-004 Task 6 (Audit Integration — injects AuditService into VendorServiceImpl)
        ↓
Remaining VENDOR-004 tasks
```

---

## Verification Checklist

- [ ] `mvn compile` passes
- [ ] `V8__create_audit_log_table.sql` exists with correct DDL
- [ ] `AuditEventType` enum has `VENDOR_APPROVED` and `VENDOR_REJECTED`
- [ ] `AuditLog` entity maps to `audit_logs` table
- [ ] `AuditLogRepository` compiles and saves entries
- [ ] `AuditService.logEvent()` persists correctly
- [ ] No existing migrations, entities, or services modified
- [ ] No unrelated functionality introduced
