package io.github.arthursouto.prumo.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

@AnalyzeClasses(packages = "io.github.arthursouto.prumo", importOptions = ImportOption.DoNotIncludeTests.class)
class CodingRulesTest {

    private static final String AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired";
    private static final String SERVICE = "org.springframework.stereotype.Service";
    private static final String COMPONENT = "org.springframework.stereotype.Component";
    private static final String REPOSITORY = "org.springframework.stereotype.Repository";

    @ArchTest
    static final ArchRule FF_COD_01 = noFields()
            .should().beAnnotatedWith(AUTOWIRED)
            .as("FF-COD-01: no field injection, use constructor injection");

    @ArchTest
    static final ArchRule FF_COD_02 = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().beAnnotatedWith(SERVICE)
            .orShould().beAnnotatedWith(COMPONENT)
            .orShould().beAnnotatedWith(REPOSITORY)
            .as("FF-COD-02: domain and application have no @Service, @Component or @Repository");

    @ArchTest
    static final ArchRule FF_COD_03 = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
            .as("FF-COD-03: no System.out or System.err");
}
