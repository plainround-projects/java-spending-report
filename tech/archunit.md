# ArchUnit (1.5.1)

## 1. What it is

ArchUnit is a library that checks the **structure** of your code in a unit test. It reads the compiled `.class` files, builds a model of which class uses which, and lets you write rules such as "classes in `domain` may not use classes in `infrastructure`". A broken rule fails the test like any other.

## 2. Why this repository uses it

The repository's design is layered: the domain depends on nothing, the application on the domain, the infrastructure and interfaces on both, and nothing on interfaces. A rule written in a README is only a wish; ArchUnit makes it a test that runs on every `make test`.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | `com.tngtech.archunit:archunit-junit5`, scope `test` |
| `src/test/java/com/plainround/spend/ArchitectureTest.java` | the rules |
| `README.md`, `ARCHITECTURE.md` | describe the layers that the test enforces |

## 4. How it works underneath

- ArchUnit uses a library that reads bytecode (the `.class` files) without running the program.
- It builds a graph: nodes are classes, edges are "uses" relations (method calls, field types, inheritance, annotations).
- A rule is a query on the graph. `layeredArchitecture()` builds the query from your layer definitions; `noClasses().that()...should()...` builds a custom one.
- If the query finds a violation, the test fails with a list of the offending classes and lines.

## 5. Walkthrough of this repository's use

```java
@AnalyzeClasses(
    packages = "com.plainround.spend",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
```

`@AnalyzeClasses` tells ArchUnit which compiled classes to read: everything under `com.plainround.spend`. `ImportOption.DoNotIncludeTests` leaves the test classes out, so the rules judge production code only. The class has no `@Test` methods: ArchUnit's JUnit 5 integration (the dependency `archunit-junit5`) runs every `static final ArchRule` field marked `@ArchTest` as a test.

```java
  private static final String DOMAIN = "com.plainround.spend.domain..";
  private static final String APPLICATION = "com.plainround.spend.application..";
  private static final String INFRASTRUCTURE = "com.plainround.spend.infrastructure..";
  private static final String INTERFACES = "com.plainround.spend.interfaces..";
```

These are **package patterns**. `..` at the end means "this package and all packages below it".

```java
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
```

`layeredArchitecture()` is a ready-made rule type. `consideringOnlyDependenciesInLayers()` ignores dependencies on anything outside the four layers (the JDK, libraries). Each `.layer(name).definedBy(pattern)` names a layer. Each `whereLayer(name)...` line limits who may use it: `interfaces` may not be used by any layer; `infrastructure` only by `interfaces`; `application` only by `infrastructure` and `interfaces`; `domain` by all three above it. Read together: the arrows only point towards the domain.

The next rules add specific bans (the exact packages differ per project; see the file). For this project:

```java
      noClasses()
          .that()
          .resideInAPackage(DOMAIN)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(APPLICATION, INFRASTRUCTURE, INTERFACES, "org.slf4j..", "ch.qos..");
```

## 6. Try it yourself

- `./mvnw -q -B test -Dtest=ArchitectureTest -Djacoco.skip=true`. Expected: no output on success (add `-B` without `-q` to see `Tests run: ...`).
- Break a rule in a scratch copy: add `import com.plainround.spend.infrastructure.Settings;` (or any infrastructure class) to a class in `domain`, compile, and rerun. Expected: `Architecture Violation` naming the class and the rule.
- Read the rule list: `grep -n 'ArchTest' -A3 src/test/java/com/plainround/spend/ArchitectureTest.java`.

## 7. Mistakes people make

- Forgetting `DoNotIncludeTests`: test classes legitimately use everything, and would break the rules.
- A typo in a package pattern. In ArchUnit 1.x a rule whose subject set is empty fails by default, so the typo shows up as a failing test and not as a rule that quietly checks nothing; the patterns are kept in constants so each is typed once.
- Using the rule to replace review: it catches direction mistakes, not design quality.
- Mixing up `..` and `.`: `a..` is the package and all below, `a.*` is one level.

## 8. Check yourself

1. What does ArchUnit read: source code or compiled classes?
   **Answer:** Compiled `.class` files.
2. What does `domain..` match?
   **Answer:** The `domain` package and every package below it.
3. Which layer may use `interfaces`?
   **Answer:** None.
4. Why `DoNotIncludeTests`?
   **Answer:** So the rules judge only production code.
5. What makes a `static final ArchRule` run as a test?
   **Answer:** The `@ArchTest` annotation with the `archunit-junit5` integration.

## 9. Further reading

- ArchUnit 1.5.1 user guide: https://www.archunit.org/userguide/html/000_Index.html
- Layered architecture rules: https://www.archunit.org/userguide/html/000_Index.html#_layered_architecture
