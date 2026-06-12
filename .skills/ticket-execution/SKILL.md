# Ticket Execution Skill

## Purpose

Ensure all development work follows the Feature Ticket List precisely.

Prevent scope creep and incomplete implementations.

---

## When to Use

Activate whenever:

* Implementing a new feature
* Fixing a bug tied to a ticket
* Reviewing completed work
* Planning development tasks

---

## Development Workflow

Step 1

Identify the Ticket ID.

Examples:

* AUTH-006
* VENDOR-003
* ORDER-005

---

Step 2

Review Dependencies.

Do NOT implement tickets whose dependencies are incomplete.

---

Step 3

Read Acceptance Criteria carefully.

Acceptance Criteria define completion.

Implementation is NOT complete until every criterion is satisfied.

---

Step 4

Implement ONLY the requested scope.

Avoid:

* Future tickets
* Extra features
* Assumptions

---

Step 5

Generate Tests.

Include:

* Happy paths
* Validation failures
* Edge cases
* Security checks

---

Step 6

Update Supporting Artifacts.

If required:

* Flyway migrations
* OpenAPI documentation
* DTOs
* Security rules

---

## Definition of Done

A ticket is complete only if:

✓ Dependencies satisfied.

✓ Acceptance criteria satisfied.

✓ Tests generated.

✓ Documentation updated.

✓ Security considerations reviewed.

✓ Code review completed.

---

## Anti-Patterns

Avoid:

* Implementing multiple tickets unintentionally.

* Skipping dependencies.

* Ignoring acceptance criteria.

* Delivering code without tests.

* Adding undocumented behavior.

---

## Escalation Rules

If acceptance criteria conflict with architecture or security requirements:

Stop implementation.

Raise the conflict for review.

Do not guess.
