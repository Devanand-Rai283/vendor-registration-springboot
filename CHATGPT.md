# ChatGPT Operating Instructions

You are operating within the Street Vendor Platform AI Engineering System.

AGENTS.md is the authoritative source of truth.

You MUST follow AGENTS.md without exception.

ChatGPT-specific instructions supplement AGENTS.md and never override it.

---

# 1. DISCOVERY RESPONSIBILITIES

Before performing ANY work:

1. Read AGENTS.md completely.
2. Execute the Discovery Phase defined in AGENTS.md.
3. Identify the user's intent.
4. Determine which skills are applicable.
5. Load the corresponding skill instructions from `.skills`.
6. Explicitly state all activated skills before implementation begins.
7. Review dependencies defined in the Feature Ticket List.
8. Review relevant planning documents (PRD, TAD, FSD, Security Document) when applicable.
9. Request clarification if requirements are ambiguous or contradictory.

Do NOT begin implementation until Discovery has been completed.

Failure to complete the Discovery Phase invalidates the response.

---

# ChatGPT-Specific Behaviors

ChatGPT MUST:

* Prioritize correctness over speed.
* Verify requirements against source documents before implementation.
* Identify architectural implications early.
* Detect cross-document inconsistencies.
* Preserve MVP scope boundaries.
* Produce implementation plans before generating code.
* Explicitly state activated skills.
* Generate production-ready implementations only.
* Consider business rules and edge cases during planning.
* Highlight risks, assumptions, and dependencies.
* Generate or update tests whenever code changes occur.
* Review generated code critically rather than assuming correctness.
* Validate that implementation satisfies ticket acceptance criteria before declaring completion.

ChatGPT MUST NOT:

* Skip AGENTS.md procedures.
* Infer undocumented business rules.
* Expand the scope of the requested ticket.
* Ignore dependency requirements.
* Introduce placeholder implementations for missing infrastructure.
* Claim completion without verification.
* Silently resolve conflicts between project documents.
* Modify historical Flyway migrations.

---

# Required Workflow

ChatGPT MUST follow this sequence:

Discovery
↓
Activate Skills
↓
Analyze Requirements
↓
Validate Dependencies
↓
Review Relevant Project Documents
↓
Identify Architectural Implications
↓
Review Security Considerations
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

ChatGPT MUST enforce all requirements defined in AGENTS.md.

Additionally, ChatGPT MUST verify:

□ Acceptance criteria are fully understood.

□ Dependencies are complete.

□ Applicable skills have been activated.

□ Security implications have been reviewed.

□ Ownership requirements have been enforced.

□ Flyway rules are respected.

□ Existing architectural patterns are preserved.

□ Scope boundaries are maintained.

□ Testing requirements have been satisfied.

If any enforcement condition cannot be satisfied:

STOP.

Clearly explain the issue and request clarification.

Convenience MUST NOT override engineering standards.

---

# Cross-Document Consistency Responsibilities

When implementation touches multiple planning artifacts, ChatGPT MUST verify consistency across:

* PRD
* TAD
* FSD
* Security and Access Document
* Feature Ticket List

If contradictions are discovered:

STOP.

Clearly describe:

* The conflicting requirements.
* The affected documents.
* The implementation impact.
* The available resolution paths.

Do NOT independently resolve the conflict.

---

# Required Response Structure

For implementation-related requests, responses MUST contain:

## Activated Skills

List all activated skills.

---

## Requirement Analysis

Summarize:

* The user's request.
* Acceptance criteria.
* Assumptions.
* Constraints.

---

## Dependency Review

Identify:

* Required dependencies.
* Dependency status.
* Blocking prerequisites.

---

## Architectural Considerations

Describe:

* Impacted modules.
* Layer implications.
* Redis implications where applicable.
* Flyway implications where applicable.

---

## Security Review

Summarize:

* Authentication requirements.
* Authorization requirements.
* Ownership requirements.
* Audit logging implications.

---

## Implementation Plan

Describe the intended approach before generating code.

---

## Implementation

Provide the requested implementation.

---

## Testing Strategy

Describe:

* Unit tests.
* Integration tests.
* Security tests.
* Ownership tests.
* Regression tests.
* Redis integration tests when applicable.

---

## Verification Results

Document the outcome of all required verification checks.

Responses missing any section are incomplete.

---

# 3. VERIFICATION RESPONSIBILITIES

Before declaring work complete, ChatGPT MUST verify:

## Discovery Verification

□ AGENTS.md consulted.

□ Applicable skills identified.

□ Required skills activated.

---

## Requirement Verification

□ Acceptance criteria satisfied.

□ Ticket scope respected.

□ No unsupported functionality introduced.

□ Assumptions documented.

---

## Dependency Verification

□ Dependencies validated.

□ Existing integrations remain unaffected.

□ Compatibility concerns addressed.

---

## Architecture Verification

□ Modular monolith structure maintained.

□ Controller → Service → Repository boundaries respected.

□ DTO boundaries preserved.

□ Redis requirements maintained.

---

## Security Verification

□ Security implications reviewed.

□ Authentication requirements implemented.

□ Authorization requirements implemented.

□ Ownership validation enforced where applicable.

□ Audit logging implemented where applicable.

---

## Database Verification

□ Flyway migration created when required.

□ Existing migrations remain unchanged.

□ UUID standards maintained.

□ Constraints preserved.

---

## Testing Verification

□ Tests added or updated.

□ Existing tests remain valid.

□ Critical paths covered.

□ Security scenarios covered.

□ Ownership scenarios covered.

□ Redis integration tests included where required.

---

## Review Verification

□ Implementation reviewed against activated skills.

□ Risks documented.

□ Outstanding concerns identified.

---

If ANY verification item fails:

STOP.

Do NOT declare the work complete.

Clearly communicate the unresolved issues.

---

# Completion Criteria

ChatGPT may only declare work complete when:

✓ Discovery Phase completed.

✓ AGENTS.md requirements satisfied.

✓ ChatGPT-specific responsibilities fulfilled.

✓ Verification Phase passed.

✓ Acceptance criteria satisfied.

✓ Dependencies validated.

✓ Required tests completed.

✓ Scope boundaries respected.

✓ No blocking issues remain.

Only then may the implementation be considered complete.