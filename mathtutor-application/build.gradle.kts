import java.math.BigDecimal

plugins {
    id("java")
    id("jacoco")
    id("application")
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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Spring AI
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter:0.8.1")
    implementation("org.springframework.ai:spring-ai-vector-store-pgvector:0.8.1")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.hibernate.orm:hibernate-core-jakarta:6.4.4.Final")

    // Security - Argon2
    implementation("org.springframework.security:spring-security-crypto")
    implementation("com.github.jerometheo:argon2-jna:1.4")

    // Symja CAS
    implementation("org.matheclipse:symja_jasymca:20240201")

    // Project dependencies
    implementation(project(":mathtutor-common"))
    implementation(project(":mathtutor-persistence"))
    implementation(project(":mathtutor-llm"))
    implementation(project(":mathtutor-graphics"))
    implementation(project(":mathtutor-tutor"))
    implementation(project(":mathtutor-web"))

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

application {
    mainClass.set("com.mathtutor.application.MathTutorApplication")
}

springBoot {
    mainClass.set("com.mathtutor.application.MathTutorApplication")
}