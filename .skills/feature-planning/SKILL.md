# Feature Planning Skill

## Purpose

Transform business requirements and feature tickets into complete implementation plans before coding begins.

Prevent rushed implementations, missed requirements, and architectural inconsistencies.

---

## When to Use

Activate whenever:

* Starting a new ticket
* Designing a new feature
* Breaking down requirements
* Estimating implementation effort
* Reviewing implementation readiness

Planning MUST occur before code generation.

---

## Planning Workflow

Follow these steps sequentially.

---

# Step 1: Understand the Requirement

Identify:

* Feature objective
* Business value
* User roles involved
* Success criteria

Questions:

* What problem does this solve?
* Who benefits from this feature?
* What outcome is expected?

---

# Step 2: Identify the Ticket

Extract:

* Ticket ID
* Dependencies
* Acceptance criteria
* Scope boundaries

Requirements:

Do not proceed if dependencies are incomplete.

Do not expand scope beyond the ticket.

---

# Step 3: Identify Affected Modules

Determine which modules are impacted.

Possible modules:

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

List all affected modules explicitly.

---

# Step 4: Identify Database Changes

Determine whether persistence changes are required.

Examples:

* New tables
* New columns
* New relationships
* New indexes
* New constraints

If database changes are required:

Generate Flyway migration tasks.

---

# Step 5: Identify Required Entities

For each affected module:

Determine whether new entities are needed.

Document:

* Entity names
* Relationships
* Validation rules
* Ownership implications

---

# Step 6: Identify Required APIs

Determine required endpoints.

For each endpoint define:

HTTP Method

Endpoint URI

Authentication requirements

Authorization requirements

Ownership requirements

Request DTO

Response DTO

Error scenarios

---

# Step 7: Evaluate Security Requirements

Review:

Authentication requirements

Authorization requirements

Ownership requirements

Rate limiting requirements

Audit logging requirements

Data exposure risks

Security planning is mandatory.

---

# Step 8: Evaluate Business Rules

Identify:

State transitions

Validation requirements

Approval requirements

Workflow restrictions

Cross-module dependencies

Business rules must be explicitly documented.

---

# Step 9: Plan Testing Strategy

Generate required tests.

Required categories:

Unit Tests

Integration Tests

Security Tests

Ownership Tests

Edge Case Tests

Regression Tests

Feature work is incomplete without a testing plan.

---

# Step 10: Review Architecture Impact

Validate:

Controller → Service → Repository structure.

Module boundaries.

DTO requirements.

Redis implications.

External integrations.

Maintainability considerations.

Architecture compliance is required.

---

# Planning Deliverable Format

For every feature, generate:

## Feature Summary

Purpose:

Business value:

Affected users:

---

## Ticket Information

Ticket ID:

Dependencies:

Acceptance criteria summary:

---

## Modules Impacted

List all modules.

---

## Database Changes

Tables:

Columns:

Indexes:

Constraints:

Flyway migrations required:

---

## APIs

Endpoint definitions.

Authentication requirements.

Ownership requirements.

Request/response structures.

---

## Security Considerations

Authentication:

Authorization:

Ownership:

Audit logging:

Rate limiting:

---

## Business Rules

Validation requirements.

Workflow rules.

State transitions.

Restrictions.

---

## Testing Strategy

Unit Tests:

Integration Tests:

Security Tests:

Ownership Tests:

Regression Tests:

---

## Implementation Tasks

Task 1

Task 2

Task 3

...

Tasks should be small, independently verifiable units of work.

---

# Completion Criteria

Feature planning is complete only when:

✓ Scope defined.

✓ Dependencies verified.

✓ Modules identified.

✓ Database impact assessed.

✓ APIs defined.

✓ Security evaluated.

✓ Business rules documented.

✓ Testing strategy created.

✓ Implementation tasks generated.

---

# Anti-Patterns

Avoid:

* Coding before planning.

* Assuming business rules.

* Ignoring dependencies.

* Expanding ticket scope.

* Skipping security review.

* Planning without testing considerations.

* Designing APIs without ownership checks.

---

# Escalation Rules

Stop and request clarification if:

Acceptance criteria conflict.

Business rules are ambiguous.

Security implications are unclear.

Dependencies are missing.

Do not guess.

Raise the issue for resolution.

---

# Example Usage

Input:

"Implement ORDER-005."

Output:

Feature plan including:

* Ticket analysis
* Dependency verification
* Required endpoint
* Ownership checks
* Business rule validation
* Audit logging requirements
* Testing strategy
* Implementation checklist

Only after planning should implementation begin.
