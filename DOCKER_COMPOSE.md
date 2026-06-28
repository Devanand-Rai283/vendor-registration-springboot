# Docker Compose Guide -- Street Vendor Platform

Start the complete platform stack with a single command.

---

## Prerequisites

- Docker Desktop (or Docker Engine + Docker Compose plugin) installed and running
- `.env` file present in the repository root (copy from `.env.example` and fill in real values)
- Images are built on first `docker compose up` -- allow 3-5 minutes

---

## Environment Files

| File | Purpose | Committed |
|---|---|---|
| `.env.example` | Template -- all keys, placeholder values | Yes |
| `.env` | Runtime configuration for Docker Compose | No |
| `.env.local` | Optional -- IDE / Spring Boot direct run (localhost) | No |

Docker Compose automatically loads `.env` from the project root.

For IDE development (IntelliJ, VS Code) running Spring Boot directly without Docker,
use `.env.local` which sets `DB_HOST=localhost` and `REDIS_HOST=localhost`.

---

## Services

| Service | Image | Port | Purpose |
|---|---|---|---|
| postgres | postgres:17 | 5432 | PostgreSQL database |
| redis | redis:8 | 6379 | Redis cache + session store |
| backend | streetvendor/backend | 8080 | Spring Boot API |
| frontend | streetvendor/frontend | 3000 | Next.js UI |

---

## Quick Start

```bash
# 1. Copy environment template
cp .env.example .env
# Edit .env with your real values

# 2. Start all services
docker compose up

# Or detached (background)
docker compose up -d
```

---

## Commands

### Start

```bash
# Start all services (foreground -- shows logs)
docker compose up

# Start all services in the background
docker compose up -d

# Start and rebuild images
docker compose up -d --build
```

### Stop

```bash
# Stop containers (keep volumes and networks)
docker compose stop

# Stop and remove containers, networks (keeps volumes)
docker compose down

# Stop and remove everything including volumes (DELETES DATA)
docker compose down --volumes
```

### Status

```bash
docker compose ps
```

### Logs

```bash
# All services
docker compose logs

# Follow logs live
docker compose logs -f

# Single service
docker compose logs backend
docker compose logs -f frontend

# Last 100 lines
docker compose logs --tail=100 backend
```

### Restart

```bash
# Restart all services
docker compose restart

# Restart a single service
docker compose restart backend
```

### Configuration

```bash
# Verify compose.yaml + .env interpolation (no side effects)
docker compose config
```

### Execute commands inside containers

```bash
# Backend shell
docker compose exec backend sh

# Postgres shell
docker compose exec postgres psql -U postgres -d street_vendor

# Redis CLI
docker compose exec redis redis-cli -a $REDIS_PASSWORD --no-auth-warning
```

---

## Startup Order

```
postgres (healthcheck: pg_isready)
redis    (healthcheck: redis-cli ping)
    |
    +---> backend (waits until both are healthy, ~90s start period)
              |
              +---> frontend (starts once backend is running)
```

The first startup takes 2-5 minutes:
- PostgreSQL and Redis start in ~15 seconds
- Backend pulls Maven dependencies (if not cached) then runs Flyway migrations
- Frontend builds the Next.js bundle on first `up --build`

---

## Persistent Volumes

| Volume | Contents | Location inside container |
|---|---|---|
| `streetvendor_postgres_data` | Database files | `/var/lib/postgresql/data` |
| `streetvendor_redis_data` | Redis AOF + RDB files | `/data` |

Volumes survive `docker compose down` and `docker compose restart`.
They are destroyed only by `docker compose down --volumes`.

---

## Volume Cleanup

```bash
# Remove volumes (deletes all database data)
docker compose down --volumes

# Remove dangling volumes not used by any container
docker volume prune

# List all volumes for this project
docker volume ls | grep streetvendor
```

---

## Health Endpoints

| Endpoint | Purpose |
|---|---|
| `http://localhost:8080/actuator/health` | Full backend health |
| `http://localhost:8080/actuator/health/readiness` | Readiness (db + redis probes) |
| `http://localhost:3000` | Frontend home page |

---

## Rebuilding Images

When you change source code, rebuild images before starting:

```bash
docker compose up -d --build
```

### Frontend: NEXT_PUBLIC_* variables

NEXT_PUBLIC_API_URL and NEXT_PUBLIC_RAZORPAY_KEY_ID are compiled into the
client bundle at build time. Changing .env alone does NOT update the frontend.
You must rebuild the frontend image.

```bash
docker compose build frontend
docker compose up -d frontend
```

---

## Troubleshooting

### Backend fails to start

Check logs:
```bash
docker compose logs backend
```

Common causes:
- Missing or incorrect variable in `.env`
- PostgreSQL not yet healthy (wait longer)
- Flyway migration failure

### Flyway migration error

```bash
docker compose logs backend | grep -i flyway
```

If a migration failed and left the database in a bad state:
```bash
docker compose down --volumes
docker compose up -d
```

### Port already in use

If ports 5432, 6379, 8080, or 3000 are in use locally:
- Stop any locally running PostgreSQL, Redis, or backend processes
- Or change the host-side port in `compose.yaml` (e.g., `"5433:5432"`)

### Cannot connect to database from IDE

Make sure you are using `.env.local` (DB_HOST=localhost) when running Spring Boot
from your IDE. The `.env` file has DB_HOST=postgres which only works inside Docker.

### Redis authentication error

Verify `REDIS_PASSWORD` in `.env` matches what you expect.
The Redis container starts with `--requirepass` using this value.

---

## Security Notes

- No secrets are baked into Docker images
- Backend secrets (JWT_SECRET, R2_*, RAZORPAY_KEY_SECRET) are never passed to the frontend
- NEXT_PUBLIC_* variables (only API_URL and Razorpay publishable key) are the only values in the frontend image
- Containers run as non-root users where configured (backend: appuser/1001, frontend: nextjs/1001)
- PostgreSQL and Redis are accessible on host ports (5432, 6379) for developer tooling only