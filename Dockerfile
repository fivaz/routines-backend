FROM gradle:8.6-jdk21 AS build
WORKDIR /app
COPY . .

# Mount the secret and set it as an environment variable for Gradle
RUN --mount=type=secret,id=sentry_token,env=SENTRY_AUTH_TOKEN \
    gradle build --no-daemon

FROM openjdk:21-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
COPY sentry-opentelemetry-agent-8.2.0.jar /app/

EXPOSE 8080

ENV JAVA_OPTS="-javaagent:/app/sentry-opentelemetry-agent-8.2.0.jar"

ENTRYPOINT ["java", "-jar", "app.jar"]