# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM gradle:8.7-jdk17 AS builder
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
# Cache dependencies layer
RUN gradle dependencies --no-daemon 2>/dev/null || true

COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
