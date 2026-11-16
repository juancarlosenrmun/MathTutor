plugins {
    id("java")
    id("io.spring.dependency-management")
    id("com.github.jk1.dependency-license-report")
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

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.0")
    }
}

dependencies {
    implementation(project(":mathtutor-common"))

    // Spring Context for @Component annotation
    implementation("org.springframework:spring-context")

    // Lombok (explicit version since BOM doesn't resolve for annotationProcessor)
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    // Testing - versions managed by Spring Boot BOM via dependencyManagement (except equalsverifier)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.18")
    // JUnit Platform Launcher required for Gradle JVM Test Suite
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
    // PIT mutation testing - JUnit 5 plugin
    testRuntimeOnly("org.pitest:pitest-junit5-plugin:1.2.3")
}

tasks.withType<Test> {
    jacoco {
        exclude("com.mathtutor.graphics.dto.**")
    }
}

// V8 mide el cliente real; solo se excluye main.mjs, punto de arranque sin lógica.
val clientTest by tasks.registering(Exec::class) {
    group = "verification"
    description = "Pruebas del visor con cobertura V8 obligatoria (80 % líneas / 70 % ramas)"
    workingDir(rootProject.projectDir)
    val evidence = layout.buildDirectory.dir("reports/client").get().asFile
    doFirst { evidence.mkdirs() }
    commandLine("node", "--test", "--experimental-test-coverage",
        "--test-coverage-include=**/static/visor/*.mjs",
        "--test-coverage-exclude=**/main.mjs",
        "--test-coverage-lines=80", "--test-coverage-branches=70",
        "--test-reporter=spec", "--test-reporter=lcov",
        "--test-reporter-destination=stdout",
        "--test-reporter-destination=${evidence.resolve("lcov.info").absolutePath}",
        "mathtutor-graphics/src/test/js/*.test.mjs")
}
tasks.named("check") { dependsOn(clientTest) }
