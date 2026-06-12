# Testing Skill

## Purpose

Ensure all features are verified through automated testing.

Features are incomplete without tests.

---

## When to Use

Activate whenever:

* Implementing a feature
* Fixing a bug
* Refactoring code
* Modifying security logic
* Updating business rules

---

## Testing Philosophy

Test behavior.

Do not test implementation details.

Focus on:

* Correctness
* Stability
* Regression prevention

---

## Required Test Types

### Unit Tests

Verify:

* Service logic
* Validation logic
* State transitions

Use:

* JUnit 5
* Mockito

---

### Integration Tests

Verify:

* Repository behavior
* Database interactions
* Transactional workflows

Use:

* Testcontainers
* PostgreSQL containers

---

### Security Tests

Verify:

* Authentication requirements
* Authorization restrictions
* Ownership enforcement
* Access denial scenarios

---

## Minimum Test Coverage

Generate tests for:

✓ Happy paths.

✓ Validation failures.

✓ Edge cases.

✓ Unauthorized access attempts.

✓ Business rule violations.

---

## Order Workflow Testing

Validate:

PLACED

↓

ACCEPTED

↓

PREPARING

↓

READY

↓

COMPLETED

Reject invalid transitions.

---

## Ownership Testing

Verify:

Customers cannot access others' orders.

Vendors cannot modify others' menus.

Admins enforce platform operations correctly.

---

## Security Testing

Verify:

* 401 responses.
* 403 responses.
* Rate-limiting behavior.
* Token validation behavior.

---

## Regression Policy

Whenever a bug is fixed:

Add a test reproducing the bug.

Ensure the bug cannot reappear unnoticed.

---

## Validation Checklist

✓ Unit tests written.

✓ Integration tests written.

✓ Security scenarios tested.

✓ Ownership scenarios tested.

✓ Edge cases covered.

✓ Regression tests added.

---

## Anti-Patterns

Avoid:

* Testing private methods.

* Skipping negative cases.

* Mocking everything.

* Writing tests after merging.

* Relying solely on manual testing.
