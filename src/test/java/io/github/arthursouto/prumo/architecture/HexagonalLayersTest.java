package io.github.arthursouto.prumo.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "io.github.arthursouto.prumo", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalLayersTest {

    @ArchTest
    static final ArchRule FF_LAY_01 = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Adapter").definedBy("..infrastructure..")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
            .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
            .as("FF-LAY-01: dependencies only go adapter -> application -> domain");

    @ArchTest
    static final ArchRule FF_LAY_02 = slices()
            .matching("io.github.arthursouto.prumo.(*)..")
            .should().beFreeOfCycles()
            .as("FF-LAY-02: no cycles between first-level packages");
}
