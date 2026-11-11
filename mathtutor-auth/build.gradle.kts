plugins {
    id("java")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.github.jk1.dependency-license-report")
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
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework:spring-tx")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("com.auth0:java-jwt:4.5.0")

    // Argon2 for password hashing
    implementation("de.mkammerer:argon2-jvm:2.11")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // pgvector
    implementation("com.pgvector:pgvector:0.1.6")

    // Project dependencies
    implementation(project(":mathtutor-common"))
    implementation(project(":mathtutor-persistence"))

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.18")
    // JUnit Platform Launcher required for Gradle JVM Test Suite
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
    // PIT mutation testing - JUnit 5 plugin
    testRuntimeOnly("org.pitest:pitest-junit5-plugin:1.2.3")

    // ArchUnit for architecture tests
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.0")
}

tasks.named("bootJar") {
    enabled = false
}