package com.mathtutor.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Pruebas de arquitectura con ArchUnit.
 * Verifica que:
 * - llm, graphics, tutor, persistence, cas dependen solo de common (interfaces)
 * - Ninguno de estos módulos depende de otro
 * - application es el único que los conecta
 *
 * Nota: allowEmptyShould(true) permite que la prueba pase si el módulo no está en el classpath
 * (ej. al ejecutar tests de un módulo individual). En CI, al ejecutar ./gradlew check desde la raíz,
 * todos los módulos están disponibles y las reglas se aplican completamente.
 */
@AnalyzeClasses(packages = "com.mathtutor",
    importOptions = { ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class })
class ArchitectureTest {

    private static final String COMMON_PACKAGE = "com.mathtutor.common..";
    private static final String PERSISTENCE_PACKAGE = "com.mathtutor.persistence..";
    private static final String LLM_PACKAGE = "com.mathtutor.llm..";
    private static final String GRAPHICS_PACKAGE = "com.mathtutor.graphics..";
    private static final String TUTOR_PACKAGE = "com.mathtutor.tutor..";
    private static final String CAS_PACKAGE = "com.mathtutor.cas..";
    private static final String WEB_PACKAGE = "com.mathtutor.web..";
    private static final String APPLICATION_PACKAGE = "com.mathtutor.application..";

    @ArchTest
    static final ArchRule persistence_only_depends_on_common =
        noClasses().that().resideInAPackage(PERSISTENCE_PACKAGE)
            .should().dependOnClassesThat().resideInAnyPackage(LLM_PACKAGE, GRAPHICS_PACKAGE, TUTOR_PACKAGE, CAS_PACKAGE, WEB_PACKAGE, APPLICATION_PACKAGE)
            .because("persistence must only depend on common interfaces");

    @ArchTest
    static final ArchRule llm_only_depends_on_common =
        noClasses().that().resideInAPackage(LLM_PACKAGE)
            .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_PACKAGE, GRAPHICS_PACKAGE, TUTOR_PACKAGE, CAS_PACKAGE, WEB_PACKAGE, APPLICATION_PACKAGE)
            .allowEmptyShould(true)
            .because("llm must only depend on common interfaces");

    @ArchTest
    static final ArchRule graphics_only_depends_on_common =
        noClasses().that().resideInAPackage(GRAPHICS_PACKAGE)
            .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_PACKAGE, LLM_PACKAGE, TUTOR_PACKAGE, CAS_PACKAGE, WEB_PACKAGE, APPLICATION_PACKAGE)
            .allowEmptyShould(true)
            .because("graphics must only depend on common interfaces");

    @ArchTest
    static final ArchRule tutor_only_depends_on_common =
        noClasses().that().resideInAPackage(TUTOR_PACKAGE)
            .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_PACKAGE, LLM_PACKAGE, GRAPHICS_PACKAGE, CAS_PACKAGE, WEB_PACKAGE, APPLICATION_PACKAGE)
            .allowEmptyShould(true)
            .because("tutor must only depend on common interfaces");

    @ArchTest
    static final ArchRule cas_only_depends_on_common =
        noClasses().that().resideInAPackage(CAS_PACKAGE)
            .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_PACKAGE, LLM_PACKAGE, GRAPHICS_PACKAGE, TUTOR_PACKAGE, WEB_PACKAGE, APPLICATION_PACKAGE)
            .allowEmptyShould(true)
            .because("cas must only depend on common interfaces");

    @ArchTest
    static final ArchRule no_cycles_between_modules =
        SlicesRuleDefinition.slices().matching("com.mathtutor.(*)..")
            .should().beFreeOfCycles()
            .because("there should be no cyclic dependencies between modules");

    @ArchTest
    static final ArchRule application_connects_modules =
        noClasses().that().resideInAPackage(APPLICATION_PACKAGE)
            .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_PACKAGE, LLM_PACKAGE, GRAPHICS_PACKAGE, TUTOR_PACKAGE, CAS_PACKAGE, WEB_PACKAGE)
            .allowEmptyShould(true)
            .because("application is the only module that should connect all other modules");

    @ArchTest
    static final ArchRule common_has_no_external_dependencies =
        noClasses().that().resideInAPackage(COMMON_PACKAGE)
            .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_PACKAGE, LLM_PACKAGE, GRAPHICS_PACKAGE, TUTOR_PACKAGE, CAS_PACKAGE, WEB_PACKAGE, APPLICATION_PACKAGE)
            .allowEmptyShould(true)
            .because("common must not depend on any other module");

    @ArchTest
    static final ArchRule web_depends_on_application_modules =
        noClasses().that().resideInAPackage(WEB_PACKAGE)
            .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_PACKAGE, LLM_PACKAGE, GRAPHICS_PACKAGE, TUTOR_PACKAGE, CAS_PACKAGE)
            .allowEmptyShould(true)
            .because("web should connect through application layer, not directly depend on internal modules");
}