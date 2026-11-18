import java.security.SecureRandom
import java.util.Base64
import java.util.Properties

plugins {
    id("java")
    id("application")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.github.jk1.dependency-license-report")
    // JaCoCo applied from parent build.gradle.kts
}

group = "com.mathtutor"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

// Ambos lanzadores resuelven .env desde la raíz, también en Windows.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    workingDir(rootProject.projectDir)
}

val localEnvFile = rootProject.layout.projectDirectory.file(
    providers.gradleProperty("localEnvFile").orElse(".env").get()).asFile

tasks.register("initLocalEnv") {
    group = "application"
    description = "Crea .env con secretos locales aleatorios; nunca sobrescribe uno existente"
    doLast {
        if (localEnvFile.exists()) {
            logger.lifecycle("El archivo local ya existe; se conserva sin cambios.")
        } else {
            fun secret(): String {
                val bytes = ByteArray(48)
                SecureRandom().nextBytes(bytes)
                return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
            }
            val template = rootProject.file(".env.example").readText(Charsets.UTF_8)
                .replace("\r\n", "\n")
                .replace("POSTGRES_PASSWORD=\n", "POSTGRES_PASSWORD=${secret()}\n")
                .replace("JWT_SECRET=\n", "JWT_SECRET=${secret()}\n")
            localEnvFile.parentFile.mkdirs()
            localEnvFile.writeText(template, Charsets.UTF_8)
            logger.lifecycle("Archivo local creado. Configura GOOGLE_AI_API_KEY antes de iniciar la IA.")
        }
    }
}

val checkLocalAi = tasks.register("checkLocalAi") {
    group = "verification"
    description = "Comprueba las variables necesarias para iniciar con IA sin mostrar secretos"
    doLast {
        if (!localEnvFile.isFile) throw GradleException("Falta .env. Ejecuta initLocalEnv y configura la clave de IA.")
        val values = Properties().apply {
            localEnvFile.reader(Charsets.UTF_8).use { load(it) }
        }
        for (name in listOf("POSTGRES_PASSWORD", "JWT_SECRET", "GOOGLE_AI_API_KEY")) {
            val value = System.getenv(name) ?: values.getProperty(name)
            if (value.isNullOrBlank()) throw GradleException("Falta $name en el entorno o en .env.")
            if (name == "JWT_SECRET" && value.length < 32) throw GradleException("JWT_SECRET debe tener al menos 32 caracteres.")
        }
        logger.lifecycle("Configuración local de PostgreSQL, JWT e IA disponible.")
    }
}

val localPostgres = tasks.register<Exec>("localPostgres") {
    group = "application"
    description = "Inicia PostgreSQL+pgvector con las credenciales de este dispositivo"
    dependsOn(checkLocalAi)
    workingDir(rootProject.projectDir)
    // Compose recibe los valores mediante el entorno, sin volver a interpretar secretos.
    commandLine("docker", "compose", "--env-file", rootProject.file(".env.example").absolutePath,
        "-f", rootProject.file("docker-compose.yml").absolutePath, "up", "-d", "--wait", "postgres")
    doFirst {
        // La misma lectura que Spring: un $ en la contraseña no se interpola en Compose.
        val values = Properties().apply {
            localEnvFile.reader(Charsets.UTF_8).use { load(it) }
        }
        for (name in listOf("POSTGRES_DB", "POSTGRES_USER", "POSTGRES_PASSWORD", "POSTGRES_PORT", "COMPOSE_PROJECT_NAME")) {
            if (System.getenv(name) == null && values.getProperty(name) != null) {
                environment(name, values.getProperty(name))
            }
        }
    }
    onlyIf { !providers.gradleProperty("skipLocalPostgres").isPresent }
}

tasks.register<org.springframework.boot.gradle.tasks.run.BootRun>("localAi") {
    group = "application"
    description = "Inicia PostgreSQL y el tutor con Google AI Studio y los apuntes indexados"
    dependsOn(localPostgres, tasks.named("classes"))
    mainClass.set("com.mathtutor.application.MathTutorApplication")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir(rootProject.projectDir)
    args("--spring.profiles.active=dev,google-ai-studio,notes-indexing",
        "--spring.config.import=${localEnvFile.toURI()}[.properties]")
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/snapshot") }
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0")
        mavenBom("org.testcontainers:testcontainers-bom:1.21.4")
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Flyway (not a Spring Boot starter in 3.x, use core)
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // pgvector - for vector extension
    implementation("com.pgvector:pgvector:0.1.6")

    // Security - use Spring Security's BCrypt instead of Argon2 (available in Maven Central)
    implementation("org.springframework.security:spring-security-crypto")

    // Project dependencies - application connects all modules
    implementation(project(":mathtutor-common"))
    implementation(project(":mathtutor-persistence"))
    implementation(project(":mathtutor-auth"))
    implementation(project(":mathtutor-cas"))
    implementation(project(":mathtutor-llm"))
    implementation(project(":mathtutor-graphics"))
    implementation(project(":mathtutor-tutor"))

    // Spring AI Model for EmbeddingModel interface
    implementation("org.springframework.ai:spring-ai-model")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.18")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.0")
    testRuntimeOnly("com.h2database:h2")
    // JUnit Platform Launcher required for Gradle JVM Test Suite
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
    // PIT mutation testing - JUnit 5 plugin
    testRuntimeOnly("org.pitest:pitest-junit5-plugin:1.2.3")
}

// Module-specific JaCoCo exclusions
// Exclude main application class (entry point)
tasks.withType<Test> {
    jacoco {
        exclude("com.mathtutor.application.MathTutorApplication")
    }
    // Pass Docker configuration to test JVM for Testcontainers
    // Use BOTH system properties and environment variables to ensure Testcontainers sees them
    // Testcontainers checks: 1) env vars, 2) system properties, 3) defaults
    // Testcontainers detecta Docker en Windows y Linux y respeta el entorno del usuario.

    // JVM Test Suite workaround: configure fork options to avoid EOFException on Windows
    // Use a dedicated fork with isolated classloader
    maxParallelForks = 1
    forkEvery = 100
    jvmArgs = listOf("-Xmx2g", "-XX:+UseSerialGC")
    // Ensure test results are written immediately
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Exclude all external API tests from the regular test run: consumen cuota y no son deterministas.
// Exclude detector-demo tagged tests (demonstration test that deliberately fails to prove detector works)
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("real-api", "external-api", "detector-demo")
    }
}

application {
    mainClass.set("com.mathtutor.application.MathTutorApplication")
}

springBoot {
    mainClass.set("com.mathtutor.application.MathTutorApplication")
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

    // Disable test logging that could leak secrets
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }

    // Load .env file from project root and set GOOGLE_AI_API_KEY as system property for test JVM
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
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.lifecycle("Skipping realApiTest: GOOGLE_AI_API_KEY not set. Set it in .env or environment to run real API tests.")
            enabled = false
        }
    }
}

// detectorDemoTest task: runs the demonstration test that deliberately logs a key to prove detector works
tasks.register("detectorDemoTest", Test::class) {
    group = "Verification"
    description = "Runs detector demo test (DELIBERATELY FAILS to prove detector catches logged keys)"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        includeTags("detector-demo")
    }

    testLogging {
        events("passed", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

