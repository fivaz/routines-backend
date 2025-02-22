# Dockerfile
FROM gradle:8.6-jdk21 AS build
WORKDIR /app
COPY . .

ARG SENTRY_AUTH_TOKEN
ENV SENTRY_AUTH_TOKEN=${SENTRY_AUTH_TOKEN}

RUN gradle build --no-daemon

FROM openjdk:21-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
COPY sentry-opentelemetry-agent-8.2.0.jar /app/

EXPOSE 8080

ENV JAVA_OPTS="-javaagent:/app/sentry-opentelemetry-agent-8.2.0.jar"

ENTRYPOINT ["java", "-jar", "app.jar"]