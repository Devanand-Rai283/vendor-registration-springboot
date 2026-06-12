# Code Review Skill

## Purpose

Evaluate code quality before merging changes.

Prevent defects, security issues, and architectural decay.

---

## When to Use

Activate whenever:

* Reviewing pull requests
* Completing feature work
* Performing self-review
* Assessing generated code

---

## Review Order

Evaluate in this sequence:

1. Correctness
2. Security
3. Ownership
4. Architecture
5. Testing
6. Maintainability
7. Performance

---

## Correctness Checks

Verify:

* Acceptance criteria satisfied.
* Dependencies respected.
* Business rules implemented accurately.
* Edge cases handled.

---

## Security Checks

Verify:

* Authentication enforced.
* Authorization enforced.
* Ownership validated.
* Sensitive information protected.
* Inputs validated.

---

## Ownership Checks

Verify:

Customers access only their resources.

Vendors access only their resources.

Admins operate within defined responsibilities.

---

## Architecture Checks

Verify:

Controller → Service → Repository respected.

DTO boundaries preserved.

Business logic isolated within services.

Module boundaries maintained.

---

## Testing Checks

Verify:

Required tests exist.

Negative cases included.

Regression tests added.

Security scenarios covered.

---

## Maintainability Checks

Verify:

Meaningful naming.

Readable methods.

Minimal duplication.

Clear separation of concerns.

---

## Performance Checks

Verify:

Pagination used appropriately.

Indexes considered.

Unnecessary database queries avoided.

Redis usage aligned with requirements.

---

## Review Classification

### Critical Issues

Examples:

* Security vulnerabilities
* Ownership violations
* Data corruption risks

Must be fixed before merge.

---

### Major Issues

Examples:

* Missing tests
* Architectural violations
* Incorrect business logic

Should be fixed before merge.

---

### Minor Issues

Examples:

* Naming improvements
* Small refactoring opportunities
* Documentation enhancements

May be deferred.

---

## Final Review Checklist

✓ Acceptance criteria met.

✓ Security requirements satisfied.

✓ Ownership enforced.

✓ Architecture respected.

✓ Tests included.

✓ Performance considerations reviewed.

✓ Maintainability acceptable.

---

## Anti-Patterns

Avoid:

* Reviewing style before correctness.

* Ignoring security concerns.

* Approving code without tests.

* Accepting ownership violations.

* Treating generated code as inherently correct.
