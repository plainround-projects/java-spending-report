package com.plainround.spend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** The dependency rule from the README's "Project structure", enforced on every test run. */
@AnalyzeClasses(
    packages = "com.plainround.spend",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String DOMAIN = "com.plainround.spend.domain..";
  private static final String APPLICATION = "com.plainround.spend.application..";
  private static final String INFRASTRUCTURE = "com.plainround.spend.infrastructure..";
  private static final String INTERFACES = "com.plainround.spend.interfaces..";

  @ArchTest
  static final ArchRule layersOnlyDependDownwards =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("domain")
          .definedBy(DOMAIN)
          .layer("application")
          .definedBy(APPLICATION)
          .layer("infrastructure")
          .definedBy(INFRASTRUCTURE)
          .layer("interfaces")
          .definedBy(INTERFACES)
          .whereLayer("interfaces")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("infrastructure")
          .mayOnlyBeAccessedByLayers("interfaces")
          .whereLayer("application")
          .mayOnlyBeAccessedByLayers("infrastructure", "interfaces")
          .whereLayer("domain")
          .mayOnlyBeAccessedByLayers("application", "infrastructure", "interfaces");

  @ArchTest
  static final ArchRule domainUsesOnlyTheJdk =
      noClasses()
          .that()
          .resideInAPackage(DOMAIN)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(APPLICATION, INFRASTRUCTURE, INTERFACES, "org.slf4j..", "ch.qos..");

  @ArchTest
  static final ArchRule applicationDoesNotKnowTheFileFormat =
      noClasses()
          .that()
          .resideInAPackage(APPLICATION)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("java.nio.file..", "java.io..");
}
