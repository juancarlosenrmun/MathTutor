import java.io.File
import java.math.BigDecimal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

plugins {
    id("org.springframework.boot") version "3.2.5" apply false
    id("io.spring.dependency-management") version "1.1.5" apply false
    id("org.sonarqube") version "4.2.1.3168" apply false
}

group = "com.mathtutor"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/snapshot") }
    maven { url = uri("https://repo.spring.io/milestone") }
}

allprojects {
    group = "com.mathtutor"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/snapshot") }
        maven { url = uri("https://repo.spring.io/milestone") }
    }
}

// JaCoCo configuration for all subprojects (not root project)
subprojects {
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

    // Configure lazily after all subprojects are evaluated
    doLast {
        val subprojectsWithJacoco = subprojects.filter { it.plugins.hasPlugin("jacoco") }

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
            val jacocoDir = File(subproject.buildDir, "jacoco")
            if (jacocoDir.exists()) {
                jacocoDir.listFiles()?.filter { it.extension == "exec" }?.forEach { allExecData.add(it) }
            }
        }

        sourceDirectories.setFrom(allSourceDirs)
        classDirectories.setFrom(allClassDirs)
        executionData.setFrom(allExecData)

        // Global exclusions for aggregated report
        // Main classes, DTOs, port interfaces, generated code (lambdas, etc.)
        classDirectories.setFrom(classDirectories.files.map { fileTree(it) {
            exclude("**/*Application*.class")
            exclude("**/dto/**/*.class")
            exclude("**/model/**/*.class")
            exclude("**/port/**/*.class")
            exclude("**/record/**/*.class")
            exclude("**/*$$*") // Lambdas/anonymous classes
        } })
    }
}

// Make aggregated report depend on all test tasks
tasks.named("jacocoAggregateReport") {
    dependsOn(subprojects.filter { it.plugins.hasPlugin("jacoco") }.map { it.tasks.named("test") })
}