# JUnit Jupiter (6.1.3)

## 1. What it is

JUnit is the standard test framework for Java. A **test** is a method that runs a small piece of your code and fails if the result is wrong. JUnit finds the tests, runs them one by one, and reports passes and failures. "Jupiter" is the name of the programming model (annotations such as `@Test`) in JUnit 5 and 6.

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

- Maven's Surefire plugin starts a new JVM, puts the compiled classes, the test classes and the test libraries on its class path, and asks the JUnit **platform** to run.
- The platform looks for test **engines** on the class path. The Jupiter engine scans the classes for `@Test` and friends.
- For each test method the engine creates a **new instance of the test class** (so tests cannot leak state into each other through fields), runs the `@BeforeEach` methods, the test, then the `@AfterEach` methods.
- Results go to the console and to `target/surefire-reports/`.

## 5. Walkthrough of this repository's use

Every test class in this repository is plain Java in `src/test/java`, in the same package as the code it tests. Surefire (see `maven.md`) finds classes whose names end in `Test` and runs every method marked `@Test`. Here is one test, then the other things JUnit gives us.

```java
  @Test
  void aQuotedAmountWithAThousandsCommaStaysOneField() {
    ParsedRow result = parser.parse(4, "2026-01-05,Rail Pass,\"1,204.50\",USD,Travel");

    assertThat(result)
        .isInstanceOfSatisfying(
            ParsedRow.Ok.class,
            ok -> assertThat(ok.transaction().amount()).isEqualTo(new BigDecimal("1204.50")));
  }
```

`@Test` marks a test method. The method name is a sentence that says the behavior (the house style). A test passes if the method returns without throwing; a failed assertion throws an `AssertionError`. The class and its methods are not `public`: JUnit 5 and later do not need it.

**Parameterized tests** run the same method with many inputs:

```java
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "2026-01-11,Ride Share,-8.00,USD,Travel|amount must not be null or negative: -8.00",
        "2026-01-14,Streaming Plan,15.99,USD,|category must not be blank",
        "2026-01-18,Hardware Store,sixty,USD,Home|amount is not a number: 'sixty'",
        "2026-02-05,Rail Pass,1,204.50,USD,Travel|expected 5 columns, found 6",
        "2026-13-10,Pharmacy,16.15,USD,Health|date must look like 2026-01-31, got '2026-13-10'",
        "2026-02-25,Storage Unit,45.00,US$,Home|unknown currency code 'US$'",
        "2026-02-27,Corner Grocer,,USD,Groceries|amount is not a number: ''",
      })
  void everyPlantedBadRowOfTheLessonGetsAnActionableReason(String line, String reason) {
    assertThat(reasonFor(line)).isEqualTo(reason);
  }
```

`@ParameterizedTest` replaces `@Test`. A **source** annotation (`@CsvSource`, `@ValueSource`) supplies the arguments, and JUnit calls the method once per row, converting the text to the parameter types. Each call is reported as its own test, so one bad row shows which input failed.

**Set-up and clean-up.** `@TempDir` asks JUnit for a fresh empty folder before each test and deletes it afterwards. The test below writes its input file there, so tests never touch real files and never leave anything behind.

```java
  @TempDir Path folder;
```

```java
  @Test
  void aMissingFileIsReportedAsAnUnreadableInputNotAStackTrace() {
    assertThatThrownBy(() -> read(folder.resolve("nope.csv")))
        .isInstanceOf(InputUnreadableException.class)
        .hasMessageContaining("cannot read")
        .hasMessageContaining("NoSuchFileException");
  }
```

The `junit-jupiter` dependency in the pom (scope `test`) is an **aggregator**: one line that brings in the API for writing tests (`junit-jupiter-api`), the parameterized-test support (`junit-jupiter-params`) and the engine that runs them (`junit-jupiter-engine`). JUnit 6 needs Java 17 or newer; this repository runs on 25.

## 6. Try it yourself

- `./mvnw -B test | grep 'Tests run'`. Expected: one `Tests run: N, Failures: 0, Errors: 0, Skipped: 0` line per class and a last total line of 57.
- Run one class: `./mvnw -q -B test -Dtest=CsvRowParserTest -Djacoco.skip=true`. Expected: no output on success.
- Make a test fail on purpose in a scratch copy (change an expected value). Expected: `[ERROR] Failures:` and the test name with `expected: ... but was: ...`.
- `ls target/surefire-reports | head`. Expected: one `.txt` and one `.xml` file per test class.

## 7. Mistakes people make

- Test methods that depend on each other or on order. Each test here builds its own data.
- Sharing a mutable field between tests. JUnit makes a new instance per test, and the tests here also avoid static mutable state.
- Sleeping to wait for something. Where timing matters, tests here wait on a condition or use generous limits, never a fixed sleep as the only check.
- Writing a test with no assertion. A test that cannot fail proves nothing.

## 8. Check yourself

1. How does JUnit decide which methods are tests?
   **Answer:** Methods marked `@Test` (or `@ParameterizedTest`) in classes found on the test class path.
2. What does a new instance per test mean for fields?
   **Answer:** Every test starts with fresh field values, so tests cannot affect each other through fields.
3. What is `@TempDir` / `@BeforeEach` for (see the walkthrough)?
   **Answer:** `@TempDir` gives a fresh temporary folder that is deleted after the test; `@BeforeEach` runs set-up code before every test.
4. How is a parameterized test different?
   **Answer:** One method runs many times, once per row from a source annotation such as `@CsvSource`, and each row is reported separately.
5. What does `junit-jupiter` bring in?
   **Answer:** The API, the parameterized-test support and the engine.

## 9. Further reading

- JUnit 6 user guide: https://docs.junit.org/current/user-guide/
- Parameterized tests: https://docs.junit.org/current/user-guide/#writing-tests-parameterized-tests
- Maven Surefire 3.6.1: https://maven.apache.org/surefire/maven-surefire-plugin/
