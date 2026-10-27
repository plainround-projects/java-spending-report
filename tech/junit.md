# JUnit Jupiter (6.1.3)

## 1. What it is

A **test** is a small program that runs a piece of your code and fails if the result is not what it should be. Running them all by hand would be tedious, so a **test framework** finds the tests, runs them one by one, and prints a report of passes and failures. **JUnit** is the standard test framework for Java. "Jupiter" is the name of its programming model: you mark ordinary methods with **annotations** (labels written with `@`, such as `@Test`) and JUnit does the rest.

## 2. Why this repository uses it

The lesson asks for tests that read as behavior and for a build that fails when a test fails. The 57 tests are the safety net: they prove the Done-when lines and keep them true.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | `org.junit.jupiter:junit-jupiter:${junit.version}`, scope `test` |
| `src/test/java/**` | every test class; the folders mirror the packages under `src/main/java` |
| `Makefile` | `make test` runs them through Maven |
| `.github/workflows/ci.yml` | CI runs `make test` |

## 4. How it works underneath

1. **Discovery.** Maven's Surefire plugin starts a new JVM, puts the compiled classes, the test classes and the test libraries on its class path, and asks the JUnit **platform** to run. The platform looks for test **engines** on the class path; the Jupiter engine scans the classes for methods with the annotations listed below. Surefire only considers classes whose names match its patterns (such as `*Test`).
2. **One new object per test.** For each test method the engine creates a new instance of the test class. A field that a test changes therefore starts fresh in the next test; tests cannot leak state into each other through fields.
3. **Order of events for one test:** create the object, run the `@BeforeEach` methods, run the test method, run the `@AfterEach` methods. `@BeforeAll` and `@AfterAll` bracket the whole class and must be `static`.
4. **Outcome.** A test that returns normally passes. An assertion that fails throws `AssertionError`; any other exception is an error. Both are reported with the test's name. A test is not `public`: JUnit 5 and later do not need it.
5. **Reports** go to the console and to `target/surefire-reports/` (a `.txt` and a `.xml` file per class).
6. **The dependency `junit-jupiter`** is an aggregator: one line in the pom that brings in the API for writing tests (`junit-jupiter-api`), the parameterized-test support (`junit-jupiter-params`) and the engine that runs them (`junit-jupiter-engine`). JUnit 6 needs Java 17 or newer; this repository runs on 25.

## 5. Walkthrough of this repository's use

### Every JUnit and ArchUnit annotation used in this repository

| Annotation | Full name | What it does | Used in |
| --- | --- | --- | --- |
| `@Test` | `org.junit.jupiter.api.Test` | marks a method as a test. JUnit runs it; if it returns normally the test passes, if it throws (an assertion error or any exception) the test fails. | `BuildSpendingReportTest.java`, `CsvFileRowSourceTest.java`, `CsvRowParserEdgeCasesTest.java`, `CsvRowParserTest.java`, `LessonScenarioTest.java`, `SettingsTest.java` ... |
| `@ParameterizedTest` | `org.junit.jupiter.params.ParameterizedTest` | marks a method as a test that is run many times, once per set of arguments supplied by a source annotation. It replaces `@Test` on that method. | `CsvRowParserEdgeCasesTest.java` |
| `@CsvSource` | `org.junit.jupiter.params.provider.CsvSource` | supplies the arguments as rows of comma-separated text. Here `delimiter = '|'` changes the separator to a pipe (the test inputs themselves contain commas) and `value = {...}` lists the rows. Each row's pieces are converted to the method's parameter types (here both `String`). | `CsvRowParserEdgeCasesTest.java` |
| `@TempDir` | `org.junit.jupiter.api.io.TempDir` | on a field (or parameter) of type `Path` makes JUnit create a fresh temporary folder for each test and delete it afterwards. | `CsvFileRowSourceTest.java`, `SpendCliTest.java` |
| `@AnalyzeClasses` | `com.tngtech.archunit.junit.AnalyzeClasses` | ArchUnit's class-level annotation: says which packages of compiled classes the architecture rules examine, and with which import options. | `ArchitectureTest.java` |
| `@ArchTest` | `com.tngtech.archunit.junit.ArchTest` | marks a field of type `ArchRule` as a test: ArchUnit's JUnit integration runs each such rule against the analyzed classes. | `ArchitectureTest.java` |

### Every test file

| Test file | Tests in it |
| --- | --- |
| `src/test/java/com/plainround/spend/ArchitectureTest.java` | 3 `@ArchTest` rules |
| `src/test/java/com/plainround/spend/application/BuildSpendingReportTest.java` | 1 `@Test` |
| `src/test/java/com/plainround/spend/domain/SpendingAccumulatorTest.java` | 8 `@Test` |
| `src/test/java/com/plainround/spend/domain/TransactionTest.java` | 7 `@Test` |
| `src/test/java/com/plainround/spend/infrastructure/SettingsTest.java` | 4 `@Test` |
| `src/test/java/com/plainround/spend/infrastructure/csv/CsvFileRowSourceTest.java` | 5 `@Test` |
| `src/test/java/com/plainround/spend/infrastructure/csv/CsvRowParserEdgeCasesTest.java` | 5 `@Test`, 1 `@ParameterizedTest` |
| `src/test/java/com/plainround/spend/infrastructure/csv/CsvRowParserTest.java` | 5 `@Test` |
| `src/test/java/com/plainround/spend/interfaces/cli/LessonScenarioTest.java` | 2 `@Test` |
| `src/test/java/com/plainround/spend/interfaces/cli/SpendCliTest.java` | 10 `@Test` |

Every test method of every file is explained, line by line, in `java.md` (Part B of its walkthrough). The way JUnit runs a **parameterized** test deserves a sentence: `@ParameterizedTest` plus a source annotation makes one method behave like many tests; each row is reported as its own test, so one bad row shows which input failed.

## 6. Try it yourself

- `./mvnw -B test | grep 'Tests run'`. Expected: one `Tests run: N, Failures: 0, Errors: 0, Skipped: 0` line per class and a last total line of 57.
- Run one class: `./mvnw -q -B test -Dtest=CsvRowParserTest -Djacoco.skip=true`. Expected: no output on success.
- Make a test fail on purpose in a scratch copy (change an expected value). Expected: `[ERROR] Failures:` and the test name with `expected: ... but was: ...`.
- `ls target/surefire-reports | head`. Expected: one `.txt` and one `.xml` file per test class.

## 7. Mistakes people make

- Test methods that depend on each other or on order. Each test here builds its own data.
- Sharing a mutable field between tests. JUnit makes a new instance per test, and the tests here also avoid static mutable state.
- Sleeping to wait for something. Where timing matters, tests here wait on a condition (a latch, a future with a timeout) or use generous limits, never a fixed sleep as the only check.
- Writing a test with no assertion. A test that cannot fail proves nothing.

## 8. Check yourself

1. How does JUnit decide which methods are tests?
   **Answer:** Methods marked `@Test` (or `@ParameterizedTest`) in classes found on the test class path.
2. What does a new instance per test mean for fields?
   **Answer:** Every test starts with fresh field values, so tests cannot affect each other through fields.
3. What is `@TempDir` for?
   **Answer:** It gives a fresh temporary folder that is deleted after the test.
4. How is a parameterized test different?
   **Answer:** One method runs many times, once per row from a source annotation such as `@CsvSource`, and each row is reported separately.
5. What does `junit-jupiter` bring in?
   **Answer:** The API, the parameterized-test support and the engine.

## 9. Further reading

- JUnit 6 user guide: https://docs.junit.org/current/user-guide/
- Parameterized tests: https://docs.junit.org/current/user-guide/#writing-tests-parameterized-tests
- Maven Surefire 3.6.1: https://maven.apache.org/surefire/maven-surefire-plugin/
