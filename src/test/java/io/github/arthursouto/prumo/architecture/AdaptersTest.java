package io.github.arthursouto.prumo.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.github.arthursouto.prumo", importOptions = ImportOption.DoNotIncludeTests.class)
class AdaptersTest {

    private static final String CONTROLLER = "org.springframework.stereotype.Controller";
    private static final String REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";
    private static final String SPRING_DATA_REPOSITORY = "org.springframework.data.repository.Repository";

    @ArchTest
    static final ArchRule FF_ADP_01 = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
            .as("FF-ADP-01: input adapters do not depend on output adapters");

    @ArchTest
    static final ArchRule FF_ADP_02 = classes()
            .that().areAnnotatedWith(REST_CONTROLLER)
            .or().areAnnotatedWith(CONTROLLER)
            .should().resideInAPackage("..adapter.in..")
            .as("FF-ADP-02: controllers live only in adapter.in");

    @ArchTest
    static final ArchRule FF_ADP_03 = noClasses()
            .that().areAnnotatedWith(REST_CONTROLLER)
            .or().areAnnotatedWith(CONTROLLER)
            .should().dependOnClassesThat().resideInAPackage("..application.usecase..")
            .orShould().dependOnClassesThat().areAssignableTo(SPRING_DATA_REPOSITORY)
            .as("FF-ADP-03: controllers talk to port.in, never to use cases or Spring Data repositories");
}
