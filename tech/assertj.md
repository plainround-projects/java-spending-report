# AssertJ (3.27.7)

## 1. What it is

AssertJ is a library of **assertions**: statements that check a result in a test. Its style is a fluent chain, `assertThat(value).isEqualTo(expected)`, where each step returns an object for the next. Error messages name both the expected and the actual value.

## 2. Why this repository uses it

The repository rule says test names read as behavior. AssertJ makes the body read the same way, and its failure messages explain the failure without a debugger.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | `org.assertj:assertj-core:${assertj.version}`, scope `test` |
| `src/test/java/**` | almost every test imports `org.assertj.core.api.Assertions.assertThat` |

## 4. How it works underneath

- AssertJ is plain Java classes; there is no agent and no magic. `assertThat` is a family of overloaded static methods; the compiler picks the one that matches the type of its argument.
- A failed assertion throws `AssertionError`. JUnit sees the exception and marks the test as failed.
- The assertion objects are immutable-style builders: each call checks something and returns the same object so you can continue.

## 5. Walkthrough of this repository's use

Every assertion in this repository starts with a static import of `assertThat`, so tests read from left to right:

```java
import static org.assertj.core.api.Assertions.assertThat;
```

```java
  @Test
  void aGoodRowBecomesATransactionWithTheExactAmount() {
    ParsedRow result = parser.parse(2, "2026-01-03,Corner Grocer,42.17,USD,Groceries");

    assertThat(result)
        .isInstanceOfSatisfying(
            ParsedRow.Ok.class,
            ok -> {
              assertThat(ok.transaction().amount()).isEqualTo(new BigDecimal("42.17"));
              assertThat(ok.transaction().category()).isEqualTo("Groceries");
            });
  }
```

`assertThat(actual)` wraps the value and returns an assertion object whose methods depend on the value's type. A `String` gets `startsWith`, `contains`, `isEqualTo`; a collection gets `hasSize`, `containsExactly`; a `Duration` gets `isLessThan`; an `Optional` gets `hasValue`. A failed check throws an `AssertionError` with a message that shows both values.

**Chaining.** Each assertion method returns the assertion object, so conditions chain: `assertThat(text).contains("a").doesNotContain("b")`. **Exceptions** are checked with `assertThatThrownBy`:

```java
  @Test
  void aNegativeAmountIsRejectedWithAMessageNamingTheField() {
    assertThatThrownBy(() -> new Transaction(DAY, "Ride", new BigDecimal("-8.00"), USD, "Travel"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("amount must not be null or negative: -8.00");
  }
```

`assertThatThrownBy(() -> code)` runs the lambda, expects it to throw, and gives assertions on the exception (`isInstanceOf`, `hasMessage`, `hasMessageContaining`). If nothing is thrown, the test fails.

Some tests here compare money with `isEqualByComparingTo("0.30")` rather than `isEqualTo`: for `BigDecimal`, `equals` also compares the scale (`0.30` is not equal to `0.3`), while `compareTo` compares the value only.

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
