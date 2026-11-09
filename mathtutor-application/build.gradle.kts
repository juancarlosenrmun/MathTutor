plugins {
    id("java")
    id("application")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
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

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0")
        mavenBom("org.testcontainers:testcontainers-bom:1.21.4")
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

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
    implementation(project(":mathtutor-auth"))
    implementation(project(":mathtutor-cas"))
    implementation(project(":mathtutor-llm"))
    implementation(project(":mathtutor-graphics"))
    implementation(project(":mathtutor-tutor"))

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
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
    // Pass Docker configuration to test JVM for Testcontainers
    // Use BOTH system properties and environment variables to ensure Testcontainers sees them
    // Testcontainers checks: 1) env vars, 2) system properties, 3) defaults
    systemProperty("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")
    systemProperty("DOCKER_HOST", "unix:///var/run/docker.sock")
    systemProperty("TESTCONTAINERS_RYUK_DISABLED", "true")
    systemProperty("testcontainers.docker.socket.override", "/var/run/docker.sock")
    systemProperty("docker.host", "unix:///var/run/docker.sock")
    environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")
    environment("DOCKER_HOST", "unix:///var/run/docker.sock")
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
}

application {
    mainClass.set("com.mathtutor.application.MathTutorApplication")
}

springBoot {
    mainClass.set("com.mathtutor.application.MathTutorApplication")
}