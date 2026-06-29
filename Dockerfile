# ---------- Builder Stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Cache Maven dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && \
    ./mvnw -B dependency:go-offline

# Copy source and build the JAR (skip tests)
COPY src ./src
RUN chmod +x mvnw && \
    ./mvnw -B clean package -DskipTests

# Rename built JAR to deterministic name
RUN cp target/*.jar app.jar

# ---------- Runtime Stage ----------
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Install curl (Debian‑based Temurin image uses apt‑get)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create non‑root user/group
ARG USER=appuser
ARG GROUP=appgroup
ARG UID=1001
ARG GID=1001
RUN groupadd -g ${GID} ${GROUP} && \
    useradd -m -u ${UID} -g ${GROUP} -s /bin/sh ${USER}

# Copy deterministic JAR from builder
COPY --from=build /app/app.jar app.jar

# Permissions (read‑only)
RUN chown ${USER}:${GROUP} app.jar && chmod 500 app.jar

# Switch to non‑root user
USER ${USER}:${GROUP}

# JVM options via JAVA_TOOL_OPTIONS (container‑friendly defaults)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Djava.awt.headless=true"

# Optional timezone (comment out to keep UTC)
# ENV TZ=Asia/Kolkata

EXPOSE 8080

# OCI image labels
LABEL org.opencontainers.image.title="Street Vendor Platform Backend"
LABEL org.opencontainers.image.description="Spring Boot backend for the Street Vendor Platform"
LABEL org.opencontainers.image.vendor="Street Vendor Platform"
LABEL org.opencontainers.image.licenses="MIT"

# Healthcheck using curl with a 60‑second start period
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1

# Exec‑form entrypoint – no shell wrapper
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
