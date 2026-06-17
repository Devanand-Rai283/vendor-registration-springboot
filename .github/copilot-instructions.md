# GitHub Copilot Operating Instructions

You are operating within the Street Vendor Platform AI Engineering System.

AGENTS.md is the authoritative source of truth.

You MUST follow AGENTS.md without exception.

Copilot-specific instructions supplement AGENTS.md and never override it.

---

# 1. DISCOVERY RESPONSIBILITIES

Before performing ANY work:

1. Open and read AGENTS.md completely.
2. Execute the Discovery Phase defined in AGENTS.md.
3. Identify the user's intent.
4. Determine which skills are applicable.
5. Load and follow the corresponding skill instructions from `.skills`.
6. Explicitly state all activated skills before implementation begins.
7. Request clarification if requirements or acceptance criteria are ambiguous.

Do NOT begin implementation until Discovery has been completed.

Failure to complete the Discovery Phase invalidates the response.

---

# Copilot-Specific Behaviors

Copilot MUST:

* Favor minimal and focused changes.
* Respect existing project conventions.
* Follow repository patterns before introducing new ones.
* Limit implementation strictly to the requested scope.
* Generate production-ready code only.
* Include tests whenever code changes occur.
* Prefer modifying existing files over creating unnecessary new files.
* Preserve backward compatibility unless explicitly instructed otherwise.
* Highlight assumptions when certainty is low.

Copilot MUST prioritize correctness over completion speed.

Copilot MUST NOT:

* Skip AGENTS.md procedures.
* Generate speculative implementations.
* Expand the scope of the request.
* Ignore acceptance criteria.
* Introduce unrelated refactoring.
* Claim work is complete without verification.

---

# Required Workflow

Copilot MUST follow this sequence:

Discovery
↓
Activate Skills
↓
Analyze Requirements
↓
Validate Acceptance Criteria
↓
Plan Minimal Changes
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

Copilot MUST enforce all requirements defined in AGENTS.md.

Before suggesting code, verify:

□ Acceptance criteria understood.

□ Applicable skills identified.

□ Ownership requirements satisfied.

□ Security implications reviewed.

□ Spring standards applicable.

□ Scope boundaries respected.

□ Existing project conventions followed.

If any item cannot be verified:

STOP.

Request clarification before proceeding.

Convenience MUST NOT override engineering standards.

---

# Required Response Structure

For implementation-related requests, responses MUST contain:

## Activated Skills

List all activated skills.

---

## Scope Analysis

Describe the requested work and identify scope boundaries.

---

## Implementation Plan

Describe the intended approach before generating code.

---

## Implementation

Provide the requested implementation.

---

## Testing Strategy

Describe tests added or updated.

---

## Verification Results

Document the outcome of all required verification checks.

Responses missing any section are incomplete.

---

# 3. VERIFICATION RESPONSIBILITIES

Before declaring work complete, Copilot MUST verify:

## Discovery Verification

□ AGENTS.md consulted.

□ Applicable skills identified.

□ Required skills activated.

---

## Requirement Verification

□ Acceptance criteria satisfied.

□ Requested scope respected.

□ No unrelated functionality introduced.

---

## Security Verification

□ Security implications reviewed.

□ Authorization requirements implemented where applicable.

□ Ownership validation enforced where applicable.

---

## Standards Verification

□ Spring standards followed.

□ Repository conventions respected.

□ Existing architectural patterns preserved.

---

## Database Verification

□ Flyway migration created when required.

□ Existing migrations remain unchanged.

---

## Testing Verification

□ Tests added or updated.

□ Existing tests remain valid.

□ Critical paths covered.

---

## Review Verification

□ Generated code reviewed against activated skills.

□ Risks and assumptions documented.

---

If ANY verification item fails:

STOP.

Do NOT declare the work complete.

Clearly communicate the unresolved issues.

---

# Completion Criteria

Copilot may only declare work complete when:

✓ Discovery Phase completed.

✓ AGENTS.md requirements satisfied.

✓ Copilot-specific responsibilities fulfilled.

✓ Verification Phase passed.

✓ Acceptance criteria satisfied.

✓ Scope boundaries respected.

✓ Required tests completed.

✓ No blocking issues remain.

Only then may the implementation be considered complete.
