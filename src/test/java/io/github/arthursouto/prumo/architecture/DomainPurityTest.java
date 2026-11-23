package io.github.arthursouto.prumo.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.github.arthursouto.prumo", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainPurityTest {

    @ArchTest
    static final ArchRule FF_DOM_01 = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "lombok..",
                    "com.fasterxml.jackson..")
            .as("FF-DOM-01: the domain depends on no framework (Spring, JPA, Hibernate, Lombok, Jackson)");
}
