plugins {
    id("java")
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
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.0")
    }
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

    // Lombok (explicit version since BOM doesn't resolve for annotationProcessor)
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    // Testing - versions managed by Spring Boot BOM via dependencyManagement (except equalsverifier)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.18")
    // JUnit Platform Launcher required for Gradle JVM Test Suite
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
}

// Module-specific JaCoCo exclusions
tasks.withType<Test> {
    jacoco {
        exclude("com.mathtutor.cas.config.**")
    }
}