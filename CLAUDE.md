# Claude Operating Instructions

You are operating within the Street Vendor Platform AI Engineering System.

AGENTS.md is the authoritative source of truth.

You MUST follow AGENTS.md without exception.

Claude-specific instructions supplement AGENTS.md and never override it.

---

# 1. DISCOVERY RESPONSIBILITIES

Before performing ANY work:

1. Read AGENTS.md completely.
2. Execute the Discovery Phase defined in AGENTS.md.
3. Identify the user's intent.
4. Determine which skills are applicable.
5. Load the corresponding skill instructions from `.skills`.
6. Explicitly state all activated skills before implementation begins.
7. If requirements are ambiguous, request clarification before proceeding.

Do NOT begin implementation until Discovery has been completed.

---

# Claude-Specific Behaviors

Claude MUST:

* Think step-by-step before implementation.
* Prefer correctness over speed.
* Produce implementation plans before code.
* Explicitly state activated skills.
* Ask clarifying questions when requirements are incomplete.
* Avoid assumptions regarding business rules.
* Highlight architectural implications when relevant.
* Consider edge cases during planning.
* Generate production-ready solutions.

Claude MUST NOT:

* Infer missing ticket requirements.
* Expand the scope of work.
* Skip planning in favor of immediate coding.
* Ignore AGENTS.md directives.
* Claim completion without verification.

---

# Required Workflow

Claude MUST follow this sequence:

Discovery
↓
Activate Skills
↓
Analyze Requirements
↓
Produce Implementation Plan
↓
Implement
↓
Generate or Update Tests
↓
Perform Verification
↓
Review Against Activated Skills
↓
Declare Completion

Skipping any phase invalidates the response.

---

# 2. ENFORCEMENT RESPONSIBILITIES

Claude MUST enforce all constraints defined in AGENTS.md.

In addition:

* Stop when security requirements conflict with implementation requests.
* Stop when ownership requirements cannot be verified.
* Stop when acceptance criteria are contradictory.
* Stop when implementation would violate existing architectural principles.
* Stop when database modifications violate Flyway rules.

When stopping, explain the issue clearly and request clarification or correction.

Convenience MUST NOT override engineering standards.

---

# Required Response Structure

For implementation-related requests, responses MUST contain:

## Activated Skills

List all activated skills.

---

## Requirement Analysis

Summarize the problem and constraints.

---

## Implementation Plan

Describe the intended approach before coding.

---

## Implementation

Provide the requested implementation.

---

## Testing Strategy

Describe the tests added or updated.

---

## Verification Results

Document the outcome of all required verification checks.

Responses missing any section are incomplete.

---

# 3. VERIFICATION RESPONSIBILITIES

Before declaring work complete, Claude MUST verify:

## Discovery Verification

□ AGENTS.md was consulted.

□ Applicable skills were identified.

□ Relevant skills were activated.

---

## Requirement Verification

□ Acceptance criteria satisfied.

□ Ticket scope respected.

□ No unsupported assumptions introduced.

---

## Security Verification

□ Security implications reviewed.

□ Authorization requirements implemented.

□ Ownership validation enforced where applicable.

---

## Database Verification

□ Flyway migrations created when required.

□ Existing migrations remain unchanged.

---

## Quality Verification

□ Spring standards followed.

□ Architectural consistency maintained.

□ Business rules enforced.

---

## Testing Verification

□ Tests created or updated.

□ Existing tests remain valid.

□ Critical paths covered.

---

## Review Verification

□ Implementation reviewed against activated skills.

□ Potential risks identified and documented.

---

If ANY verification item fails:

STOP.

Do NOT declare the work complete.

Clearly communicate the unresolved issues.

---

# Completion Criteria

Claude may only declare work complete when:

✓ Discovery Phase completed.

✓ AGENTS.md requirements satisfied.

✓ Claude-specific responsibilities fulfilled.

✓ Verification Phase passed.

✓ Acceptance criteria satisfied.

✓ Required tests completed.

✓ No blocking issues remain.

Only then may the implementation be considered complete.
