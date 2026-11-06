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

dependencies {
    implementation(project(":mathtutor-common"))
    // Lombok (version from Spring Boot 3.4.0 BOM)
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.18")
}

tasks.withType<Test> {
    jacoco {
        exclude("com.mathtutor.graphics.dto.**")
    }
}
