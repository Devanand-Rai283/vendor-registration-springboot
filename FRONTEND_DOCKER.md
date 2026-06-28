# Frontend Docker Guide — Street Vendor Platform

This document covers building and running the Next.js 15 frontend container in isolation.
Docker Compose configuration (linking backend + frontend) is covered in a separate guide.

---

## Base Images

| Stage   | Image                    | Purpose                           |
|---------|--------------------------|-----------------------------------|
| deps    | node:22-bookworm-slim    | Dependency installation           |
| builder | node:22-bookworm-slim    | Production build                  |
| runtime | node:22-bookworm-slim    | Minimal runtime (standalone only) |

---

## Environment Variables

### Build-Time Variables (required at docker build)

CRITICAL: NEXT_PUBLIC_* variables are compiled into the client-side JavaScript
bundle by Next.js at build time. They are NOT read at container start.

Changing your .env file after the image is built has no effect.
You must rebuild the image to update these values.

| Variable | Description | Example |
|---|---|---|
| NEXT_PUBLIC_API_URL | Backend API base URL | http://localhost:8080/api |
| NEXT_PUBLIC_RAZORPAY_KEY_ID | Razorpay publishable key | rzp_test_... |

### Runtime Variables

No runtime environment variables are required. The Next.js standalone server
reads only what was compiled into the bundle. No server-only secrets are used.

---

## Security Notes

The following server-only variables are NOT used in src/ and are
NOT present in the runtime container:

- JWT_SECRET
- DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
- REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
- R2_ACCESS_KEY, R2_SECRET_KEY, R2_ENDPOINT, R2_BUCKET_NAME
- RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET

---

## Build

Run from the repository root:

```bash
docker build \
  --build-arg NEXT_PUBLIC_API_URL=http://localhost:8080/api \
  --build-arg NEXT_PUBLIC_RAZORPAY_KEY_ID=rzp_test_YOUR_KEY \
  -t streetvendor/frontend:latest \
  ./frontend
```

Replace rzp_test_YOUR_KEY with your actual Razorpay test key ID.
For production, use your live key ID and a production backend URL.

---

## Run

```bash
docker run -d \
  --name streetvendor-frontend \
  -p 3000:3000 \
  streetvendor/frontend:latest
```

The application will be available at: http://localhost:3000

No -e flags are needed -- all configuration was baked in at build time.

---

## Verify

### Non-root user
```bash
docker exec streetvendor-frontend id
# Expected: uid=1001(nextjs) gid=1001(nodejs)
```

### Runtime artifact audit
```bash
docker exec streetvendor-frontend ls -la /app
# Expected contents: server.js, .next/, public/
# Must NOT contain: src/, full node_modules/, tsconfig.json, package-lock.json
```

### Secret leak audit
```bash
docker exec streetvendor-frontend env
# Only NEXT_PUBLIC_* and system vars should appear
# Must NOT show: JWT_SECRET, DB_*, R2_*, RAZORPAY_KEY_SECRET
```

### Health check
```bash
curl http://localhost:3000/
# Expected: 200 OK
```

### Graceful shutdown
```bash
docker stop streetvendor-frontend
# SIGTERM delivered directly to node process -- clean exit
```

---

## Runtime Container Contents

The runtime image contains ONLY:

  /app/server.js              Next.js standalone entry point
  /app/.next/standalone/      Server-side bundled node_modules
  /app/.next/static/          Client-side CSS, JS, images
  /app/public/                Static public files

It does NOT contain:
  src/               TypeScript source code
  node_modules/      Full dev dependencies
  tsconfig.json      TypeScript config
  package-lock.json  Lock file
  .env files         Environment files

---

## Rebuild Required When

You must rebuild the image when:
- NEXT_PUBLIC_API_URL changes (e.g., pointing to a different backend)
- NEXT_PUBLIC_RAZORPAY_KEY_ID changes (test to live keys)
- Any source code in src/ changes
- next.config.ts changes
- Dependencies (package.json) change

---

## Production Notes

- next.config.ts includes a dev-only root .env loader. It is safely
  guarded with fs.existsSync() and does nothing inside the container.
- NEXT_TELEMETRY_DISABLED=1 prevents Next.js from sending telemetry data.
- The container runs as nextjs (UID 1001) -- never as root.
- Health check uses curl (pre-installed via apt) -- consistent with the backend image.