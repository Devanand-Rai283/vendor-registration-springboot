# Architecture Skill

## Purpose

Enforce the Street Vendor Platform modular monolith architecture and prevent architectural drift.

---

## When to Use

Activate this skill whenever:

* Creating a new backend feature
* Modifying existing backend modules
* Refactoring code
* Designing new APIs
* Introducing new entities or services
* Reviewing pull requests

---

## Architecture Style

The system follows a **Modular Monolith** architecture.

Modules:

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

---

## Layer Responsibilities

Controller
↓
Service
↓
Repository

### Controllers

Responsibilities:

* Receive requests
* Validate DTOs
* Delegate to services
* Return HTTP responses

Must NOT:

* Contain business logic
* Access repositories directly
* Implement workflows

---

### Services

Responsibilities:

* Implement use cases
* Coordinate domain operations
* Enforce business rules
* Manage transactions

Must NOT:

* Handle HTTP concerns
* Return ResponseEntity objects

---

### Repositories

Responsibilities:

* Persistence operations
* Database querying

Must NOT:

* Implement business rules
* Call other services

---

## DTO Rules

Use DTOs for all API boundaries.

Never expose JPA entities directly.

Required DTO categories:

* Request DTOs
* Response DTOs

---

## Dependency Rules

Allowed:

Controller → Service

Service → Repository

Service → Service

Repository → Database

Forbidden:

Controller → Repository

Repository → Service

Controller → Entity exposure

Cross-module shortcuts that bypass services

---

## Redis Infrastructure Requirements

Redis is required infrastructure, not an optional dependency.

The following features cannot function without Redis and have no acceptable fallback:

* Rate limiting (SECURITY-003) — counters stored in `ratelimit:{ip}:{endpoint}` keys
* Account lockout (SECURITY-004) — counters stored in `lockout:{email}` keys
* Refresh token revocation (AUTH-007) — revoked token blacklist
* Vendor menu cache — `vendor:menu:{vendorId}` TTL 15 min
* Vendor search cache — `search:vendors:{lat}:{lng}:{radius}` TTL 10 min
* Analytics cache — `analytics:{vendorId}` TTL 15 min
* Session invalidation on suspension — `suspended_users` Redis Set TTL 15 min

### Startup Enforcement Rule

The application MUST fail startup if Redis is unreachable.

Graceful degradation is not acceptable for security features. An application that starts without Redis will silently skip rate limiting and account lockout, making brute-force attacks trivially possible.

Implement in `RedisConfig`:

```java
@Bean
public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
    LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
    factory.afterPropertiesSet();
    // Validate connection on startup — throws if Redis unreachable
    try {
        factory.getConnection().ping();
    } catch (Exception e) {
        throw new IllegalStateException(
            "Redis is required infrastructure and is unreachable at startup. " +
            "Check REDIS_HOST and REDIS_PORT environment variables.", e);
    }
    return factory;
}
```

### Actuator Health Check

Include Redis in the Spring Boot Actuator health endpoint.

`GET /actuator/health` must report Redis status.

The readiness endpoint must return `503 Service Unavailable` if Redis is unreachable — this prevents the load balancer from routing traffic to an instance where security features are non-functional.

`application.yml` configuration:

```yaml
management:
  health:
    redis:
      enabled: true
  endpoint:
    health:
      show-details: always
```

### Infrastructure Sequencing

Redis must be provisioned and reachable before the application starts. In CI/CD pipelines, Redis must be part of the environment before the Docker container launches — not an optional sidecar.

DISCOVERY-004 (Redis Discovery Cache) and SECURITY-003 (Rate Limiting) both depend on this infrastructure being in place. Do not implement either ticket without Redis operational in the target environment.

---

## Validation Checklist

Before finalizing implementation:

✓ Controllers contain no business logic.

✓ DTOs used at boundaries.

✓ Repositories contain persistence only.

✓ Services implement workflows.

✓ Module boundaries respected.

✓ No duplicate business logic introduced.

✓ Redis connectivity validated at startup — application fails hard if Redis is unreachable.

✓ Redis included in Actuator health check — readiness returns 503 if Redis is down.

---

## Anti-Patterns

Avoid:

* Fat controllers
* Anemic services
* Entity exposure
* Circular dependencies
* God services

---

## Refactoring Policy

Improve:

* Readability
* Maintainability
* Separation of concerns

Do NOT change business behavior during refactoring.
