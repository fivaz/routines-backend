FROM gradle:8.6-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
COPY src/main/resources/firebase-credentials.json /app/firebase-credentials.json
COPY src/main/resources/application.properties /app/application.properties

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]