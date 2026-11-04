plugins {
    id("java")
    id("application")
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.5"
    // JaCoCo applied from parent build.gradle.kts
}

group = "com.mathtutor"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/snapshot") }
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Flyway (not a Spring Boot starter in 3.x, use core)
    implementation("org.flywaydb:flyway-core")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // pgvector - for vector extension
    implementation("com.pgvector:pgvector:0.1.6")

    // Security - use Spring Security's BCrypt instead of Argon2 (available in Maven Central)
    implementation("org.springframework.security:spring-security-crypto")

    // Project dependencies - application connects all modules
    implementation(project(":mathtutor-common"))
    implementation(project(":mathtutor-persistence"))
    implementation(project(":mathtutor-llm"))
    implementation(project(":mathtutor-graphics"))
    implementation(project(":mathtutor-tutor"))

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
    testImplementation("org.testcontainers:postgresql:1.19.7")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.18")
    testImplementation("io.projectreactor:reactor-test")
}

// Module-specific JaCoCo exclusions
// Exclude main application class (entry point)
tasks.withType<Test> {
    jacoco {
        exclude("com.mathtutor.application.MathTutorApplication")
    }
}

application {
    mainClass.set("com.mathtutor.application.MathTutorApplication")
}

springBoot {
    mainClass.set("com.mathtutor.application.MathTutorApplication")
}