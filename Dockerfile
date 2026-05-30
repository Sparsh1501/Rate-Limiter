# ---- Build stage ----
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

# Cache Gradle wrapper + dependency metadata first for faster rebuilds.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --version

# Copy sources and build the boot jar (skip tests; they need Docker/Redis).
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# Run as a non-root user.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER app

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
