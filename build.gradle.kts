buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("java")
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    id("io.sentry.jvm.gradle") version "5.2.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Firebase Admin SDK
    implementation("com.google.firebase:firebase-admin:9.4.3")

    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("jakarta.validation:jakarta.validation-api:3.1.1")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

//    implementation("org.springframework.security:spring-security-core:6.4.3")
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}


sentry {
    // Generates a JVM (Java, Kotlin, etc.) source bundle and uploads your source code to Sentry.
    // This enables source context, allowing you to see your source
    // code as part of your stack traces in Sentry.
    includeSourceContext = true

    org = "fivaz-lb"
    projectName = "java-spring"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}

tasks.test {
    useJUnitPlatform()
}