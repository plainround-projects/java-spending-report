# AssertJ (3.27.7)

## 1. What it is

An **assertion** is a statement in a test that says what must be true: "this total equals 12.50". If it is not true, the test fails. AssertJ is a library of assertions with a *fluent* style: `assertThat(value).isEqualTo(expected)` reads left to right like a sentence, and each step returns an object so another check can follow. When one fails, the message shows both the expected and the actual value.

## 2. Why this repository uses it

The repository rule says test names read as behavior. AssertJ makes the body read the same way, and its failure messages explain a failure without a debugger.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | `org.assertj:assertj-core:${assertj.version}`, scope `test` |
| `src/test/java/**` | almost every test imports `org.assertj.core.api.Assertions.assertThat` and often `assertThatThrownBy` |

## 4. How it works underneath

1. `assertThat(actual)` is one of many overloaded static methods. The compiler picks the one that matches the type of its argument, and each returns a different assertion object with methods that make sense for that type: a `String` gets `startsWith`, a collection gets `hasSize`, an `Optional` gets `hasValue`.
2. Each check method tests something. If it holds, it returns the same assertion object so that a further check can be chained. If not, it throws an `AssertionError` whose text begins `Expecting actual:` and shows the values; JUnit sees the error and marks the test failed.
3. `assertThatThrownBy(() -> code)` runs the code inside the lambda and expects it to throw; the checks that follow apply to the thrown exception. If nothing is thrown, the test fails.
4. A bare `assertThat(x)` with no check checks nothing. A chain must end with a check method.

## 5. Walkthrough of this repository's use

### Every assertion method used in this repository

These are all the methods that appear in a chain after `assertThat(...)` or `assertThatThrownBy(...)` in `src/test/java`. Their use in each test is explained in `java.md`.

| Method | What it checks | Used in |
| --- | --- | --- |
| `contains` | a text contains each given piece, or a collection contains each given element (in any position). | `LessonScenarioTest.java`, `SpendCliTest.java` |
| `containsExactly` | a collection holds exactly the given elements in exactly that order. | `CsvRowParserEdgeCasesTest.java`, `SpendingAccumulatorTest.java` |
| `containsPattern` | a text contains a match for the given regular expression. | `SpendCliTest.java` |
| `doesNotContain` | a text or collection contains none of the given pieces. | `SpendCliTest.java` |
| `extracting` | turns each element of a collection into one of its properties (given as a method reference such as `MonthlyTotals::month`), so the next check applies to those values. | `SpendingAccumulatorTest.java` |
| `hasMessage` | an exception's message equals the given text exactly. | `TransactionTest.java` |
| `hasMessageContaining` | an exception's message contains the given text. | `CsvFileRowSourceTest.java`, `SettingsTest.java` |
| `hasMessageStartingWith` | an exception's message begins with the given text. | `TransactionTest.java` |
| `hasSize` | a collection has exactly that many elements. | `CsvFileRowSourceTest.java` |
| `isEmpty` | a collection, text, `Optional` or map has nothing in it. | `SpendingAccumulatorTest.java` |
| `isEqualByComparingTo` | compares two `BigDecimal` (or other `Comparable`) values by numeric value, so `0.30` equals `0.3`; `isEqualTo` would treat different scales as different. | `SpendingAccumulatorTest.java`, `TransactionTest.java` |
| `isEqualTo` | the actual value equals the expected one (`equals`). For a record that means all components are equal. | `BuildSpendingReportTest.java`, `CsvFileRowSourceTest.java`, `CsvRowParserEdgeCasesTest.java`, `CsvRowParserTest.java`, `LessonScenarioTest.java` ... |
| `isFalse` | a boolean is `false`. | `SpendingAccumulatorTest.java` |
| `isInstanceOf` | the value is an object of the given class (or a subclass). After `assertThatThrownBy` it checks the type of the exception. | `CsvFileRowSourceTest.java`, `CsvRowParserEdgeCasesTest.java`, `SpendingAccumulatorTest.java`, `TransactionTest.java` |
| `isInstanceOfSatisfying` | the value is of the given class, and the given check (a lambda taking the value as that type) passes. | `CsvFileRowSourceTest.java`, `CsvRowParserTest.java` |
| `isTrue` | a boolean is `true`. | `SpendingAccumulatorTest.java` |
| `isZero` | a number is 0. | `SettingsTest.java`, `SpendCliTest.java`, `SpendingAccumulatorTest.java` |
| `singleElement` | the collection has exactly one element, and the next check applies to it. | `CsvFileRowSourceTest.java` |
| `startsWith` | a text begins with the given text. | `SpendCliTest.java` |

Money is compared with `isEqualByComparingTo("0.30")` rather than `isEqualTo`: for `BigDecimal`, `equals` also compares the scale (`0.30` is not equal to `0.3`), while `compareTo` compares the value only.

## 6. Try it yourself

- `./mvnw -q -B test -Dtest=TransactionTest -Djacoco.skip=true`. Expected: no output (all assertions hold).
- In a scratch copy change an expected value in that test and rerun. Expected: a message beginning `Expecting actual:` with both values, and the failing line number.
- `grep -rn 'assertThat' src/test/java | wc -l`. Expected: a few hundred lines; assertions are everywhere.

## 7. Mistakes people make

- Writing `assertThat(x == y).isTrue()`. A failure then only says `expected true`. Write `assertThat(x).isEqualTo(y)` and the message shows both values.
- Forgetting to finish a chain: `assertThat(x)` alone checks nothing. Always end with a check method.
- Comparing `BigDecimal` with `isEqualTo` when only the value matters; use `isEqualByComparingTo`.
- Catching an exception by hand with try/catch in a test. `assertThatThrownBy` fails the test if nothing is thrown.

## 8. Check yourself

1. What does `assertThat(5)` return?
   **Answer:** An assertion object for an integer, with methods such as `isEqualTo` and `isPositive`.
2. How do you check that code throws?
   **Answer:** `assertThatThrownBy(() -> code).isInstanceOf(...).hasMessage(...)`.
3. What happens when an assertion fails?
   **Answer:** An `AssertionError` is thrown and JUnit reports the test as failed with AssertJ's message.
4. Why is a bare `assertThat(x)` useless?
   **Answer:** It only wraps the value; no check runs until you call a check method.
5. Which scope has AssertJ in the pom?
   **Answer:** `test`.

## 9. Further reading

- AssertJ core 3.27.7: https://assertj.github.io/doc/
- Javadoc of `Assertions`: https://www.javadoc.io/doc/org.assertj/assertj-core/3.27.7/org/assertj/core/api/Assertions.html
