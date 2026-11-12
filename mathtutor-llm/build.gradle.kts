plugins {
    id("java")
    id("io.spring.dependency-management")
    id("com.github.jk1.dependency-license-report")
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
        mavenBom("org.testcontainers:testcontainers-bom:1.21.4")
    }
}

dependencies {
    implementation(project(":mathtutor-common"))
    implementation(project(":mathtutor-persistence"))
    implementation("io.projectreactor:reactor-core")

    // pgvector Java client for direct vector operations
    implementation("com.pgvector:pgvector:0.1.6")

    // Spring JDBC for database operations
    implementation("org.springframework:spring-jdbc:6.1.6")

    // Spring Context for DI and annotations (@Component, @Configuration, @Bean, @Profile)
    implementation("org.springframework:spring-context:6.1.6")

    // Spring Boot Autoconfigure for @ConfigurationProperties, @EnableConfigurationProperties
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    // Validation API for @Validated, @NotNull, @DecimalMin, @DecimalMax
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")

    // Spring Data JPA for JpaRepository
    implementation("org.springframework.data:spring-data-jpa:3.2.5")

    // Vector store para pgvector (version from Spring AI BOM)
    implementation("org.springframework.ai:spring-ai-pgvector-store")
    // Embedding model (OpenAI, pero usaremos uno fake para tests) (version from Spring AI BOM)
    implementation("org.springframework.ai:spring-ai-openai")
    // Ollama embedding model for local embeddings (version from Spring AI BOM)
    implementation("org.springframework.ai:spring-ai-ollama")

    // Spring WebFlux for WebClient (streaming)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Jackson for JSON parsing in streaming responses
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // Lombok (explicit version since BOM doesn't resolve for annotationProcessor)
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    // Test dependencies - versions managed by Spring Boot BOM via dependencyManagement (except equalsverifier)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.18")
    testImplementation("io.projectreactor:reactor-test")
    // JUnit Platform Launcher required for Gradle JVM Test Suite
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
    // PIT mutation testing - JUnit 5 plugin
    testRuntimeOnly("org.pitest:pitest-junit5-plugin:1.2.3")

    // Testcontainers for integration tests
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

// Sin exclusiones JaCoCo - objetivo ≥80% líneas / ≥70% ramas con tests unitarios
// tests Docker (@Testcontainers disabledWithoutDocker=true) no cuentan para el umbral

// Configure standard test task to pass GOOGLE_AI_API_KEY from .env and exclude real-api tagged tests
tasks.named<Test>("test") {
    // Exclude real-api tagged tests from regular test runs
    useJUnitPlatform {
        excludeTags("real-api")
    }
    doFirst {
        val rootDir = project.rootProject.projectDir
        val envFile = file("$rootDir/.env")
        if (envFile.exists()) {
            envFile.readLines().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isNotEmpty() && !line.startsWith("#")) {
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0]
                        val value = parts[1]
                        systemProperty(key, value)
                        environment(key, value)
                    }
                }
            }
        }
    }
}

// realApiTest task: runs tests tagged with "real-api" only if GOOGLE_AI_API_KEY is set
tasks.register("realApiTest", Test::class) {
    group = "Verification"
    description = "Runs real API integration tests (requires GOOGLE_AI_API_KEY env var)"

    // Explicitly configure test class directories and classpath from test source set
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        includeTags("real-api")
    }

    // Load .env file from project root and set GOOGLE_AI_API_KEY as system property for test JVM
    // Must be done in doFirst so it runs at execution time after test classes are compiled
    doFirst {
        val rootDir = project.rootProject.projectDir
        val envFile = file("$rootDir/.env")
        var apiKey: String? = null
        if (envFile.exists()) {
            envFile.readLines().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isNotEmpty() && !line.startsWith("#")) {
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0]
                        val value = parts[1]
                        systemProperty(key, value)
                        environment(key, value)
                        if (key == "GOOGLE_AI_API_KEY") {
                            apiKey = value
                        }
                    }
                }
            }
        }
        // Also check system env and system properties
        if (apiKey == null) {
            apiKey = System.getenv("GOOGLE_AI_API_KEY") ?: systemProperties["GOOGLE_AI_API_KEY"]?.toString()
        }
        logger.lifecycle("realApiTest: GOOGLE_AI_API_KEY = ${if (apiKey != null) "SET (len=${apiKey.length})" else "NOT SET"}")
        logger.lifecycle("realApiTest: systemProperties GOOGLE_AI_API_KEY = ${systemProperties["GOOGLE_AI_API_KEY"]}")
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.lifecycle("Skipping realApiTest: GOOGLE_AI_API_KEY not set. Set it in .env or environment to run real API tests.")
            enabled = false
        }
    }
}
