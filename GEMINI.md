# Gemini Operating Instructions

You are operating within the Street Vendor Platform AI Engineering System.

AGENTS.md is the authoritative source of truth.

You MUST follow AGENTS.md without exception.

Gemini-specific instructions supplement AGENTS.md and never override it.

---

# 1. DISCOVERY RESPONSIBILITIES

Before performing ANY work:

1. Read AGENTS.md completely.
2. Execute the Discovery Phase defined in AGENTS.md.
3. Identify the user's intent.
4. Determine which skills are applicable.
5. Load and follow the corresponding skill instructions from `.skills`.
6. Explicitly list all activated skills before implementation begins.
7. Validate dependencies, assumptions, and constraints.
8. Request clarification if requirements are ambiguous or incomplete.

Do NOT begin implementation until Discovery has been completed.

Failure to complete the Discovery Phase invalidates the response.

---

# Gemini-Specific Behaviors

Gemini MUST:

* Use structured outputs.
* Organize responses into clearly defined sections.
* Explicitly list activated skills.
* Validate dependencies before implementation.
* Highlight architectural implications early.
* Summarize security considerations separately.
* Produce testing requirements before concluding work.
* Identify assumptions and constraints.
* Consider system-wide impacts of proposed changes.
* Prefer clarity and completeness over brevity.

Gemini MUST NOT:

* Skip AGENTS.md procedures.
* Infer missing business requirements.
* Ignore dependency implications.
* Expand ticket scope without explicit approval.
* Claim completion without verification.

---

# Required Workflow

Gemini MUST follow this sequence:

Discovery
↓
Activate Skills
↓
Analyze Requirements
↓
Validate Dependencies
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

Gemini MUST enforce all requirements defined in AGENTS.md.

In addition, Gemini MUST ensure:

□ Dependencies are compatible.

□ Architectural decisions remain consistent.

□ Security implications are identified.

□ Scope boundaries are respected.

□ Testing requirements are defined.

□ Business constraints are preserved.

If any enforcement condition cannot be satisfied:

STOP.

Clearly explain the issue and request clarification.

Convenience MUST NOT override engineering standards.

---

# Required Response Structure

For implementation-related requests, responses MUST contain:

## Activated Skills

List all activated skills.

---

## Requirement Analysis

Summarize the request, assumptions, constraints, and acceptance criteria.

---

## Dependency Review

Identify dependencies affected by the implementation.

---

## Architectural Considerations

Describe potential impacts on architecture and system boundaries.

---

## Security Review

Summarize security implications and required safeguards.

---

## Implementation Plan

Describe the intended approach before generating code.

---

## Implementation

Provide the requested implementation.

---

## Testing Strategy

Describe tests added, updated, or required.

---

## Verification Results

Document the outcome of all required verification checks.

Responses missing any section are incomplete.

---

# 3. VERIFICATION RESPONSIBILITIES

Before declaring work complete, Gemini MUST verify:

## Discovery Verification

□ AGENTS.md consulted.

□ Applicable skills identified.

□ Required skills activated.

---

## Requirement Verification

□ Acceptance criteria satisfied.

□ Scope boundaries respected.

□ Assumptions documented.

□ No unsupported functionality introduced.

---

## Dependency Verification

□ Dependencies validated.

□ Existing integrations remain unaffected.

□ Compatibility concerns addressed.

---

## Architecture Verification

□ Architectural consistency maintained.

□ Module boundaries respected.

□ System-wide implications considered.

---

## Security Verification

□ Security implications reviewed.

□ Authorization requirements implemented where applicable.

□ Ownership validation enforced where applicable.

---

## Database Verification

□ Flyway migrations created when required.

□ Existing migrations remain unchanged.

---

## Testing Verification

□ Tests added or updated.

□ Existing tests remain valid.

□ Critical paths covered.

□ Testing requirements satisfied.

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

Gemini may only declare work complete when:

✓ Discovery Phase completed.

✓ AGENTS.md requirements satisfied.

✓ Gemini-specific responsibilities fulfilled.

✓ Verification Phase passed.

✓ Acceptance criteria satisfied.

✓ Dependencies validated.

✓ Required tests completed.

✓ No blocking issues remain.

Only then may the implementation be considered complete.
