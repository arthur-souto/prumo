package io.github.arthursouto.prumo.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.github.arthursouto.prumo", importOptions = ImportOption.DoNotIncludeTests.class)
class PortsTest {

    @ArchTest
    static final ArchRule FF_PRT_01 = classes()
            .that().resideInAPackage("..port.in")
            .or().resideInAPackage("..port.out")
            .should().beInterfaces()
            .as("FF-PRT-01: types directly in port.in and port.out are interfaces");

    @ArchTest
    static final ArchRule FF_PRT_02 = noClasses()
            .that().resideOutsideOfPackage("..application.usecase..")
            .and().areNotInterfaces()
            .should().implement(JavaClass.Predicates.resideInAPackage("..port.in.."))
            .as("FF-PRT-02: only classes in application.usecase implement input ports");
}
