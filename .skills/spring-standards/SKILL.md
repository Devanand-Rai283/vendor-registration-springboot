# Spring Standards Skill

## Purpose

Ensure consistent implementation practices across the backend.

---

## When to Use

Activate whenever:

* Writing backend code
* Modifying Spring components
* Generating new modules
* Reviewing implementations

---

## Technology Standards

Java: 17

Framework: Spring Boot 4.0.2

Persistence: Spring Data JPA

Security: Spring Security

Migrations: Flyway

Database: PostgreSQL

Caching: Redis

---

## Dependency Injection

Use:

Constructor Injection.

Do NOT use:

Field Injection.

---

## Entity Standards

Requirements:

* UUID primary keys
* Audit timestamps
* Validation constraints
* Clear relationships

Avoid:

* Business logic inside entities
* Bidirectional relationships unless necessary

---

## Exception Handling

Use:

Global exception handling.

Requirements:

* Structured error responses
* Consistent status codes
* Sanitized messages

Never expose:

* Stack traces
* SQL errors
* Server paths
* Secret values

---

## Validation Standards

Use Bean Validation.

Examples:

* @NotBlank
* @Email
* @Positive
* @Size

Validate all incoming requests.

---

## API Standards

Use REST principles.

Requirements:

* Appropriate HTTP methods
* Meaningful status codes
* Pagination support where applicable
* Backward compatibility awareness

---

## Logging Standards

Log:

* Important business events
* Security events
* Operational failures

Do NOT log:

* Passwords
* JWT tokens
* Payment secrets
* Identity documents

---

## Validation Checklist

✓ Constructor injection used.

✓ Validation annotations present.

✓ Global exception handling respected.

✓ REST conventions followed.

✓ Sensitive data protected.

✓ UUIDs used consistently.
