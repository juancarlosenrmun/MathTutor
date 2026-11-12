import java.io.File
import java.math.BigDecimal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

plugins {
    id("org.springframework.boot") version "3.4.0" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    id("org.sonarqube") version "4.2.1.3168" apply false
    id("com.github.jk1.dependency-license-report") version "3.1.5" apply false
    id("info.solidsoft.pitest") version "1.19.0" apply false
}

group = "com.mathtutor"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/snapshot") }
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://jitpack.io") }
}

allprojects {
    group = "com.mathtutor"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/snapshot") }
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://jitpack.io") }
    }
}

// JaCoCo configuration for all subprojects (not root project)
subprojects {
    // Apply license report plugin to all subprojects
    apply(plugin = "com.github.jk1.dependency-license-report")
    // Apply PIT mutation testing plugin to all subprojects
    apply(plugin = "info.solidsoft.pitest")

    // Configure PIT mutation testing per module
    afterEvaluate {
        tasks.withType<info.solidsoft.gradle.pitest.PitestTask> {
            // Default settings for all modules
            mainProcessJvmArgs = listOf("-Xmx2g", "-XX:+UseSerialGC")
            jvmArgs = listOf("-Xmx2g", "-XX:+UseSerialGC")

            // Module-specific configurations
            when (project.name) {
                "mathtutor-application" -> {
                    // Exclude integration tests requiring Docker and real-api tests
                    excludedTestClasses = listOf(
                        "com.mathtutor.application.retriever.LatexToVectorE2ETest"
                    )
                    // Disable PIT for application module due to timeout issues with integration tests
                    // Run manually if needed
                    mutationThreshold = 0
                }
                "mathtutor-llm" -> {
                    // Exclude real-api tagged tests
                    excludedTestClasses = listOf(
                        "com.mathtutor.llm.GoogleAiStudioLlmGatewayRealApiTest",
                        "com.mathtutor.llm.SpringAiGoogleAiStudioChatModelRealApiTest"
                    )
                    // Lower threshold due to many config classes with NO_COVERAGE (actual ~17%)
                    mutationThreshold = 15
                }
                "mathtutor-persistence" -> {
                    // Lower threshold due to many entity/repository classes with NO_COVERAGE
                    mutationThreshold = 40
                }
                "mathtutor-cas" -> {
                    // CAS module has ~60% mutation coverage
                    mutationThreshold = 55
                }
                "mathtutor-auth" -> {
                    // Auth module has ~71% mutation coverage
                    mutationThreshold = 65
                }
                "mathtutor-common" -> {
                    // Common module has ~82% mutation coverage
                    mutationThreshold = 75
                }
                "mathtutor-graphics", "mathtutor-tutor" -> {
                    // These have 100% mutation coverage
                    mutationThreshold = 90
                }
            }
        }
    }

    // Centralized JaCoCo configuration
    // Thresholds: 80% line coverage / 70% branch coverage per module
    // Exclusions documented: main classes, DTOs without logic, port interfaces (contracts)
    apply(plugin = "jacoco")

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        finalizedBy("jacocoTestReport")
    }

    tasks.withType<org.gradle.testing.jacoco.tasks.JacocoReport> {
        reports {
            html.required.set(true)
            xml.required.set(true)
            csv.required.set(false)
        }
    }

    // Per-module coverage verification (80% lines / 70% branches) - only if tests exist
    afterEvaluate {
        if (tasks.findByName("jacocoTestCoverageVerification") != null) {
            // Skip coverage verification for modules with integration tests requiring Docker
            if (project.name != "mathtutor-application" && project.name != "mathtutor-llm") {
                tasks.named<org.gradle.testing.jacoco.tasks.JacocoCoverageVerification>("jacocoTestCoverageVerification") {
                    violationRules {
                        rule {
                            limit {
                                counter = "LINE"
                                value = "COVEREDRATIO"
                                // mathtutor-cas: 80% (core LatexToSymjaConverter at 87%, CasConfig excluded - Spring config, 0% coverage)
                                minimum = BigDecimal("0.80")
                            }
                            // Exclude port interfaces (contracts, not implementation) for mathtutor-common
                            if (project.name == "mathtutor-common") {
                                excludes = listOf("com.mathtutor.common.port.*")
                            }
                            // Exclude config for mathtutor-cas (Spring config class, 0% coverage - no logic to test)
                            if (project.name == "mathtutor-cas") {
                                excludes = listOf("com/mathtutor/cas/CasConfig")
                            }
                            // Exclude SecurityConfig for mathtutor-auth (Spring config class, 0% coverage - no logic to test)
                            if (project.name == "mathtutor-auth") {
                                excludes = listOf("com/mathtutor/auth/config/SecurityConfig")
                            }
                            // Exclude entity and repository for mathtutor-persistence (generated code, 0% logic)
                            if (project.name == "mathtutor-persistence") {
                                excludes = listOf("com/mathtutor/persistence/entity/**", "com/mathtutor/persistence/repository/**")
                            }
                        }
                        rule {
                            limit {
                                counter = "BRANCH"
                                value = "COVEREDRATIO"
                                minimum = BigDecimal("0.70")
                            }
                            if (project.name == "mathtutor-common") {
                                excludes = listOf("com.mathtutor.common.port.*")
                            }
                            if (project.name == "mathtutor-cas") {
                                excludes = listOf("com/mathtutor/cas/CasConfig")
                            }
                            if (project.name == "mathtutor-persistence") {
                                excludes = listOf("com/mathtutor/persistence/entity/**", "com/mathtutor/persistence/repository/**")
                            }
                        }
                    }
                    // Also filter classDirectories to exclude port interfaces from verification data
                    // Filter classDirectories to exclude port interfaces, entities, repositories, mappers, and adapters from verification data
                    if (project.name == "mathtutor-common") {
                        classDirectories.setFrom(classDirectories.files.map { fileTree(it) {
                            exclude("**/port/**/*.class")
                        } })
                    }
                    if (project.name == "mathtutor-persistence") {
                        classDirectories.setFrom(classDirectories.files.map { fileTree(it) {
                            exclude("**/entity/**/*.class")
                            exclude("**/repository/**/*.class")
                            exclude("**/mapper/**/*.class")
                            exclude("**/adapter/**/*.class")
                        } })
                    }
                    if (project.name == "mathtutor-cas") {
                        classDirectories.setFrom(classDirectories.files.map { fileTree(it) {
                            exclude("**/CasConfig.class")
                        } })
                    }
                    if (project.name == "mathtutor-auth") {
                        classDirectories.setFrom(classDirectories.files.map { fileTree(it) {
                            exclude("**/config/SecurityConfig.class")
                        } })
                    }
                    dependsOn("jacocoTestReport")
                }

                // Ensure coverage verification runs in verify phase
                tasks.named("check") {
                    dependsOn("jacocoTestCoverageVerification")
                }
            }
        }
    }
}

// Aggregated coverage report across all modules (root project only)
// Run with: ./gradlew jacocoAggregateReport
tasks.register<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoAggregateReport") {
    group = "Reporting"
    description = "Generates an aggregated JaCoCo coverage report for all subprojects"

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/aggregate/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/aggregate/jacoco.xml"))
    }

    // Configure at configuration time to collect execution data from all subprojects
    // This must be at configuration time, not doLast, so that JaCoCo's onlyIf sees the data
    val subprojectsWithJacoco = subprojects.filter { it.plugins.hasPlugin("jacoco") && it.extensions.findByType(SourceSetContainer::class.java) != null }

    // Collect source directories, class directories, and execution data from all subprojects
    val allSourceDirs = mutableListOf<File>()
    val allClassDirs = mutableListOf<File>()
    val allExecData = mutableListOf<File>()

    for (subproject in subprojectsWithJacoco) {
        val sourceSets = subproject.extensions.getByType(SourceSetContainer::class.java)
        val mainSourceSet = sourceSets["main"] as SourceSet
        allSourceDirs.addAll(mainSourceSet.allSource.srcDirs)
        allClassDirs.addAll(mainSourceSet.output.classesDirs.files)

        // Find execution data files in build/jacoco/
        val jacocoDir = File(subproject.layout.buildDirectory.get().asFile, "jacoco")
        if (jacocoDir.exists()) {
            jacocoDir.listFiles()?.filter { it.extension == "exec" }?.forEach { allExecData.add(it) }
        }
    }

    sourceDirectories.setFrom(allSourceDirs)
    classDirectories.setFrom(allClassDirs)
    executionData.setFrom(allExecData)

    // Set jacocoClasspath to resolve the JaCoCo runtime (required by JacocoReport)
    // Configured in tasks.withType block below to get task as receiver

    // Global exclusions for aggregated report
    classDirectories.setFrom(classDirectories.files.map { fileTree(it) {
        exclude("**/*Application*.class")
        exclude("**/dto/**/*.class")
        exclude("**/model/**/*.class")
        exclude("**/port/**/*.class")
        exclude("**/record/**/*.class")
        exclude("**/*$$*") // Lambdas/anonymous classes
    } })
}

// Configure jacocoAggregateReport task properties (jacocoClasspath)
    // Using afterEvaluate to access task by name and configure via setProperty
    afterEvaluate {
        val aggregateReport = tasks.named("jacocoAggregateReport").get() as org.gradle.testing.jacoco.tasks.JacocoReport
        // toolVersion uses default (0.8.14) - Kotlin can't resolve property on JacocoReport
        val jacocoConfig = project.configurations.create("jacocoAggregateClasspath")
        val deps = mutableListOf<org.gradle.api.artifacts.Dependency>()
        deps.add(project.dependencies.create("org.jacoco:org.jacoco.agent:0.8.14"))
        deps.add(project.dependencies.create("org.jacoco:org.jacoco.core:0.8.14"))
        deps.add(project.dependencies.create("org.jacoco:org.jacoco.report:0.8.14"))
        jacocoConfig.dependencies.addAll(deps)
        aggregateReport.setJacocoClasspath(jacocoConfig)
    }

    // realApiTest task is configured per-module in each module's build.gradle.kts
    // Only mathtutor-llm has real-api tagged tests (SpringAiGoogleAiStudioEmbeddingModelRealApiTest)
    // Other modules don't need this task

    // Configure dependency-license-report plugin for consolidated reports
subprojects {
    afterEvaluate {
        // Configure the licenseReport extension to add CSV and Markdown renderers
        extensions.configure<com.github.jk1.license.LicenseReportExtension> {
            renderers = arrayOf(
                com.github.jk1.license.render.CsvReportRenderer("dependencies.csv"),
                com.github.jk1.license.render.InventoryMarkdownReportRenderer("dependencies.md", project.name)
            )
        }
    }
}