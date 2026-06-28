# Environment Configuration & Secret Management

This document defines the unified environment configuration model for the Street Vendor Platform. All configurations (local development, Docker, CI/CD, and production deployments) rely on a single, environment‑driven system.

---

## 1. Getting Started (Local Development)

### Copying the Example Environment File
To configure your local environment, copy the template file to a concrete `.env` file:
```bash
cp .env.example .env
```
*(On Windows PowerShell: `Copy-Item .env.example .env`)*

### Naming Conventions
- Every environment variable must use **`UPPER_SNAKE_CASE`** (e.g., `DB_HOST`, `JWT_SECRET`, `NEXT_PUBLIC_API_URL`).
- Frontend variables that need exposure to the browser must be prefixed with **`NEXT_PUBLIC_`** (e.g., `NEXT_PUBLIC_API_URL`).

### Default Values & Secrets
- Non-secret configurations (e.g., `SERVER_PORT: 8080`, `LOG_LEVEL: INFO`) have sensible fallbacks in the codebase.
- Critical credentials and secrets (e.g., `JWT_SECRET`, `RAZORPAY_KEY_SECRET`, `R2_SECRET_KEY`) **must never** have fallback values in the configuration files or code.

---

## 2. Spring Boot Env-Var Resolution

Spring Boot parses configuration files (`application.yml`) and replaces placeholders using the `${ENV_VAR_NAME}` syntax.

For example, in [application.yml](file:///c:/Users/devan/sem_6%20pbl/vendor-registration/src/main/resources/application.yml):
```yaml
server:
  port: ${SERVER_PORT:8080}
```
If `SERVER_PORT` is set in the environment, Spring Boot uses it. Otherwise, it falls back to `8080`.

Java components retrieve these values via `@Value` or `@ConfigurationProperties` classes. For example, [DocumentUploadValidator](file:///c:/Users/devan/sem_6%20pbl/vendor-registration/src/main/java/com/streetvendor/vendor/validation/DocumentUploadValidator.java) uses:
```java
public DocumentUploadValidator(
        @Value("${security.max-file-size-pdf-mb:5}") long maxPdfSizeMb,
        @Value("${security.max-file-size-image-mb:2}") long maxImageSizeMb)
```

---

## 3. Docker Compose Consumption

Docker Compose automatically loads variables from a file named `.env` in the same directory as `docker-compose.yml`.

In future phases, you can declare:
```yaml
services:
  backend:
    image: street-vendor-backend
    env_file:
      - .env
```
This passes all environment variables defined in `.env` into the backend container, where Spring Boot reads them.

---

## 4. Next.js (Frontend) Consumption

Next.js automatically loads `.env` files. During the production build (`next build`), Next.js embeds any environment variable starting with `NEXT_PUBLIC_` directly into the JavaScript bundle sent to the client browser.

- Non-prefixed variables (e.g. `JWT_SECRET`) are only accessible in Node.js server‑side code and are hidden from the browser.
- In Next.js, use:
  ```typescript
  const api_url = process.env.NEXT_PUBLIC_API_URL;
  ```

---

## 5. Render Deployment Mapping

To deploy the platform to Render:
1. Navigate to the **Render Dashboard**.
2. Go to your Web Service settings page.
3. Click on the **Environment** tab.
4. Add the required environment variables listed in `.env.example` individually (or create an **Environment Group** to share them across the database, backend, and frontend).
5. Render exposes these variables to the runtime container automatically.

---

## 6. GitHub Actions Mapping

To supply credentials to the CI/CD pipeline without exposing them:
1. Define secrets in your GitHub repository: **Settings** -> **Secrets and variables** -> **Actions** -> **New repository secret**.
2. Reference the secrets in your workflow YAML files (`.github/workflows/*.yml`):
   ```yaml
   env:
     DATABASE_URL: ${{ secrets.DATABASE_URL }}
     DB_USERNAME: ${{ secrets.DB_USERNAME }}
     DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
     JWT_SECRET: ${{ secrets.JWT_SECRET }}
     RAZORPAY_KEY_ID: ${{ secrets.RAZORPAY_KEY_ID }}
     RAZORPAY_KEY_SECRET: ${{ secrets.RAZORPAY_KEY_SECRET }}
     R2_ACCESS_KEY: ${{ secrets.R2_ACCESS_KEY }}
     R2_SECRET_KEY: ${{ secrets.R2_SECRET_KEY }}
   ```
