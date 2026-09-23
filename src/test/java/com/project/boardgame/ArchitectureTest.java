package com.project.boardgame;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.project.boardgame", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    public static final ArchRule domain_should_not_depend_on_framework_or_infrastructure =
            noClasses().that().resideInAPackage("com.project.boardgame.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "java.sql..",
                            "..infrastructure.."
                    ).allowEmptyShould(true);

    @ArchTest
    public static final ArchRule application_should_not_depend_on_rule_implementations =
            noClasses().that().resideInAPackage("com.project.boardgame.application..")
                    .should().dependOnClassesThat(
                            resideInAPackage("com.project.boardgame.domain.rules..")
                                    .and(are(not(INTERFACES)))
                    );
}
