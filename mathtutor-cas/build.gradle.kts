plugins {
    id("java")
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
    // Symja - INTENTADO CON Maven Central:
    // Versiones disponibles en Central: 2.0.0, 3.0.0, 3.1.0, 3.1.1, 3.2.0 (latest)
    // Se intenta con 3.2.0 (la más reciente estable)
    implementation("org.matheclipse:matheclipse-core:3.2.0")

    // Spring for DI and annotations
    implementation("org.springframework:spring-context:6.1.6")

    // SLF4J for logging
    implementation("org.slf4j:slf4j-api:2.0.9")

    // Project dependencies
    implementation(project(":mathtutor-common"))

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.18")
}

// Module-specific JaCoCo exclusions
tasks.withType<Test> {
    jacoco {
        exclude("com.mathtutor.cas.config.**")
    }
}