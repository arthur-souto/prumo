package io.github.arthursouto.prumo.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "io.github.arthursouto.prumo", importOptions = ImportOption.DoNotIncludeTests.class)
class PersistenceTest {

    private static final String ENTITY = "jakarta.persistence.Entity";
    private static final String SPRING_DATA_REPOSITORY = "org.springframework.data.repository.Repository";

    @ArchTest
    static final ArchRule FF_PER_01 = classes()
            .that().areAnnotatedWith(ENTITY)
            .should().resideInAPackage("..adapter.out.persistence.entity..")
            .as("FF-PER-01: @Entity only in the persistence entity package");

    @ArchTest
    static final ArchRule FF_PER_02 = classes()
            .that().areAssignableTo(SPRING_DATA_REPOSITORY)
            .should().resideInAPackage("..adapter.out.persistence.jpa..")
            .as("FF-PER-02: Spring Data repositories only in the persistence jpa package");
}
