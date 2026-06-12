# Flyway Skill

## Purpose

Ensure all database schema changes are versioned, reproducible, and safe.

Database modifications must never occur outside Flyway migrations.

---

## When to Use

Activate whenever:

* Creating new entities
* Adding columns
* Modifying constraints
* Adding indexes
* Creating tables
* Introducing relationships
* Refactoring database structures

---

## Migration Principles

All schema changes must be implemented through Flyway migrations.

Never rely on:

* Hibernate auto-DDL
* Manual SQL execution in production
* Database-specific hot fixes

---

## Migration Naming Convention

Format:

V{version}__{description}.sql

Examples:

V1__create_users.sql

V2__create_customers.sql

V7__create_orders.sql

---

## Versioning Rules

Requirements:

* Versions must be sequential.
* Existing migrations must never be modified.
* New migrations must always create forward-only changes.

---

## Table Standards

Requirements:

* UUID primary keys.
* Audit timestamps where applicable.
* Foreign key constraints.
* Appropriate indexes.
* Explicit NOT NULL declarations.

---

## Index Guidelines

Create indexes for:

* Frequently filtered columns.
* Foreign keys when beneficial.
* Discovery-related location fields.

Example:

CREATE INDEX idx_vendors_location
ON vendors(latitude, longitude);

---

## Constraint Guidelines

Enforce business rules at the database layer whenever appropriate.

Examples:

UNIQUE(email)

UNIQUE(order_id)

CHECK(price >= 0)

CHECK(quantity > 0)

---

## Rollback Philosophy

Migrations are forward-only.

Fixes require:

* New migrations.
* Data repair migrations.

Never edit historical migrations.

---

## Validation Checklist

✓ Flyway migration created.

✓ Naming convention respected.

✓ Constraints added.

✓ Indexes evaluated.

✓ Foreign keys validated.

✓ UUID standards maintained.

✓ Historical migrations untouched.

---

## Anti-Patterns

Avoid:

* Editing executed migrations.

* Using Hibernate auto-update in production.

* Skipping database constraints.

* Introducing schema changes without migrations.

* Using integer IDs for new entities.
