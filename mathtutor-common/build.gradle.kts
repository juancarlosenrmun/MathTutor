import java.math.BigDecimal

plugins {
    id("java")
    id("jacoco")
}

group = "com.mathtutor"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

dependencies {
    // Jackson for JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")

    // Reactor for reactive streams
    implementation("io.projectreactor:reactor-core:3.6.7")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.18")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }

    // Configure JaCoCo to exclude port interfaces (contracts, not implementation)
    jacoco {
        exclude("com.mathtutor.common.port.**")
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