# Stage 1: Build
FROM gradle:8.6-jdk21 AS build
WORKDIR /app
COPY . .

ARG SENTRY_AUTH_TOKEN
ENV SENTRY_AUTH_TOKEN=${SENTRY_AUTH_TOKEN}

RUN gradle build --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine-3.23
WORKDIR /app

# Copy the built jar
COPY --from=build /app/build/libs/*.jar app.jar

# Copy Sentry agent
COPY sentry-opentelemetry-agent-8.2.0.jar /app/

EXPOSE 8080

# Set Java options
ENV JAVA_OPTS="-javaagent:/app/sentry-opentelemetry-agent-8.2.0.jar"

# Entry point
ENTRYPOINT ["java", "-jar", "app.jar"]