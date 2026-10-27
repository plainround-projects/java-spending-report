# Java 25

## 1. What it is

Java is a general-purpose programming language that runs on the **Java Virtual Machine (JVM)**. You write source files (`.java`), the compiler turns them into bytecode (`.class`), and the JVM runs the bytecode. Java is statically typed (the compiler checks types), object-oriented (code is organized in classes) and has a large standard library. Version 25 is a long-term-support release (September 2025).

## 2. Why this repository uses it

The lesson is a Java lesson, and the repository is written the way a production Java 25 project is: records for data, sealed types and pattern matching where they fit, exceptions for the file level and values for the row level, immutable objects, interfaces at the boundaries, and only the standard library for the logic.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `src/main/java/**` (15 files) | the program, in four layers: `domain`, `application`, `infrastructure`, `interfaces` |
| `src/test/java/**` (10 files) | the tests, in the same packages |
| `pom.xml` | `<java.release>25</java.release>`: compile for Java 25 |
| `Dockerfile` | builds with a JDK 25 image and runs on a JRE 25 image |

## 4. How it works underneath

- Java source files end in `.java`. The compiler `javac` turns each one into `.class` files holding **bytecode**, instructions for the Java Virtual Machine (JVM), not for a particular processor.
- The `java` command starts a JVM process. The JVM loads classes from the **class path** (folders and jars), runs the `main` method of the class you name, and keeps running until `main` returns and all non-daemon threads are finished (or `System.exit` is called).
- A **package** (`com.plainround...`) is a namespace; it matches folders under `src/main/java`. `import` names classes from other packages.
- Memory: objects live on the **heap**, which a garbage collector cleans. Each running thread has a **stack** for method calls. Primitive values (`int`, `long`, `boolean`) are not objects.
- Java is statically typed: every variable has a type the compiler checks. Generics (`List<String>`) let a type be a parameter.

## 5. Walkthrough of this repository's use

### Classes, fields, constructors, `final`

```java
public final class BuildSpendingReport {

  private final RowSource source;
  private final Currency currency;
  private final int largestCount;
  private final int maxRejectedKept;

  /**
   * Creates the use case.
   *
   * @param source where the rows come from
   * @param currency the report currency
   * @param largestCount how many largest purchases to list
   * @param maxRejectedKept how many rejected rows to keep for display
   */
  public BuildSpendingReport(
      RowSource source, Currency currency, int largestCount, int maxRejectedKept) {
    this.source = source;
    this.currency = currency;
    this.largestCount = largestCount;
    this.maxRejectedKept = maxRejectedKept;
  }
```

A **class** bundles data (fields) and behavior (methods). `final class` cannot be extended. `private final` fields are set once in the **constructor** (the method with the class name, no return type) and never change: the object is immutable after it is built. `this.x = x` assigns the constructor parameter to the field of the same name. This class takes its collaborators as constructor arguments (**dependency injection**), so a test can pass a fake `RowSource`.

### Records and compact constructors

```java
public record Transaction(
    LocalDate date, String description, BigDecimal amount, Currency currency, String category) {
```

```java
  /** Rejects missing or invalid fields, naming the field first in each message. */
  public Transaction {
    if (date == null) {
      throw new IllegalArgumentException("date must not be null");
    }
    if (amount == null || amount.signum() < 0) {
      throw new IllegalArgumentException("amount must not be null or negative: " + amount);
    }
    if (currency == null) {
      throw new IllegalArgumentException("currency must not be null");
    }
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("category must not be blank");
    }
  }
```

A **record** (`public record Transaction(LocalDate date, ..., String category)`) declares a class whose data is exactly its components. Java writes the constructor, accessors (`date()`, `amount()`), `equals`, `hashCode` and `toString` for you, and the fields are `final`. The block `public Transaction { ... }` is a **compact constructor**: it has no parameter list, runs before the fields are assigned, and is where validation goes. `IllegalArgumentException` is a standard exception for a bad argument; the message starts with the field name. `amount.signum()` returns -1, 0 or 1, so `< 0` means negative; the `null` check comes first, because calling a method on `null` would throw `NullPointerException`.

### Sealed interfaces, nested records and `switch` patterns

```java
public sealed interface ParsedRow permits ParsedRow.Ok, ParsedRow.Rejected {

  /**
   * A row that became a valid transaction.
   *
   * @param transaction the parsed transaction
   */
  record Ok(Transaction transaction) implements ParsedRow {}

  /**
   * A row that could not be used.
   *
   * @param lineNumber the line in the file, counting the header as line 1
   * @param line the raw text of the line
   * @param reason what is wrong, written for the person who has to fix the file
   */
  record Rejected(int lineNumber, String line, String reason) implements ParsedRow {}
}
```

```java
  /** Adds one parsed row to the running totals. */
  public void accept(ParsedRow row) {
    switch (row) {
      case ParsedRow.Ok ok -> add(ok.transaction());
      case ParsedRow.Rejected bad -> reject(bad);
    }
  }
```

`sealed interface ParsedRow permits ParsedRow.Ok, ParsedRow.Rejected` means exactly these two types may implement it, and no others. The two records are declared inside the interface. The `switch` in `accept` is a **pattern-matching switch**: `case ParsedRow.Ok ok ->` tests the type and, if it matches, gives the variable `ok` of that type. Because the interface is sealed, the compiler knows the two cases cover everything, so there is no `default`; adding a third type would make this code fail to compile until it is handled. This is how a bad row is **data** and not an exception.

### Interfaces as ports, lambdas and method references

```java
public interface RowSource {

  /**
   * Reads every data row in file order and hands each to the consumer as it is parsed.
   *
   * @param consumer receives one {@link ParsedRow} per non-blank data line
   * @throws InputUnreadableException when the source as a whole cannot be read
   */
  void readRows(Consumer<ParsedRow> consumer);
```

```java
    source.readRows(accumulator::accept);
```

An **interface** lists methods without bodies; classes that `implements` it supply them. `RowSource` is the *port*: the use case needs rows and does not care where they come from. `Consumer<ParsedRow>` is a standard interface with one method that takes a value and returns nothing. `accumulator::accept` is a **method reference**, a short way to pass the method as a function. The tests pass a **lambda** (`consumer -> List.of(rows).forEach(consumer)`), an inline function, as a fake `RowSource`.

### Collections, generics and `Comparator`

```java
  private static final Comparator<Transaction> LARGEST_FIRST =
      Comparator.comparing(Transaction::amount)
          .reversed()
          .thenComparing(Transaction::date)
          .thenComparing(Transaction::description);
```

```java
  private final Map<YearMonth, Map<String, BigDecimal>> totals = new TreeMap<>();
  private final PriorityQueue<Transaction> largest = new PriorityQueue<>(LARGEST_FIRST.reversed());
  private final List<ParsedRow.Rejected> rejected = new ArrayList<>();
```

`Map<YearMonth, Map<String, BigDecimal>>` is a map from a month to a map from a category to a sum; the types in `<>` are **generics**. `TreeMap` keeps its keys sorted, so months come out in order. `PriorityQueue` keeps the smallest element (by its comparator) at the head; the accumulator keeps the five largest purchases by throwing away the head whenever there are six. A `Comparator` says how to order two values: `Comparator.comparing(Transaction::amount).reversed().thenComparing(...)` orders by amount, biggest first, and breaks ties by date, then description, so the result is the same on every run.

### Streams

```java
  private static MonthlyTotals month(Map.Entry<YearMonth, Map<String, BigDecimal>> entry) {
    List<CategoryTotal> categories =
        entry.getValue().entrySet().stream()
            .map(category -> new CategoryTotal(category.getKey(), category.getValue()))
            .sorted(
                Comparator.comparing(CategoryTotal::total)
                    .reversed()
                    .thenComparing(CategoryTotal::category))
            .toList();
    BigDecimal sum =
        categories.stream().map(CategoryTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new MonthlyTotals(entry.getKey(), categories, sum);
  }
```

A **stream** is a pipeline over a collection: `entrySet().stream().map(...).sorted(...).toList()` turns each map entry into a `CategoryTotal`, sorts them (largest first, ties by name) and collects an unmodifiable list. `reduce(BigDecimal.ZERO, BigDecimal::add)` folds the list into one sum, starting from zero.

### `BigDecimal`: exact decimal money

```java
  /** Rounds half-even to the currency's minor units, then formats with US grouping everywhere. */
  private String money(BigDecimal amount) {
    BigDecimal rounded = amount.setScale(fractionDigits, RoundingMode.HALF_EVEN);
    return String.format(Locale.US, "%,." + fractionDigits + "f", rounded);
  }
```

```java
        .merge(transaction.category(), transaction.amount(), BigDecimal::add);
```

`double` stores binary fractions, so `0.10 + 0.20` is `0.30000000000000004`. `BigDecimal` stores the decimal digits exactly. `map.merge(key, value, BigDecimal::add)` adds `value` to what is already stored under `key`, or stores it if there is nothing yet. Rounding happens once, at print time: `setScale(fractionDigits, RoundingMode.HALF_EVEN)` rounds to the currency's minor units (two for USD) and breaks ties towards the even digit (banker's rounding). `String.format(Locale.US, "%,.2f", rounded)` prints with US grouping, so the output is the same on every machine.

### Exceptions, catch order and `try`-with-resources

```java
  /**
   * Parses one data line.
   *
   * @param lineNumber the line number in the file, header counted as line 1
   * @param line the raw line
   * @return {@link ParsedRow.Ok} or {@link ParsedRow.Rejected}
   */
  public ParsedRow parse(int lineNumber, String line) {
    List<String> fields = CsvFieldSplitter.split(line);
    if (fields.size() != COLUMNS) {
      return reject(lineNumber, line, "expected 5 columns, found " + fields.size());
    }
    // The order of the catch blocks matters: NumberFormatException is an
    // IllegalArgumentException, and the broadest one must come last.
    try {
      return new ParsedRow.Ok(transaction(fields));
    } catch (DateTimeParseException problem) {
      return reject(
          lineNumber, line, "date must look like 2026-01-31, got '" + fields.get(0) + "'");
    } catch (NumberFormatException problem) {
      return reject(lineNumber, line, "amount is not a number: '" + fields.get(2) + "'");
    } catch (IllegalArgumentException problem) {
      return reject(lineNumber, line, problem.getMessage());
    }
  }
```

```java
  @Override
  public void readRows(Consumer<ParsedRow> consumer) {
    LOG.info("reading file path={}", path);
    // The charset is spelled out: the platform default differs between machines.
    try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
      emit(lines.iterator(), consumer);
    } catch (UncheckedIOException problem) {
      throw unreadable(problem.getCause());
    } catch (IOException problem) {
      throw unreadable(problem);
    }
  }
```

Code that fails **throws** an exception; a `catch` block handles it. When catch blocks are listed, Java uses the first that matches, so the most specific type must come first: `NumberFormatException` is a subclass of `IllegalArgumentException`, and putting the general one first would hide the specific reason (the comment in the code says so). Each `catch` here turns a failure into a `Rejected` value. In `readRows`, `try (Stream<String> lines = Files.lines(...))` is **try-with-resources**: the stream (and the open file) is closed automatically when the block ends, even on error. `UncheckedIOException` is what a lazy stream throws instead of `IOException`; the code unwraps it so the caller gets the real cause.

### A custom exception

```java
public final class InputUnreadableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message what is wrong with the input, written for the person running the tool
   * @param cause the underlying failure
   */
  public InputUnreadableException(String message, Throwable cause) {
    super(message, cause);
  }
```

A named error type makes the failure part of the design: it is thrown when the *file as a whole* cannot be read, as opposed to one bad row. It extends `RuntimeException` (an **unchecked** exception, so callers are not forced to declare it). `serialVersionUID` is a version number Java asks for on serializable classes; the compiler flag `-Xlint:all -Werror` would otherwise fail the build.

### `java.time` and `java.util.regex`

```java
  private Transaction transaction(List<String> fields) {
    LocalDate date = LocalDate.parse(fields.get(0), DateTimeFormatter.ISO_LOCAL_DATE);
    // The String constructor keeps the digits typed; new BigDecimal(double) would not.
    BigDecimal amount = amount(fields.get(2));
    Currency currency = currency(fields.get(3));
    return new Transaction(date, fields.get(1), amount, currency, fields.get(4));
  }
```

```java
  private static final Pattern PLAIN_DECIMAL = Pattern.compile("[+-]?\\d+(\\.\\d+)?");
```

```java
  private static BigDecimal amount(String text) {
    String digits = text.replace(",", "");
    if (!PLAIN_DECIMAL.matcher(digits).matches()) {
      throw new NumberFormatException("not a plain decimal: " + text);
    }
    return new BigDecimal(digits);
  }
```

`LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE)` reads `2026-01-31` and rejects `31/02/2026`; `YearMonth.from(date)` groups dates by month. A **regular expression** (`Pattern`) describes text: `[+-]?\\d+(\\.\\d+)?` means an optional sign, digits, and optionally a dot and more digits (the backslashes are doubled because Java strings use `\\`). It runs first so that an exponent such as `1E+999999999`, which `BigDecimal` would accept, is rejected before it can allocate a huge number.

### `main`, exit codes and `System.exit`

```java
  /**
   * Process entry point.
   *
   * @param args the command-line arguments
   */
  public static void main(String[] args) {
    System.exit(run(List.of(args), System.getenv(), System.out, System.err));
  }
```

```java
  public static final int EXIT_OK = 0;

  /** Exit status when the file as a whole could not be read. */
  public static final int EXIT_UNREADABLE = 1;

  /** Exit status when at least one row was rejected, so a script can notice. */
  public static final int EXIT_REJECTS = 2;

  /** Exit status for wrong command-line usage or configuration. */
  public static final int EXIT_USAGE = 64;
```

The JVM calls `public static void main(String[] args)`. `args` holds the words after the program name. `run(...)` does the work and returns an **exit status**, a number the shell can read: `0` for success, `2` when rows were rejected (so a script can notice), `1` for an unreadable file, `64` for wrong usage. Only `main` calls `System.exit`; the logic is in `run`, so tests can call it and check the number without ending the test JVM.

### Javadoc and annotations

```java
/**
 * One card transaction. Money is an exact {@link BigDecimal}; a zero amount is allowed (a waived
 * fee is still a real row), a negative one is not.
 *
 * @param date the day the purchase was posted
 * @param description the merchant text as exported
 * @param amount the exact amount, never negative
 * @param currency the currency of the amount
 * @param category the spending category, never blank
 */
```

A comment starting `/**` is **Javadoc**, documentation that tools can read; `@param` describes a parameter. `@Override` on a method tells the compiler "this replaces a method of the parent type" and makes it an error if it does not. Checkstyle requires Javadoc on every public type and method.

## 6. Try it yourself

- `java -version`. Expected: `openjdk version "25..."`.
- `./mvnw -q -B clean package -DskipTests -Djacoco.skip=true && javap -cp target/classes com.plainround.spend.domain.* 2>/dev/null | head -20` (or `javap -p -cp target/classes <a domain class>`). Expected: the members of a compiled class; for a record you will see `equals`, `hashCode`, `toString` that you never wrote.
- `find src/main/java -name '*.java' | head -30`. Expected: the file list that matches the layers in the README's project-structure tree.
- `jshell`, then `record P(int x, int y) {}` and `new P(1,2).equals(new P(1,2))`. Expected: `$2 ==> true`.
- `./mvnw -B test | grep 'Tests run:' | tail -1`. Expected: the total of 57 tests with no failures.

## 7. Mistakes people make

- Using `==` to compare strings or objects. `==` compares identity; use `equals`. The Checkstyle rule `StringLiteralEquality` catches the string case.
- Catching a general exception first and hiding the specific one (see the catch-order note in the walkthrough).
- Floating point for money (`double`). Use `BigDecimal` from a `String`.
- Forgetting to close files and connections: use try-with-resources.
- Sharing a mutable object between threads without a lock or a thread-safe type.
- Writing `null` checks everywhere instead of returning `Optional` or an empty value.

## 8. Check yourself

1. What is a record, and what does Java write for you?
   **Answer:** A class for plain data. Java writes the constructor, accessors, `equals`, `hashCode` and `toString` from the components.
2. Why does a sealed interface make a `switch` safer?
   **Answer:** The compiler knows every permitted type, so a switch without `default` must cover all of them, and a new type becomes a compile error until handled.
3. What does `final` on a field mean?
   **Answer:** It is assigned once (in the constructor) and never reassigned.
4. What is the class path?
   **Answer:** The list of folders and jars where the JVM looks for classes.
5. Why is the Java version 25 written in the pom as `release`?
   **Answer:** `<release>25</release>` makes the compiler check against the Java 25 language and library, whatever JDK runs the build.

## 9. Further reading

- Java SE 25 documentation: https://docs.oracle.com/en/java/javase/25/
- The Java Language Specification, Java SE 25: https://docs.oracle.com/javase/specs/jls/se25/html/index.html
- Records: https://docs.oracle.com/en/java/javase/25/language/records.html ; sealed classes: https://docs.oracle.com/en/java/javase/25/language/sealed-classes-and-interfaces.html ; pattern matching for switch: https://docs.oracle.com/en/java/javase/25/language/pattern-matching.html
- Virtual threads: https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html
