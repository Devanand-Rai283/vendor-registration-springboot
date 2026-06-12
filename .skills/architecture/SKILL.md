# Architecture Skill

## Purpose

Enforce the Street Vendor Platform modular monolith architecture and prevent architectural drift.

---

## When to Use

Activate this skill whenever:

* Creating a new backend feature
* Modifying existing backend modules
* Refactoring code
* Designing new APIs
* Introducing new entities or services
* Reviewing pull requests

---

## Architecture Style

The system follows a **Modular Monolith** architecture.

Modules:

* auth
* customer
* vendor
* menu
* discovery
* order
* payment
* rating
* analytics
* admin

---

## Layer Responsibilities

Controller
↓
Service
↓
Repository

### Controllers

Responsibilities:

* Receive requests
* Validate DTOs
* Delegate to services
* Return HTTP responses

Must NOT:

* Contain business logic
* Access repositories directly
* Implement workflows

---

### Services

Responsibilities:

* Implement use cases
* Coordinate domain operations
* Enforce business rules
* Manage transactions

Must NOT:

* Handle HTTP concerns
* Return ResponseEntity objects

---

### Repositories

Responsibilities:

* Persistence operations
* Database querying

Must NOT:

* Implement business rules
* Call other services

---

## DTO Rules

Use DTOs for all API boundaries.

Never expose JPA entities directly.

Required DTO categories:

* Request DTOs
* Response DTOs

---

## Dependency Rules

Allowed:

Controller → Service

Service → Repository

Service → Service

Repository → Database

Forbidden:

Controller → Repository

Repository → Service

Controller → Entity exposure

Cross-module shortcuts that bypass services

---

## Validation Checklist

Before finalizing implementation:

✓ Controllers contain no business logic.

✓ DTOs used at boundaries.

✓ Repositories contain persistence only.

✓ Services implement workflows.

✓ Module boundaries respected.

✓ No duplicate business logic introduced.

---

## Anti-Patterns

Avoid:

* Fat controllers
* Anemic services
* Entity exposure
* Circular dependencies
* God services

---

## Refactoring Policy

Improve:

* Readability
* Maintainability
* Separation of concerns

Do NOT change business behavior during refactoring.
