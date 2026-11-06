plugins {
    id("java")
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

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.0")
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0")
    }
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

    // Vector store para pgvector (version from Spring AI BOM)
    implementation("org.springframework.ai:spring-ai-pgvector-store")
    // Embedding model (OpenAI, pero usaremos uno fake para tests) (version from Spring AI BOM)
    implementation("org.springframework.ai:spring-ai-openai")

    // Lombok (explicit version since BOM doesn't resolve for annotationProcessor)
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    // Test dependencies - versions managed by Spring Boot BOM via dependencyManagement (except equalsverifier)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.18")
    testImplementation("io.projectreactor:reactor-test")

    // Testcontainers for integration tests
    testImplementation("org.testcontainers:junit-jupiter:1.20.0")
    testImplementation("org.testcontainers:postgresql:1.20.0")
}

// Sin exclusiones JaCoCo - objetivo ≥80% líneas / ≥70% ramas con tests unitarios
// tests Docker (@Testcontainers disabledWithoutDocker=true) no cuentan para el umbral
