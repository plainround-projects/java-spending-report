# ArchUnit (1.5.1)

## 1. What it is

Good code has a **structure**: some parts may use others, but not the other way round. In a README this is only a wish; a person can forget it in a hurry. **ArchUnit** is a library that turns the wish into a test. It reads the compiled `.class` files, builds a model of which class uses which, and lets you write rules such as "classes in `domain` may not use classes in `infrastructure`". A broken rule fails the test like any other test.

## 2. Why this repository uses it

The repository's design is layered: the domain depends on nothing, the application on the domain, the infrastructure and interfaces on both, and nothing on interfaces. ArchUnit makes this a test that runs on every `make test`.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | `com.tngtech.archunit:archunit-junit5`, scope `test` |
| `src/test/java/com/plainround/spend/ArchitectureTest.java` | the rules |
| `README.md`, `ARCHITECTURE.md` | describe the layers that the test enforces |

## 4. How it works underneath

1. ArchUnit reads bytecode (the `.class` files) without running the program.
2. It builds a graph: nodes are classes, edges are "uses" relations (method calls, field types, inheritance, annotations).
3. A rule is a query on the graph. `layeredArchitecture()` builds the query from your layer definitions; `noClasses().that()...should()...` builds a custom one. The statements read almost like English.
4. If the query finds a violation, the test fails with a list of the offending classes and lines. The JUnit 5 integration (the dependency `archunit-junit5`) treats every `static final ArchRule` field marked `@ArchTest` as a test, so the class has no `@Test` methods.
5. Package patterns end in `..` to mean "this package and every package below it"; a single `.*` means one level only.

## 5. Walkthrough of this repository's use

### Every ArchUnit method used in this repository

| Method | What it does |
| --- | --- |
| `consideringOnlyDependenciesInLayers` | ignores dependencies on classes that belong to no declared layer (the JDK, libraries). |
| `definedBy` | says which packages form the layer just declared. |
| `dependOnClassesThat` | classes that use (call, extend, have a field of) the described classes. |
| `layer` | declares a layer by name. |
| `mayNotBeAccessedByAnyLayer` | no layer may use the layer. |
| `mayOnlyBeAccessedByLayers` | only the listed layers may use the layer. |
| `resideInAPackage` | (in `that()`) classes in this package pattern; (in `dependOnClassesThat()`) classes in it as the *dependency*. |
| `resideInAnyPackage` | classes in any of the given package patterns. |
| `should` | ends the subject and begins the condition. |
| `that` | narrows the kind of class the rule is about. |
| `whereLayer` | starts an access rule for a named layer. |

Each rule of `ArchitectureTest.java` is quoted and explained in `java.md` (Part B of its walkthrough).

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
