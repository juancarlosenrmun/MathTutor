plugins {
    id("java")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
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
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.18")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<Test> {
    jacoco {
        exclude("com.mathtutor.tutor.dto.**")
        exclude("com.mathtutor.tutor.strategy.**")
    }
}
