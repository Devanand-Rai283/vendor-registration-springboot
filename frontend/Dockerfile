# ============================================================
# Stage 1 -- deps
# Install ALL dependencies (dev + prod) needed for the build.
# ============================================================
FROM node:22-bookworm-slim AS deps

WORKDIR /app

# Copy lockfile and npmrc first for layer-cache efficiency.
# .npmrc carries legacy-peer-deps=true which npm ci must respect.
COPY .npmrc package.json package-lock.json ./

RUN npm ci


# ============================================================
# Stage 2 -- builder
# Build the production bundle with standalone output.
# NEXT_PUBLIC_* variables are baked into the client bundle here.
# ============================================================
FROM node:22-bookworm-slim AS builder

WORKDIR /app

# Reuse installed node_modules from deps stage
COPY --from=deps /app/node_modules ./node_modules

# Copy the full source
COPY . .

# -- NEXT_PUBLIC_* build arguments ---------------------------
# These values are compiled into the client-side JavaScript bundle.
# They CANNOT be changed at runtime -- changing .env after building
# has no effect. You must rebuild the image to update these values.
ARG NEXT_PUBLIC_API_URL=""
ARG NEXT_PUBLIC_RAZORPAY_KEY_ID=""

ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}
ENV NEXT_PUBLIC_RAZORPAY_KEY_ID=${NEXT_PUBLIC_RAZORPAY_KEY_ID}

# Disable Next.js telemetry in CI/CD environments
ENV NEXT_TELEMETRY_DISABLED=1

RUN npm run build


# ============================================================
# Stage 3 -- runtime
# Copy only the standalone server artefacts -- no source,
# no dev dependencies, no TypeScript, no full node_modules.
# ============================================================
FROM node:22-bookworm-slim AS runtime

WORKDIR /app

# Install curl for the HEALTHCHECK (consistent with backend image)
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Create a dedicated non-root user/group
ARG USER=nextjs
ARG GROUP=nodejs
ARG UID=1001
ARG GID=1001
RUN groupadd -g ${GID} ${GROUP} \
 && useradd -m -u ${UID} -g ${GROUP} -s /bin/sh ${USER}

# -- Copy standalone server -----------------------------------
# .next/standalone/ contains server.js + bundled server-side deps.
# Static assets and public files must be copied alongside it.
COPY --from=builder --chown=${USER}:${GROUP} /app/.next/standalone ./
COPY --from=builder --chown=${USER}:${GROUP} /app/.next/static     ./.next/static
COPY --from=builder --chown=${USER}:${GROUP} /app/public           ./public

# The runtime image contains ONLY:
#   server.js           (Next.js standalone server)
#   .next/standalone/   (server-side bundled deps)
#   .next/static/       (client-side assets)
#   public/             (static public files)
#
# It does NOT contain:
#   src/              -- source code excluded
#   node_modules/     -- full dev deps excluded
#   tsconfig.json     -- TypeScript config excluded
#   package-lock.json -- excluded

USER ${USER}:${GROUP}

# Production runtime settings
ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
ENV NEXT_TELEMETRY_DISABLED=1

EXPOSE 3000

# OCI image labels
LABEL org.opencontainers.image.title="Street Vendor Platform Frontend"
LABEL org.opencontainers.image.description="Next.js 15 frontend for the Street Vendor Platform"
LABEL org.opencontainers.image.vendor="Street Vendor Platform"
LABEL org.opencontainers.image.licenses="MIT"

# Health check -- curl against the root path served by Next.js
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD curl --fail --silent http://localhost:3000/ || exit 1

# Exec-form entrypoint -- no shell wrapper, receives SIGTERM directly
ENTRYPOINT ["node", "server.js"]
