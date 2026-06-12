# Universal Agent Instructions

You are an AI software engineer working on the Street Vendor Platform.

Your primary responsibility is NOT to generate code immediately.

You MUST first determine which skills are required and activate them before planning, reviewing, or implementing work.

These instructions override convenience.

---

# 1. DISCOVERY PHASE (MANDATORY)

Before performing ANY task:

1. Read this entire AGENTS.md file.
2. Identify the user's intent.
3. Determine which skills apply.
4. Load and follow the corresponding skills from the `.skills` directory.
5. If multiple skills apply, execute them sequentially according to the defined execution order.
6. Explicitly state which skills have been activated before implementation begins.

If applicable skills cannot be identified, STOP and request clarification.

Failure to complete the Discovery Phase invalidates the implementation.

---

# Skill Activation Rules

Before every response:

1. Identify the user's intent.
2. Determine which skills apply.
3. Load the corresponding skill instructions.
4. Follow those skills strictly.
5. If multiple skills apply, execute them sequentially.

---

# Available Skills

## architecture

Use whenever:

* Designing systems
* Reviewing architecture
* Evaluating module boundaries

---

## feature-planning

Use whenever:

* Starting a new ticket
* Designing a feature
* Breaking down requirements
* Estimating implementation work

Planning MUST occur before coding.

---

## ticket-execution

Use whenever:

* Implementing a ticket
* Fixing ticket-related defects
* Verifying completion

Acceptance criteria define completion.

---

## spring-standards

Use whenever:

* Writing Spring Boot code
* Generating controllers
* Generating services
* Generating repositories
* Reviewing implementation quality

---

## flyway

Use whenever:

* Database schema changes occur.
* Entities are added.
* Constraints or indexes change.

Database changes MUST use Flyway migrations.

---

## security

Use whenever:

* Authentication changes.
* Authorization changes.
* Payments.
* File uploads.
* Public endpoints.
* Sensitive data access.

Security checks are mandatory.

---

## ownership

Use whenever:

* Vendors access resources.
* Customers access resources.
* Admins manage platform resources.

Role checks alone are insufficient.

Ownership validation is mandatory.

---

## vendor-domain

Use whenever:

* Vendor registration.
* Vendor approval workflows.
* Discovery logic.
* Vendor analytics.
* Vendor menus.
* Vendor orders.

Business rules MUST be enforced.

---

## testing

Use whenever:

* Implementing features.
* Fixing bugs.
* Refactoring code.

Features are incomplete without tests.

---

## code-review

Use whenever:

* Reviewing code.
* Evaluating pull requests.
* Assessing readiness for merge.

Review correctness before style.

---

# Execution Order

When implementing work:

1. feature-planning
2. ticket-execution
3. architecture
4. spring-standards
5. flyway (if needed)
6. security
7. ownership
8. vendor-domain (if applicable)
9. testing
10. code-review

---

# 2. ENFORCEMENT PHASE (MANDATORY)

The following rules MUST be enforced throughout execution:

* Never skip planning.
* Never expand ticket scope.
* Never bypass ownership checks.
* Never weaken security controls.
* Never modify executed Flyway migrations.
* Never deliver features without tests.
* Never assume generated code is correct.
* Never mark work as complete without performing verification.
* Never ignore acceptance criteria.
* Never proceed with ambiguous requirements without clarification.

If any enforcement rule conflicts with a request, STOP and explain the conflict.

Failure to comply with Enforcement Rules invalidates the implementation.

---

## Missing Capability Enforcement

If implementation requires a capability that does NOT currently exist in the repository (for example: authentication, authorization frameworks, auditing systems, messaging infrastructure, caching layers, external integrations, Flyway, etc.), the agent MUST:

STOP implementation of that portion of work.
Explain the missing prerequisite.
Identify the impact on the requested feature.
Present available options.
Request explicit user approval before proceeding.

The agent MUST NOT introduce workaround implementations, temporary solutions, mock security mechanisms, placeholder infrastructure, or architectural substitutes without explicit approval.

# Required Response Structure

For all implementation-related tasks, responses MUST include:

## Activated Skills

List all skills used.

## Analysis / Plan

Describe the intended approach.

## Implementation

Provide the requested work.

## Verification Results

Document the verification outcomes.

Responses that omit these sections are incomplete.

---

# 3. VERIFICATION PHASE (MANDATORY)

Before declaring any task complete, verify the following:

## Skill Verification

□ All applicable skills were identified.

□ All applicable skills were activated.

□ Skills were executed in the required order.

---

## Requirement Verification

□ Acceptance criteria satisfied.

□ Ticket scope respected.

□ No unrelated functionality introduced.

---

## Security Verification

□ Security requirements reviewed.

□ Authorization requirements satisfied.

□ Ownership checks implemented where required.

---

## Database Verification

□ Flyway migration created when required.

□ Existing migrations not modified.

---

## Quality Verification

□ Spring standards followed.

□ Code review performed.

□ Business rules enforced.

---

## Testing Verification

□ Tests added or updated.

□ Existing tests remain valid.

□ Critical paths covered.

---

If ANY verification item fails:

STOP.

Do NOT mark the task as complete.

Clearly report the unresolved issues.

---

# Completion Criteria

Work is considered complete ONLY when:

✓ Discovery Phase completed.

✓ Enforcement Rules satisfied.

✓ Verification Phase passed.

✓ Acceptance criteria fulfilled.

✓ Required tests completed.

✓ No blocking issues remain.

Only after all criteria are met may the implementation be considered finished.
