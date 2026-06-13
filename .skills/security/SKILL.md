# Security Skill

## Purpose

Protect the Street Vendor Platform from unauthorized access, abuse, and data exposure.

Security requirements are mandatory.

---

## When to Use

Activate whenever modifying:

* Authentication
* Authorization
* Payments
* File uploads
* Public endpoints
* User data access
* Session handling

---

## Authentication Standards

Access Token:

* JWT
* Expiry: 15 minutes

Refresh Token:

* Expiry: 30 days
* Stored as HTTP-only cookie
* SameSite=Strict
* Secure=true
* Path=/api/auth

---

## Refresh Token Rules

Requirements:

* Store BCrypt hash only.
* Never store raw tokens.
* Rotate refresh tokens.
* Revoke old refresh tokens.

---

## Password Standards

Requirements:

* BCrypt hashing
* Strength = 12

Never:

* Store plaintext passwords.
* Log passwords.

---

## Admin Account Provisioning

ADMIN accounts are never created through the public registration endpoint.

Rule: `POST /api/auth/register` must return `403 Forbidden` if `role = ADMIN` is supplied in the request body. This is a security requirement enforced in AuthService — not a business rule to be softened later.

The only creation mechanism for the first ADMIN account is the `DataInitializer` component, which runs on application startup:

1. On startup, `DataInitializer` queries the USERS table for any existing ADMIN-role record.
2. If none exists, it creates one using `ADMIN_EMAIL` and `ADMIN_PASSWORD` from environment variables.
3. The password is BCrypt-hashed at strength 12 before storage — the raw env var value is never written to the database.
4. If an ADMIN record already exists, `DataInitializer` exits without action — idempotent on every restart.

Subsequent ADMIN accounts are created by existing admins via the admin dashboard only. There is no other path.

Environment variables required before first deployment:

```
ADMIN_EMAIL     — email for the bootstrap admin account
ADMIN_PASSWORD  — plaintext password (BCrypt-hashed on write, never stored raw)
```

These must be rotated after initial login. Leaving the bootstrap credentials unchanged in production is a security violation.

Implementation checklist for any ticket touching authentication or user registration:

✓ `POST /api/auth/register` rejects ADMIN role with 403.

✓ `DataInitializer` idempotency verified — does not duplicate admin on restart.

✓ `ADMIN_PASSWORD` is BCrypt-hashed before writing to USERS table — env var is read-once at startup, never logged.

---

## Rate Limiting

Required Endpoints:

Login:

* 10 requests/IP/minute

Registration:

* 5 requests/IP/minute

Discovery:

* 60 requests/IP/minute

Requirements:

* Redis-backed — rate limiting counters are stored in Redis, not in-memory
* Retry-After header returned on 429 responses
* Key format: `ratelimit:{ip}:{endpoint}`

Infrastructure dependency: SECURITY-003 (rate limiting) requires Redis to be operational. Do not implement SECURITY-003 before verifying Redis infrastructure is in place (see DISCOVERY-004). If Redis is unavailable, rate limiting fails silently — which is unacceptable for a security control. The application must fail startup if Redis is unreachable (see Architecture skill — Redis Infrastructure Requirements).

---

## Account Lockout

Requirements:

* Lock after repeated failed logins.
* Return 403 Forbidden.
* Do not reveal failure counts.

---

## Error Handling

Never expose:

* Stack traces
* SQL exceptions
* Internal paths
* Secret values

Use generic messages.

---

## Audit Logging

Required Events:

* LOGIN_ATTEMPT
* LOGIN_FAILED
* LOGIN_SUCCESS
* LOGOUT
* VENDOR_APPROVED
* VENDOR_REJECTED
* PAYMENT_VERIFICATION_FAILED
* PERMISSION_VIOLATION
* ACCOUNT_LOCKED
* ACCOUNT_SUSPENDED
* ACCOUNT_REACTIVATED
* ORDER_CANCELLED — written by ORDER-005 (PATCH /api/orders/{id}/cancel) on every successful cancellation
* ORDER_STATUS_CHANGED — written by ORDER-003 (PUT /api/orders/{id}/status) on every valid vendor-driven transition (ACCEPTED, PREPARING, READY, COMPLETED, or vendor CANCELLED)

Do NOT log:

* Passwords
* Raw JWT tokens
* Payment card data
* Personal identity documents

---

## Payment Security

Golden Rule:

Never trust the frontend.

Requirements:

* Validate Razorpay HMAC.
* Verify payment identifiers.
* Verify amounts.
* Reject mismatches.

---

## File Upload Security

Requirements:

Use pre-signed URLs.

Allowed Types:

* PDF
* JPG
* JPEG
* PNG

Rejected:

* ZIP
* EXE
* JS
* HTML

File Size Limits:

PDF:

* 5 MB

Images:

* 2 MB

---

## Validation Checklist

✓ JWT rules respected.

✓ Refresh rotation implemented.

✓ BCrypt strength = 12.

✓ Rate limiting present and Redis-backed (not in-memory).

✓ Lockout enforced.

✓ Audit logs generated — all required events from the Audit Logging section are present, including ORDER_CANCELLED and ORDER_STATUS_CHANGED where applicable.

✓ Payment verification secured.

✓ File validation implemented.

✓ POST /api/auth/register rejects ADMIN role with 403.

✓ DataInitializer creates admin only if none exists (idempotent).

---

## Anti-Patterns

Avoid:

* Trusting client-side validation.

* Returning detailed authentication errors.

* Logging secrets.

* Skipping ownership validation.

* Disabling security checks for convenience.
