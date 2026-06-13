# Testing Skill

## Purpose

Ensure all features are verified through automated testing.

Features are incomplete without tests.

---

## When to Use

Activate whenever:

* Implementing a feature
* Fixing a bug
* Refactoring code
* Modifying security logic
* Updating business rules

---

## Testing Philosophy

Test behavior.

Do not test implementation details.

Focus on:

* Correctness
* Stability
* Regression prevention

---

## Required Test Types

### Unit Tests

Verify:

* Service logic
* Validation logic
* State transitions

Use:

* JUnit 5
* Mockito

---

### Integration Tests

Verify:

* Repository behavior
* Database interactions
* Transactional workflows

Use:

* Testcontainers
* PostgreSQL containers

---

### Redis Integration Tests

Redis is required infrastructure. Three security-critical features depend on it directly: rate limiting (SECURITY-003), account lockout (SECURITY-004), and refresh token revocation (AUTH-007). These must be tested against real Redis behavior — mocking Redis for these features is not acceptable.

**Required dependency:**

```xml
<dependency>
  <groupId>com.redis</groupId>
  <artifactId>testcontainers-redis</artifactId>
  <scope>test</scope>
</dependency>
```

Or use the Testcontainers generic container with the official Redis image:

```java
@Container
static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
    .withExposedPorts(6379);
```

**Test profile Redis configuration (`application-test.yml`):**

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

Override host and port in the test class using `@DynamicPropertySource`:

```java
@DynamicPropertySource
static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
}
```

**Required Redis test scenarios by feature:**

Rate Limiting (SECURITY-003):
* First N requests within the window succeed (200)
* Request N+1 returns 429 with `Retry-After` header
* Counter resets after the TTL window expires
* Different IPs have independent counters

Account Lockout (SECURITY-004):
* Failed logins 1 through (ACCOUNT_LOCK_THRESHOLD - 1) return 401
* Failed login at threshold sets `lockout:{email}` key in Redis and returns 403
* Successful login clears the `lockout:{email}` key
* Locked account returns 403 regardless of correct password
* Lock expires after ACCOUNT_LOCK_DURATION_MINUTES

Refresh Token Revocation (AUTH-007):
* Revoked refresh token returns 401 on POST /api/auth/refresh
* Already-revoked token does not cause 500 (idempotent)
* New token issued after rotation is accepted
* Old token after rotation is rejected

**Do not mock Redis for the above scenarios.** Using a mock bypasses the actual INCR, TTL, and SET semantics that the security behavior depends on. A test that passes with a mock may fail in production with real Redis.

---

### Security Tests

Verify:

* Authentication requirements
* Authorization restrictions
* Ownership enforcement
* Access denial scenarios

---

## Minimum Test Coverage

Generate tests for:

✓ Happy paths.

✓ Validation failures.

✓ Edge cases.

✓ Unauthorized access attempts.

✓ Business rule violations.

---

## Order Workflow Testing

Validate:

PLACED

↓

ACCEPTED

↓

PREPARING

↓

READY

↓

COMPLETED

Reject invalid transitions.

---

## Ownership Testing

Verify:

Customers cannot access others' orders.

Vendors cannot modify others' menus.

Admins enforce platform operations correctly.

---

## Security Testing

Verify:

* 401 responses.
* 403 responses.
* Rate-limiting behavior.
* Token validation behavior.

---

## Regression Policy

Whenever a bug is fixed:

Add a test reproducing the bug.

Ensure the bug cannot reappear unnoticed.

---

## Validation Checklist

✓ Unit tests written.

✓ Integration tests written.

✓ Redis integration tests written for any feature touching rate limiting, account lockout, or token revocation — using Testcontainers Redis, not mocks.

✓ Security scenarios tested.

✓ Ownership scenarios tested.

✓ Edge cases covered.

✓ Regression tests added.

---

## Anti-Patterns

Avoid:

* Testing private methods.

* Skipping negative cases.

* Mocking everything.

* Writing tests after merging.

* Relying solely on manual testing.
