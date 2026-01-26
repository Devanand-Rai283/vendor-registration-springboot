# ---------- STAGE 1: BUILD ----------
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Copy project files
COPY . .

# Fix permissions for Maven Wrapper
RUN chmod +x mvnw

# Build the Spring Boot jar
RUN ./mvnw clean package -DskipTests


# ---------- STAGE 2: RUN ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy ONLY the jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# IMPORTANT: shell form so wildcard issues NEVER happen
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]
