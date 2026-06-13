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

# Step 7.5: Evaluate Idempotency Requirements

For every endpoint or background job being planned, determine whether it can be safely called more than once with the same input and produce the same outcome without side effects.

This is not optional for write operations. Network retries, duplicate webhooks, and user double-taps are real production events.

---

## Idempotency Decision Tree

Ask: Can this operation be triggered more than once for the same logical request?

* User submits a form and hits back → YES
* Razorpay sends a webhook that may arrive twice → YES
* A scheduled job re-runs for the same date → YES
* A GET request → Not applicable (reads are inherently idempotent)

If YES — identify the idempotency mechanism before implementation.

---

## Idempotency Mechanisms

Choose the mechanism that fits the operation:

**1. Idempotency Key Header (`X-Idempotency-Key`)**

Use for: Customer-initiated write operations that must not duplicate on retry.

How it works: Client generates a UUID per logical request and sends it in `X-Idempotency-Key`. Server stores the key and result; subsequent requests with the same key return the cached result without reprocessing.

Required for: `POST /api/orders` (ORDER-002) — prevents duplicate order submission on network retry.

**2. UNIQUE Database Constraint**

Use for: Events that must only exist once per business entity, regardless of how many times the insert is attempted.

How it works: The database constraint rejects the duplicate insert. The application catches the constraint violation and returns the appropriate response (200 for idempotent success, not 500).

Required for:
* `PAYMENTS.razorpay_payment_id` UNIQUE — duplicate Razorpay webhooks (PAYMENT-002) fail the constraint; handler returns 200 without reprocessing
* `RATINGS.order_id` UNIQUE — prevents second review for same order (REVIEW-001); return 409 Conflict
* `REFRESH_TOKENS` — revoked token handled gracefully (AUTH-007); return 200 not 500

**3. Upsert (INSERT ... ON CONFLICT DO UPDATE)**

Use for: Background jobs or data generation processes that may re-run for the same period.

How it works: The write targets a UNIQUE constraint; on conflict it updates rather than duplicates.

Required for: `ANALYTICS_SNAPSHOTS` on `(vendor_id, snapshot_date)` UNIQUE — re-running the daily job updates the existing record (ANALYTICS-001).

---

## Idempotency Checklist

For each write endpoint or job in the plan:

✓ Identified whether the operation can be triggered more than once.

✓ Identified the appropriate idempotency mechanism (key header / UNIQUE constraint / upsert).

✓ Duplicate scenario tested — second call produces correct outcome, not a 500 error.

✓ UNIQUE constraint violations are caught and handled with the correct HTTP status (200 for idempotent success, 409 for business-rule conflicts).

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

# Step 8.5: Evaluate MVP Scope Boundaries

Before proceeding to testing, verify the feature does not cross a documented MVP boundary.

This step is mandatory. A feature plan that ignores scope boundaries is incomplete regardless of how well the other steps are executed.

---

## General Scope Check

For the ticket being planned, answer these questions:

1. Is every acceptance criterion in the FTL ticket definition?
   If yes — build exactly that. If no — stop and request clarification.

2. Does the implementation require a capability described as "Phase 2", "Post-MVP", or "Nice to Have" in the PRD §7 or FTL Post-MVP Tickets?
   If yes — do NOT implement it. Raise the scope conflict.

3. Does the FSD describe a screen or component that goes beyond what the backend ticket delivers?
   If yes — build only the backend ticket. Flag the frontend component as requiring its own ticket before it can be enabled.

---

## Analytics Scope Boundary (Known Conflict — Read Before Planning Any Analytics Work)

The analytics area has a documented scope conflict between the PRD, TAD, and FSD that was identified during pre-development review. The resolved boundary is:

**ANALYTICS-001 (SHOULD HAVE — MVP):**

Implement the daily `@Scheduled` snapshot job only.

Aggregates: `total_orders`, `total_revenue`, `average_order_value`, `top_item_id`, `peak_hour` per vendor per day.

Writes to `ANALYTICS_SNAPSHOTS` table. Upsert on `(vendor_id, snapshot_date)`.

Updates Redis key `analytics:{vendorId}` TTL 900s after each write.

**ANALYTICS-002 (SHOULD HAVE — MVP):**

Implement `GET /api/vendors/{id}/analytics?days=30` only.

Returns array of daily snapshot records. Serves from Redis cache; falls back to DB.

Vendor ownership enforced. Admin bypasses ownership check.

**NOT in ANALYTICS-001 or ANALYTICS-002 scope:**

* Revenue Trend chart component (FSD §13) — do not build the frontend chart; the API supports it but the chart is a separate frontend ticket
* Orders Trend chart component (FSD §13) — same rule
* Multi-period comparisons
* Real-time analytics
* Advanced dashboard (Phase 2)
* Revenue forecasting (Phase 2)

**Rule:** If a task references FSD §13 analytics chart components during ANALYTICS-001 or ANALYTICS-002 planning, flag it as out of scope for that ticket. The charts require their own frontend ticket created after ANALYTICS-002 ships.

---

## Other Scope Boundaries to Check

* Vendor bank settlement / payouts — Phase 2 (Razorpay Route). Do not include in any payment ticket.
* Real-time order tracking (WebSocket/SSE) — Phase 2. ORDER status is polled every 15 seconds in MVP.
* Push notifications — Phase 2.
* Delivery fleet / driver assignment — Phase 3.
* ONDC integration — Phase 4.

If the ticket being planned touches any of the above, stop and verify intent before proceeding.

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

## Scope Boundaries

List any MVP scope boundaries that apply to this ticket.

Confirm the following for each boundary item:

* Feature is within MVP scope: Yes / No / Partial
* If Partial — identify what is in scope and what is deferred
* Phase 2 items excluded from this implementation: (list them)

For analytics tickets, explicitly state which analytics tier is being implemented (snapshot job / summary API / frontend chart) and confirm the others are out of scope for this ticket.

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

✓ Idempotency requirements evaluated (Step 7.5 completed).

✓ Business rules documented.

✓ MVP scope boundaries evaluated (Step 8.5 completed).

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

* Planning analytics features beyond the ticket's tier — ANALYTICS-001 is the snapshot job only; ANALYTICS-002 is the summary API only; frontend trend charts are a separate ticket that cannot be enabled until ANALYTICS-002 is shipped.

* Treating FSD screen specifications as implementation requirements for the current backend ticket — the FSD describes the eventual frontend; it does not override the ticket's acceptance criteria.

* Silently resolving a document scope conflict by choosing one document over another — if PRD, TAD, FSD, and FTL disagree on scope, stop and escalate.

---

# Escalation Rules

Stop and request clarification if:

Acceptance criteria conflict.

Business rules are ambiguous.

Security implications are unclear.

Dependencies are missing.

A document scope conflict exists — for example, if the PRD marks a feature as "Should Have" but the FSD includes full UI specifications for it, or if the TAD includes a schema table for a feature the PRD defers to Phase 2. Do not resolve these conflicts independently. Raise them before planning.

The ticket requires implementing a feature that appears in the Post-MVP Tickets section of the FTL, even if another document implies it is in scope.

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