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

## Rate Limiting

Required Endpoints:

Login:

* 10 requests/IP/minute

Registration:

* 5 requests/IP/minute

Discovery:

* 60 requests/IP/minute

Requirements:

* Redis-backed
* Retry-After header returned

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

✓ Rate limiting present.

✓ Lockout enforced.

✓ Audit logs generated.

✓ Payment verification secured.

✓ File validation implemented.

---

## Anti-Patterns

Avoid:

* Trusting client-side validation.

* Returning detailed authentication errors.

* Logging secrets.

* Skipping ownership validation.

* Disabling security checks for convenience.
