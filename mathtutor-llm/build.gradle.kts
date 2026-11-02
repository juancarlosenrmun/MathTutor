import java.math.BigDecimal

plugins {
    id("java")
    id("jacoco")
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.5"
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
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring AI
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter:0.8.1")

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
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<JacocoReport> {
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.named("test") {
    finalizedBy("jacocoTestReport")
}

tasks.named("jacocoTestCoverageVerification") {
    configure<JacocoCoverageVerification> {
        violationRules {
            rule {
                limit {
                    minimum = BigDecimal("1")
                }
            }
        }
    }
    dependsOn("jacocoTestReport")
}