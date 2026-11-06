plugins {
    id("java")
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
    implementation(project(":mathtutor-common"))
    implementation("io.projectreactor:reactor-core")

    // pgvector Java client for direct vector operations
    implementation("com.pgvector:pgvector:0.1.6")

    // Spring JDBC for database operations
    implementation("org.springframework:spring-jdbc:6.1.6")

    // Spring Context for DI and annotations (@Component, @Configuration, @Bean, @Profile)
    implementation("org.springframework:spring-context:6.1.6")

    // Spring Data JPA for JpaRepository
    implementation("org.springframework.data:spring-data-jpa:3.2.5")

    // Spring AI - INTENTADO Y FALLADO:
    // - spring-ai-bom:1.0.0-M4 desde repo.spring.io/milestone -> NO resuelve spring-ai-pgvector-store ni spring-ai-openai
    // - spring-ai-bom:1.0.0-M3, 0.8.1 -> tampoco disponibles en Maven Central ni milestone/snapshot
    // ACTUALIZACIÓN 2026-11-06: Spring AI 1.0.0 GA SÍ está disponible en Maven Central (desde abril 2024)
    //   - BOM: org.springframework.ai:spring-ai-bom:1.0.0
    //   - Módulos: spring-ai-pgvector-store, spring-ai-openai
    //   - Compatible con Spring Boot 3.2.5
    //   - DECISIÓN: Mantener pgvector JDBC directo por ahora (EN-04 spike scope), migrar a Spring AI VectorStore en S2/S3
    // Se usa implementación directa con pgvector JDBC (com.pgvector:pgvector + spring-jdbc)

    // Persistence module for entity/repository access
    implementation(project(":mathtutor-persistence"))

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.18")
    testImplementation("io.projectreactor:reactor-test")

    // Testcontainers for integration tests
    testImplementation("org.testcontainers:junit-jupiter:1.20.0")
    testImplementation("org.testcontainers:postgresql:1.20.0")
}

// Sin exclusiones JaCoCo - objetivo ≥80% líneas / ≥70% ramas con tests unitarios
// tests Docker (@Testcontainers disabledWithoutDocker=true) no cuentan para el umbral
