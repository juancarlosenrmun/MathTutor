package com.mathtutor.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/** EN-01: reglas ejecutadas desde application, donde están todos los módulos. */
@AnalyzeClasses(packages = "com.mathtutor", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String[] MODULES = {
        "common", "persistence", "auth", "llm", "graphics", "tutor", "cas", "application"
    };

    @ArchTest
    static void allModulesArePresent(JavaClasses classes) {
        for (String module : MODULES) {
            assertThat(classes.stream().anyMatch(type -> type.getPackageName().startsWith("com.mathtutor." + module)))
                    .as("El módulo %s debe estar incluido en la auditoría", module).isTrue();
        }
    }

    @ArchTest
    static void modulesDependOnPorts(JavaClasses classes) {
        for (String source : MODULES) {
            if (source.equals("application")) {
                continue; // La raíz de composición conecta las implementaciones.
            }
            for (String target : MODULES) {
                if (source.equals(target) || target.equals("common")
                        || (source.equals("auth") && target.equals("persistence"))) {
                    continue;
                }
                noClasses().that().resideInAPackage("com.mathtutor." + source + "..")
                        .should().dependOnClassesThat().resideInAPackage("com.mathtutor." + target + "..")
                        .because("Los módulos colaboran mediante los puertos de common")
                        .check(classes);
            }
        }
    }

    @ArchTest
    static final ArchRule noCyclesBetweenModules =
            SlicesRuleDefinition.slices().matching("com.mathtutor.(*)..")
                    .should().beFreeOfCycles();
}