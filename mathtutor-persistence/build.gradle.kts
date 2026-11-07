plugins {
    id("java")
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

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // pgvector - using pgvector-java library
    implementation("com.pgvector:pgvector:0.1.6")

    // Argon2 for password hashing
    implementation("org.mindrot:jbcrypt:0.4")

    // Project dependencies
    implementation(project(":mathtutor-common"))

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2:2.2.224")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.18")

    // ArchUnit for architecture tests
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.0")
}

tasks.named("bootJar") {
    enabled = false
}

// Module-specific JaCoCo exclusions can be added here if needed
// Example: exclude DTOs or generated code
// tasks.withType<Test> {
//     configure<JacocoTaskExtension> {
//         excludes.addAll(listOf("**/dto/**", "**/model/**"))
//     }
// }