🚀 Vendor Registration System

Spring Boot | PostgreSQL | Docker | Render | REST API

A production-ready backend application built using Spring Boot that provides RESTful APIs for vendor registration and management.
The project demonstrates end-to-end backend development, from local setup to cloud deployment with Docker and PostgreSQL.

📌 Key Highlights

Clean layered architecture (Controller → Service → Repository)

RESTful API design using Spring Boot

Database migration: Local DB → PostgreSQL (Cloud)

Secure configuration using environment variables

Fully Dockerized application

Deployed on Render Cloud

Industry-standard practices followed

🧠 Problem Statement

To design and deploy a backend system that allows vendors to:

Register themselves

View vendor records

Update vendor details

Delete vendor records

The system must be scalable, secure, and cloud-deployable.

🛠️ Tech Stack
Backend

Java 17

Spring Boot

Spring Data JPA

Hibernate ORM

Database

MySQL / H2 (local development – initial phase)

PostgreSQL (production database on Render)

DevOps & Tools

IntelliJ IDEA – Development IDE

Maven – Build & dependency management

Docker – Containerization

Render – Cloud hosting

Git & GitHub – Version control

Postman – API testing

🧱 Project Architecture
Controller  →  Service  →  Repository  →  Database

Layer Responsibilities

Controller – Handles HTTP requests & responses

Service – Business logic & validation

Repository – Database access via JPA

Entity – Database table mapping

Exception Layer – Centralized error handling

⚙️ Redis Discovery Caching & Infrastructure

To maximize search performance, improve scalability, and reduce database query overhead, the application integrates a robust, production-ready Redis-based caching layer for vendor discovery endpoints.

---

### 1. Cache Specifications

#### Vendor Search Cache
* **Key Format**: `search:vendors:{lat}:{lng}:{radius}`
* **Default TTL**: `600` seconds (10 minutes)
* **Configuration Property**: `discovery.cache.vendor-search-ttl` (Overrides via env: `DISCOVERY_CACHE_VENDOR_SEARCH_TTL`)
* **Cache Population**: Automatically populated on a cache miss during a `GET /api/vendors/nearby` or keyword search request.
* **Cache Hit Behavior**: Subsequent matching requests retrieve the cached paginated payload directly from Redis, avoiding PostgreSQL queries entirely.
* **Cache Invalidation**: Evicted (all keys matching `search:vendors:*`) immediately when a vendor is approved or rejected via `VendorService`.

#### Vendor Menu Cache
* **Key Format**: `vendor:menu:{vendorId}`
* **Default TTL**: `900` seconds (15 minutes)
* **Configuration Property**: `discovery.cache.vendor-menu-ttl` (Overrides via env: `DISCOVERY_CACHE_VENDOR_MENU_TTL`)
* **Cache Population**: Automatically populated on a cache miss during a `GET /api/vendors/{vendorId}/menu` request.
* **Cache Hit Behavior**: Subsequent requests for the vendor's menu retrieve the cached DTO structure from Redis.
* **Cache Invalidation**: Evicted immediately when a menu item belonging to the vendor is added, updated, deleted, or has its availability toggled via `MenuItemService`.

---

### 2. Redis Infrastructure & Dependencies

* **Core Cache Dependency**: `org.springframework.boot:spring-boot-starter-data-redis` (runs Lettuce connection client).
* **Testing Container Dependency**: `com.redis:testcontainers-redis` (spins up a generic Docker container running `redis:7-alpine` for integration tests).
* **Connection Factory Configuration**: Defined in `RedisConfig.java` using `LettuceConnectionFactory` with standalone settings.
* **Serializer Configuration**:
  * Keys & Hash Keys: Serialized as UTF-8 Strings using `StringRedisSerializer`.
  * Values & Hash Values: Serialized as JSON DTOs using `GenericJackson2JsonRedisSerializer` backed by an `ObjectMapper` customized with `JavaTimeModule` (disabling date-to-timestamp serialization) and polymorphic default typing (`ObjectMapper.DefaultTyping.EVERYTHING`).

---

### 3. Operational & Security Controls

* **Startup Fail-Fast (Safety Control)**: Connectivity to Redis is validated via a ping at startup. If unreachable, the application fails startup immediately with an `IllegalStateException` (prevents running without security limits/rate-limiting/lockouts). Enabled via `spring.data.redis.ping-on-startup=true` in `application-local.yml` and `application-prod.yml`.
* **Actuator Health Checks**: Included in Spring Actuator's health check indicators. Query `GET /actuator/health` to monitor current Redis health status.
* **Environment Configuration & Overrides**:
  * `REDIS_HOST` (Default: `localhost`)
  * `REDIS_PORT` (Default: `6379`)
  * `REDIS_PASSWORD` (Default: empty / credentials supported natively)
  * `DISCOVERY_CACHE_VENDOR_SEARCH_TTL` (Default: `600`)
  * `DISCOVERY_CACHE_VENDOR_MENU_TTL` (Default: `900`)
* **Security & Sensitive Data**:
  * Redis authentication credentials can be set via `REDIS_PASSWORD`.
  * **No Sensitive Data Caching**: Only public vendor metadata and menus are cached. PII, passwords, refresh tokens, and payment identifiers are never cached in this layer.

---

### 4. Development & Testing Prerequisites

To run the application locally or run the integration test suite:
* **Docker Desktop**: The Docker daemon must be running to host local services and allow Testcontainers to spin up temporary containers.
* **WSL2**: Essential on Windows environments for Docker daemon support.
* **Testcontainers Integration**: JUnit 5 integration tests (`DiscoveryServiceCacheIntegrationTest`, `RedisActuatorHealthIntegrationTest`, etc.) dynamically bootstrap PostgreSQL and Redis instances for end-to-end flow validation.


