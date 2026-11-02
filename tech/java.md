# Java 25

## 1. What it is

A computer does exactly what its instructions say, and its processor understands only a very low-level language of numbers. People write programs in a **programming language**, a readable text with strict rules, and a tool translates it for the machine. **Java** is one such language, created in 1995 and still among the most widely used. A Java program is written as plain text in files ending in `.java` (the **source code**). A program called the **compiler** (`javac`) reads the source and checks it for mistakes; if there are none, it writes **bytecode** into files ending in `.class`. Bytecode is not the language of any real processor; it is the language of an imaginary one, the **Java Virtual Machine (JVM)**. The JVM is a real program (started by the `java` command) that reads bytecode and carries it out on whatever computer it runs on. That is why the same compiled program runs on a Mac, on Linux and on Windows. Version 25, used here, is a long-term-support release (September 2025).

Java is **statically typed**: every variable has a type (whole number, text, a date, ...) that the compiler checks before the program ever runs, so a whole class of mistakes cannot reach the user. It is **object-oriented**: code is organized in **classes**, each describing a kind of thing (a `Transaction`, a `Crawler`) with its data and its actions. And it has a very large **standard library**, a collection of ready-made classes for text, dates, files, networking, threads and much more, which comes with the JDK.

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

### From text to a running program

1. **Source.** You write `.java` files. In this repository they live in `src/main/java` (the program) and `src/test/java` (the tests); the folder names under them match the package names (see below).
2. **Compile.** `javac` (called for you by Maven's compiler plugin) turns every `.java` file into one or more `.class` files in `target/classes`. If the source breaks a rule, the compiler prints the file, the line and the reason and produces nothing. In this repository the compiler is also told `-Xlint:all -Werror`, so even a *warning* stops the build.
3. **Package.** The class files and the resources are zipped into a **jar** file (`target/*.jar`). Maven's Shade plugin adds the libraries' classes to the same file.
4. **Run.** `java -jar target/x.jar` starts the **JVM** (a process). The JVM opens the jar, finds the class named in its manifest, **loads** it, and calls its `main` method. It keeps running until `main` returns and no other (non-daemon) thread is still running, or until `System.exit` is called. The number given to `System.exit` becomes the program's exit status.
5. **JIT compilation.** While the program runs, the JVM watches which methods are used a lot and compiles *those* into real machine code on the fly (the JIT, "just-in-time", compiler). That is why a Java program is slow for its first seconds and fast afterwards, and why the lab scripts "warm up" the service before they measure.
6. **JDK, JRE.** The JDK (Java Development Kit) contains the compiler and tools (`javac`, `jar`, `jcmd`, `jfr`, `jshell`); the JRE (Java Runtime Environment) can only run programs. The build uses a JDK; the final Docker image only a JRE.

### The words of the language, in the order you meet them

- **Statements and blocks.** A statement ends with a semicolon `;`. Statements are grouped between curly braces `{ ... }` into a **block**; a class body, a method body, an `if` body are blocks. Indentation is only for people; the braces are what the compiler reads.
- **Comments.** `// text` runs to the end of the line; `/* ... */` can span lines; `/** ... */` is **Javadoc**, a comment that tools turn into documentation. All three are ignored by the compiler. (The walkthrough below shows the Javadoc too.)
- **Names.** Classes are written `UpperCamelCase` (`SpendingReport`), methods and variables `lowerCamelCase` (`rowsRead`), constants `UPPER_SNAKE_CASE` (`NO_STATUS`). Java is case-sensitive.
- **Types.** There are two families. **Primitive types** hold a plain value directly: `int` (whole number, about +/- 2 billion), `long` (64-bit whole number; a number literal ends in `L`), `double` (decimal number in binary form, inexact), `boolean` (`true` or `false`), `char` (one character, written in single quotes `'a'`), `byte` (8 bits). **Reference types** hold a reference (an arrow) to an **object**: `String` (text, written in double quotes), arrays (`int[]`), and every class. The value `null` means "no object".
- **Variables.** `int count = 0;` declares a variable of type `int` and gives it a value. `final` forbids changing it afterwards. `var x = 5;` lets the compiler infer the type. Fields (variables of a class) and local variables (inside a method) are different places.
- **Operators.** `+ - * / %` (arithmetic; `%` is the remainder; `+` also joins text: `"a" + 1` is `"a1"`), `== != < <= > >=` (comparison), `&& || !` (and, or, not; `&&` and `||` stop as soon as the answer is known), `=` (assign), `+= -=` (change in place), `++ --` (add or subtract one), `cond ? a : b` (choose one of two values), `instanceof` (is this object of that type?). **Careful:** `==` on objects asks "the very same object?", not "equal content?"; use `a.equals(b)`.
- **Control flow.** `if (...) {...} else {...}`; `for (int i = 0; i < n; i++) {...}` (counted loop); `for (Type x : collection) {...}` (loop over each element); `while (...) {...}`; `switch` (choose by value or by type); `return value;` (leave a method with a result); `break` (leave a loop); `continue` (go to the next round); `throw new SomeException(...)` (signal an error).
- **Methods.** `public int size() { ... }`: modifiers (`public`), the type of result (`int`; `void` means none), the name, the parameters in parentheses, and the body. A method belongs to a class (or to its objects). `static` methods belong to the class itself.
- **Classes and objects.** `new Crawler(a, b)` creates an object by calling the class's **constructor**. Inside a method `this` means the object the method was called on. `obj.method()` calls a method; `obj.field` reads a field. `private` hides a member inside its class; `public` allows everyone.
- **Packages and imports.** A **package** (`com.plainround.spend.domain`) is a named group of classes and matches a folder path. `import java.util.List;` lets the file use the short name `List`. Classes in `java.lang` (such as `String`) need no import.
- **Generics.** `List<String>` is a list that holds only strings; the part in angle brackets is a type given to the class as a parameter. `Map<String, Integer>` has two.
- **Annotations.** Words starting with `@` (`@Override`, `@Test`) attach information to the code that the compiler or a tool reads.
- **Lambdas and method references.** `x -> x + 1` is a small function written in place; `String::length` refers to an existing method as a function. They are used wherever an interface with one method is expected.
- **Exceptions.** An error is signalled by *throwing* an exception object. A `try { ... } catch (SomeException e) { ... } finally { ... }` handles it: the `catch` runs if such an exception was thrown inside the `try`, and `finally` runs always. If nobody catches an exception it ends the thread (and, in `main`, the program) with a stack trace.

### Where objects live

- Every **thread** (a line of execution) has its own **stack**: a pile of frames, one per method call currently running, holding local variables and the references they contain.
- All **objects** live in one shared area, the **heap**. A reference on the stack points to an object on the heap.
- The **garbage collector** (GC) frees heap objects that no running code can reach any more. You never free memory yourself. A program leaks memory in Java when it keeps a reference it no longer needs (see `jvm-gc-and-heap.md` in the leak lab).
- **Immutable** objects (such as `String` and records) cannot be changed after creation; sharing them between threads is safe.

### The standard library at a glance

Everything in `java.base`, the core module: `java.lang` (String, Math, Thread, exceptions), `java.util` (collections, Optional, Random), `java.util.stream` and `java.util.function` (streams, lambdas' types), `java.util.concurrent` (executors, semaphores, atomic numbers), `java.io` and `java.nio.file` (files), `java.net` and `java.net.http` (web addresses and the HTTP client), `java.math` (BigDecimal), `java.time` (dates and times), `java.text` (formatting). The imports of every file are explained in the table at the top of its walkthrough.

## 5. Walkthrough of this repository's use

Every Java file of the repository is explained below, in the order a newcomer should read it: first the program (`src/main/java`), starting with the innermost layer, then the tests (`src/test/java`). Each file is shown in pieces, and each piece is followed by its explanation. Every line of every file is shown, including the Javadoc comments (the `/** ... */` blocks above classes and methods: documentation written for people, which tools turn into web pages; the first sentence is a summary, and `@param`, `@return` and `@throws` lines describe parameters, results and errors). A word in **bold** is explained the first time it appears, in plain words, in brackets; later it appears in normal type. The glossary at the end of this section lists all of them again, so you can jump to it at any time.

### Part A. The program

#### `src/main/java/com/plainround/spend/domain/Transaction.java`

The central value of the program: one card transaction. It lives in the `domain` layer, the innermost layer, which uses only the Java standard library. It is a **record**, so it only carries data, and it refuses to exist with bad data.

> New words:
>
> - **record**: a short way to write a class that only carries data. You list the components once; Java writes the constructor, the accessor methods, `equals`, `hashCode` and `toString` for you, and the fields are `final`.

**Header.** The first lines are the **package** declaration and the **import** list. Each import brings one class into reach so the code can use its short name.

> New words:
>
> - **package**: a named group of classes. The name matches a folder path under `src/main/java`: `com.plainround.spend.domain` is the folder `com/plainround/spend/domain`. A package keeps names from clashing and lets a class hide things from other packages.
> - **import**: a line that brings a class from another package into reach so that code can write its short name. Without it the code would have to write the full name every time.

```java
package com.plainround.spend.domain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `BigDecimal` | `java.math.BigDecimal` | a decimal number with exact digits, used for money |
| `LocalDate` | `java.time.LocalDate` | a calendar date without a time or a time zone |
| `Currency` | `java.util.Currency` | an ISO 4217 currency such as USD, with its number of minor-unit digits |

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
public record Transaction(
    LocalDate date, String description, BigDecimal amount, Currency currency, String category) {
```

`public record Transaction(...)` declares the record and lists its five components: `date` (a **LocalDate**), `description` (text), `amount` (a **BigDecimal**), `currency` (a `Currency`) and `category` (text). For each component Java creates a `private final` field and an **accessor** with the same name, for example `amount()`. It also writes `equals`, `hashCode` and `toString`, so two transactions with the same five values are equal. `public` means any code may use it.

> New words:
>
> - **LocalDate**: a calendar date (year, month, day) without a time or time zone.
> - **BigDecimal**: a decimal number with exact digits. A `double` stores binary fractions (so 0.1 + 0.2 is 0.30000000000000004); `BigDecimal` keeps the digits you typed, which is why money uses it.
> - **accessor**: a method that returns one field's value. A record gives each component an accessor with the component's own name, for example `amount()`.

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
}
```

This is the **compact constructor**. It has no parameter list; the five components are already in scope as variables. Each `if` checks one rule and throws `IllegalArgumentException`, Java's standard **exception** for "a bad argument was given". `date == null` tests for **null**. For the amount, `amount == null || amount.signum() < 0` first checks for null (the `||` stops at the first true part, so the second part never runs on null) and then `signum()`, which returns -1, 0 or 1 for negative, zero or positive numbers: a negative amount is refused, zero is allowed (a waived fee is still a real row). The message text begins with the field name and, for the amount, appends the bad value with `+`, which joins text. `category.isBlank()` is true for an empty text and for text of only spaces. Because the checks run inside the constructor, no `Transaction` with bad values can ever exist, so the rest of the program does not have to re-check.

> New words:
>
> - **compact constructor**: the constructor of a record written without a parameter list. It runs before the fields are assigned, so it is the place to check or adjust the values.
> - **exception**: an object that represents something that went wrong. Code that detects a problem `throw`s one; the nearest `catch` that matches handles it; if none does, the program stops with an error message.
> - **null**: the value that means "no object". Calling a method on it throws `NullPointerException`, so code checks for it first.

#### `src/main/java/com/plainround/spend/domain/ParsedRow.java`

The answer to "what did this input line turn into?". It is the program's way of treating a bad row as data and not as a crash: a line becomes either a good `Ok` or a `Rejected` with a reason.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.domain;
```

```java
/**
 * The outcome of reading one input line: a valid {@link Transaction} or a {@link Rejected} reason.
 * A bad row is expected input, so it is data and not an exception; the sealed type makes the
 * compiler force every caller to handle both cases.
 */
public sealed interface ParsedRow permits ParsedRow.Ok, ParsedRow.Rejected {
```

`public sealed interface ParsedRow permits ParsedRow.Ok, ParsedRow.Rejected` declares a **sealed interface** **interface**: the list after `permits` is the complete list of types that may implement it. Nothing else can ever be a `ParsedRow`.

> New words:
>
> - **sealed interface**: an interface that names, after `permits`, every class allowed to implement it. The compiler then knows all the cases, which makes a `switch` over it checkable.
> - **interface**: a list of method names that a class promises to provide, without the code. Other code can use the interface and not care which class stands behind it.

```java
  /**
   * A row that became a valid transaction.
   *
   * @param transaction the parsed transaction
   */
  record Ok(Transaction transaction) implements ParsedRow {}
```

`Ok` is a record declared inside the interface. It `implements ParsedRow` and carries one component, the good `Transaction`. The empty braces `{}` mean the record needs no extra code.

```java
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

`Rejected` is the second case: the line number in the file (counting the header as line 1), the raw text of the line, and a human reason. `int` is a whole number. The closing `}` after it ends the interface.

#### `src/main/java/com/plainround/spend/domain/CategoryTotal.java`

A tiny value: how much was spent in one category.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.domain;
import java.math.BigDecimal;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `BigDecimal` | `java.math.BigDecimal` | a decimal number with exact digits, used for money |

```java
/**
 * The exact sum spent in one category.
 *
 * @param category the category name
 * @param total the exact, unrounded sum
 */
public record CategoryTotal(String category, BigDecimal total) {}
```

One record line: a category name and its exact total as a BigDecimal. The empty braces mean no extra code. The total is not rounded here; rounding happens only when the report is printed.

#### `src/main/java/com/plainround/spend/domain/MonthlyTotals.java`

Everything spent in one month: a month, the category totals and the sum.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.domain;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `BigDecimal` | `java.math.BigDecimal` | a decimal number with exact digits, used for money |
| `YearMonth` | `java.time.YearMonth` | a year and a month, such as 2026-01 |
| `List` | `java.util.List` | an ordered collection of elements |

```java
/**
 * Everything spent in one month.
 *
 * @param month the calendar month
 * @param categories the category totals, largest first, ties by name
 * @param total the exact sum over all categories
 */
public record MonthlyTotals(YearMonth month, List<CategoryTotal> categories, BigDecimal total) {
```

A record with a `YearMonth` (a year and a month, such as 2026-01), a **list** of `CategoryTotal` (the angle brackets are **generics**: the list holds only `CategoryTotal` objects) and the month's `total`.

> New words:
>
> - **list**: an ordered collection. Elements keep their position and can repeat. `List.of(...)` builds a list that cannot be changed.
> - **generics**: type parameters in angle brackets: `List<String>` is a list that holds only strings. The compiler checks the types so a wrong element cannot slip in.

```java
  /** Copies the list so the value cannot change after construction. */
  public MonthlyTotals {
    categories = List.copyOf(categories);
  }
}
```

The compact constructor replaces the incoming list with `List.copyOf(categories)`, an **immutable** copy. Without it, the caller who passed the list could change it later and change this record behind its back. After the constructor the record is truly fixed.

> New words:
>
> - **immutable**: never changed after creation. Immutable objects can be shared freely because nobody can alter them.

#### `src/main/java/com/plainround/spend/domain/SpendingReport.java`

The finished result of reading the whole file, ready to print. It is the value that the use case returns and the printer consumes.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.domain;
import java.util.Currency;
import java.util.List;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `Currency` | `java.util.Currency` | an ISO 4217 currency such as USD, with its number of minor-unit digits |
| `List` | `java.util.List` | an ordered collection of elements |

```java
/**
 * The finished report: totals, the largest purchases and the rows that could not be read.
 *
 * @param currency the currency every amount is in
 * @param rowsRead how many rows became transactions
 * @param rejectedCount how many rows were rejected, including those not kept in {@code rejected}
 * @param months one entry per month with spending, oldest first
 * @param largest the largest purchases, biggest first
 * @param rejected the rejected rows kept for display, in file order
 */
public record SpendingReport(
    Currency currency,
    int rowsRead,
    int rejectedCount,
    List<MonthlyTotals> months,
    List<Transaction> largest,
    List<ParsedRow.Rejected> rejected) {
```

A record with six components: the report `currency`, how many rows were good (`rowsRead`), how many were rejected (`rejectedCount`, always exact), the monthly totals, the largest purchases, and the rejected rows that are kept for display (a shorter list than `rejectedCount` when there were very many).

```java
  /** Copies the lists so the value cannot change after construction. */
  public SpendingReport {
    months = List.copyOf(months);
    largest = List.copyOf(largest);
    rejected = List.copyOf(rejected);
  }
```

The compact constructor makes immutable copies of the three lists with `List.copyOf`, so the report cannot change after it is built.

```java
  /** Returns true when at least one row was rejected. */
  public boolean hasRejects() {
    return rejectedCount > 0;
  }
}
```

A small **method** that answers a yes/no question: `boolean` is the type with only `true` and `false`. `rejectedCount > 0` is true when any row was rejected. The command line uses it to choose exit status 2. The closing `}` ends the record.

> New words:
>
> - **method**: a named block of code that belongs to a class. It can take parameters and return a value; `void` means it returns nothing.

#### `src/main/java/com/plainround/spend/domain/SpendingAccumulator.java`

The heart of the program. It receives parsed rows one at a time and keeps running totals, so a file of any size can be read with little memory: it never stores all rows. It is in the `domain` layer and knows nothing about files or printing.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.domain;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `BigDecimal` | `java.math.BigDecimal` | a decimal number with exact digits, used for money |
| `YearMonth` | `java.time.YearMonth` | a year and a month, such as 2026-01 |
| `ArrayList` | `java.util.ArrayList` | a growable list backed by an array |
| `Comparator` | `java.util.Comparator` | an object that says how to order two values |
| `Currency` | `java.util.Currency` | an ISO 4217 currency such as USD, with its number of minor-unit digits |
| `List` | `java.util.List` | an ordered collection of elements |
| `Map` | `java.util.Map` | a collection of key-value pairs |
| `PriorityQueue` | `java.util.PriorityQueue` | a queue that always gives out its smallest element first (by its ordering) |
| `TreeMap` | `java.util.TreeMap` | a map that keeps its keys sorted |

```java
/**
 * Folds parsed rows into a {@link SpendingReport} one at a time. Memory is bounded by the number of
 * month and category pairs, the few largest purchases and a capped list of rejects, so a file of
 * any size streams through. Amounts are added exactly and are never rounded here.
 */
public final class SpendingAccumulator {

  /** Largest first; equal amounts fall back to the earlier date, then the description. */
  private static final Comparator<Transaction> LARGEST_FIRST =
      Comparator.comparing(Transaction::amount)
          .reversed()
          .thenComparing(Transaction::date)
          .thenComparing(Transaction::description);
```

`public final class SpendingAccumulator` declares a **class** that nobody may extend. The first `static final` **field**, `LARGEST_FIRST`, is a **comparator**: `Comparator.comparing(Transaction::amount)` orders transactions by amount (`Transaction::amount` is a **method reference** to the amount accessor), `.reversed()` flips it so the biggest comes first, and `.thenComparing(...)` breaks ties by date and then by description, so the order is the same on every run. `static` means one copy for the class; `final` means it never changes; `private` hides it.

> New words:
>
> - **class**: a blueprint for objects. It lists the data each object holds (fields) and the actions it can do (methods). The program creates objects from it with `new`.
> - **field**: a variable that belongs to an object (or, when `static`, to the class). It holds one piece of the object's data.
> - **comparator**: an object that decides which of two values comes first. Comparators can be chained: order by this, and if equal, by that.
> - **method reference**: a short way to pass an existing method as a function: `BigDecimal::add` means "the `add` method of `BigDecimal`".

```java
  private final Currency currency;
  private final int largestCount;
  private final int maxRejectedKept;
  private final Map<YearMonth, Map<String, BigDecimal>> totals = new TreeMap<>();
  private final PriorityQueue<Transaction> largest = new PriorityQueue<>(LARGEST_FIRST.reversed());
  private final List<ParsedRow.Rejected> rejected = new ArrayList<>();
  private int rowsRead;
  private int rejectedCount;
```

The instance fields. `currency`, `largestCount` and `maxRejectedKept` are `private final`: set once by the constructor. `totals` is a **map** from a month to another map from a category name to a running sum; `new TreeMap<>()` is a **TreeMap**, which keeps its keys sorted, so months come out in calendar order and categories in name order. `largest` is a **priority queue** ordered by the reversed comparator, so its head (the next element removed) is the *smallest* of the kept purchases. `rejected` is an `ArrayList`, a growable list. The last two fields, `rowsRead` and `rejectedCount`, are plain `int` counters that start at 0 and change (they are not `final`).

> New words:
>
> - **map**: a collection of key-value pairs: you give a key and get the value stored under it. A key appears at most once.
> - **TreeMap**: a map that keeps its keys sorted, so iterating gives them in order.
> - **priority queue**: a collection whose head is always the smallest element according to its ordering. Adding and removing the head are fast.

```java
  /**
   * Creates an empty accumulator.
   *
   * @param currency the report currency
   * @param largestCount how many of the largest purchases to keep, at least 1
   * @param maxRejectedKept how many rejected rows to keep for display, at least 0
   */
  public SpendingAccumulator(Currency currency, int largestCount, int maxRejectedKept) {
    if (largestCount < 1 || maxRejectedKept < 0) {
      throw new IllegalArgumentException(
          "largestCount must be at least 1, maxRejectedKept 0 or more");
    }
    this.currency = currency;
    this.largestCount = largestCount;
    this.maxRejectedKept = maxRejectedKept;
  }
```

The **constructor**. It validates its limits: `largestCount < 1 || maxRejectedKept < 0` is true if either limit makes no sense, and then it throws. Otherwise it stores the arguments in the fields (`this.currency = currency` assigns the parameter to the field of the same name).

> New words:
>
> - **constructor**: the special method that sets up a new object. It has the class's name, no return type, and runs when you write `new ClassName(...)`.

```java
  /** Adds one parsed row to the running totals. */
  public void accept(ParsedRow row) {
    switch (row) {
      case ParsedRow.Ok ok -> add(ok.transaction());
      case ParsedRow.Rejected bad -> reject(bad);
    }
  }
```

The only way to feed the accumulator. This is a **pattern-matching switch** over the sealed interface type `ParsedRow`: if the row is an `Ok`, the variable `ok` has that type and `add(ok.transaction())` runs; if it is a `Rejected`, `reject(bad)` runs. There is no `default` branch because the sealed type guarantees the two cases are all there is.

> New words:
>
> - **pattern-matching switch**: a `switch` that tests the type of a value: `case ParsedRow.Ok ok ->` runs when the value is an `Ok` and gives it the name `ok`. With a sealed type the compiler checks that every case is handled.

```java
  private void add(Transaction transaction) {
    rowsRead++;
    totals
        .computeIfAbsent(YearMonth.from(transaction.date()), month -> new TreeMap<>())
        .merge(transaction.category(), transaction.amount(), BigDecimal::add);
    largest.add(transaction);
    if (largest.size() > largestCount) {
      largest.poll();
    }
  }
```

`rowsRead++` adds one to the counter. The next statement is one chain: `totals.computeIfAbsent(YearMonth.from(transaction.date()), month -> new TreeMap<>())` finds the inner map for the transaction's month, creating an empty one with the **lambda** `month -> new TreeMap<>()` if there is none yet. `.merge(category, amount, BigDecimal::add)` then adds the amount to the sum already stored under the category, or stores it if the category is new; `BigDecimal::add` is a method reference. Then the transaction is added to the priority queue, and if the queue now holds more than `largestCount`, `largest.poll()` removes its head, the smallest, so only the biggest purchases stay. Memory therefore depends on the number of months and categories, not on the number of rows.

> New words:
>
> - **lambda**: a short function written inline: `month -> new TreeMap<>()` means "given `month`, return a new map". The part before `->` names the parameters.

```java
  private void reject(ParsedRow.Rejected bad) {
    rejectedCount++;
    if (rejected.size() < maxRejectedKept) {
      rejected.add(bad);
    }
  }
```

Counts the rejected row (`rejectedCount++`, always) and keeps the row itself only while fewer than `maxRejectedKept` are stored. A file full of bad rows therefore cannot use unlimited memory, yet the printed count stays exact.

```java
  /** Returns the report for everything accepted so far. */
  public SpendingReport report() {
    List<MonthlyTotals> months =
        totals.entrySet().stream().map(SpendingAccumulator::month).toList();
    List<Transaction> top = largest.stream().sorted(LARGEST_FIRST).toList();
    return new SpendingReport(currency, rowsRead, rejectedCount, months, top, rejected);
  }
```

Builds the final value. `totals.entrySet().stream().map(SpendingAccumulator::month).toList()` is a **stream**: take the month entries, turn each into a `MonthlyTotals` with the `month` method below, and collect them into a list. `largest.stream().sorted(LARGEST_FIRST).toList()` sorts the kept purchases biggest first. The last line builds the `SpendingReport` record.

> New words:
>
> - **stream**: a sequence of values processed step by step: `filter` keeps some, `map` changes each, `sorted` orders them, and a last step such as `toList()` collects the result. A stream does not change the collection it came from.

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
}
```

Turns one month's map into a `MonthlyTotals`. It streams the category entries, builds a `CategoryTotal` for each with a lambda, sorts them by total, largest first (`Comparator.comparing(CategoryTotal::total).reversed()`), ties by name, and collects. Then `reduce(BigDecimal.ZERO, BigDecimal::add)` folds the totals into one sum, starting from zero. Nothing is rounded: all additions are exact. The final `}` closes the class.

#### `src/main/java/com/plainround/spend/application/RowSource.java`

A **port (in the design sense)**: the use case's description of "where rows come from". It lives in the `application` layer; the file reader in `infrastructure` implements it.

> New words:
>
> - **port (in the design sense)**: an interface that the inner part of the program owns and calls (for example "where do rows come from"); the outer part supplies an implementation, called an adapter.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.application;
import com.plainround.spend.domain.ParsedRow;
import java.util.function.Consumer;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `ParsedRow` | `com.plainround.spend.domain.ParsedRow` | this repository's own `ParsedRow` (file `src/main/java/com/plainround/spend/domain/ParsedRow.java`, explained in its own section) |
| `Consumer` | `java.util.function.Consumer` | a function that takes a value and returns nothing |

```java
/** Port: where transaction rows come from. Rows are pushed one at a time so nothing is held. */
public interface RowSource {

  /**
   * Reads every data row in file order and hands each to the consumer as it is parsed.
   *
   * @param consumer receives one {@link ParsedRow} per non-blank data line
   * @throws InputUnreadableException when the source as a whole cannot be read
   */
  void readRows(Consumer<ParsedRow> consumer);
}
```

One interface with one method, `readRows`, that takes a `Consumer<ParsedRow>`. A **consumer** is a function that takes a value and returns nothing, so the source pushes each row to the consumer as it reads it. Pushing one row at a time is what lets a 2 GB file be processed with a small heap. The method has no body here; every implementation supplies it.

> New words:
>
> - **consumer**: a function that takes a value and returns nothing, standard interface `Consumer<T>`; it is how code hands values to someone else one at a time.

#### `src/main/java/com/plainround/spend/application/InputUnreadableException.java`

The named error for "the file as a whole cannot be read" (missing file, bytes that are not UTF-8). It is different from a bad row, which is data.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.application;
```

```java
/** The input file as a whole cannot be read (missing, not UTF-8, I/O failure), unlike a bad row. */
public final class InputUnreadableException extends RuntimeException {

  private static final long serialVersionUID = 1L;
```

`extends RuntimeException` makes this an **unchecked exception** exception: a normal Java error type that methods need not declare. `private static final long serialVersionUID = 1L;` is the **serialVersionUID** that Java asks for; the value is just a version number.

> New words:
>
> - **unchecked exception**: an exception type (a subclass of `RuntimeException`) that methods do not have to declare. Checked ones (such as `IOException`) must be declared or caught.
> - **serialVersionUID**: a version number Java asks for on exception classes because they can be written to a stream. The compiler flag `-Xlint:all` would warn without it, and `-Werror` turns warnings into errors.

```java
  /**
   * Creates the exception.
   *
   * @param message what is wrong with the input, written for the person running the tool
   * @param cause the underlying failure
   */
  public InputUnreadableException(String message, Throwable cause) {
    super(message, cause);
  }
}
```

The constructor takes a human message and the **cause** (the original error). `super(message, cause)` hands both to the parent class, so the cause is kept for anyone who needs the details.

> New words:
>
> - **cause**: the original exception wrapped inside a new one, so the full story stays available when the new exception is reported.

#### `src/main/java/com/plainround/spend/application/BuildSpendingReport.java`

The use case: "read all rows from a source and make the report". It is in the `application` layer and orchestrates; the arithmetic is in the accumulator.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.application;
import com.plainround.spend.domain.SpendingAccumulator;
import com.plainround.spend.domain.SpendingReport;
import java.util.Currency;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `SpendingAccumulator` | `com.plainround.spend.domain.SpendingAccumulator` | this repository's own `SpendingAccumulator` (file `src/main/java/com/plainround/spend/domain/SpendingAccumulator.java`, explained in its own section) |
| `SpendingReport` | `com.plainround.spend.domain.SpendingReport` | this repository's own `SpendingReport` (file `src/main/java/com/plainround/spend/domain/SpendingReport.java`, explained in its own section) |
| `Currency` | `java.util.Currency` | an ISO 4217 currency such as USD, with its number of minor-unit digits |

```java
/** Use case: stream every row from a {@link RowSource} into a {@link SpendingReport}. */
public final class BuildSpendingReport {

  private final RowSource source;
  private final Currency currency;
  private final int largestCount;
  private final int maxRejectedKept;
```

A **final** class with four `private final` fields: the `RowSource` port (in the design sense) it reads from, the report currency, how many largest purchases to keep, and how many rejected rows to keep.

> New words:
>
> - **final**: can be set once and never changed. On a field: assigned in the constructor and then fixed. On a class: no other class may extend it.

```java
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

The constructor stores the four values. It receives the source through the constructor, which is **dependency injection**: the real program passes a file reader, a test passes an in-memory fake.

> New words:
>
> - **dependency injection**: giving an object the things it needs through its constructor instead of letting it create them. A test can then hand in a fake.

```java
  /** Reads the whole source and returns the report. */
  public SpendingReport run() {
    SpendingAccumulator accumulator =
        new SpendingAccumulator(currency, largestCount, maxRejectedKept);
    source.readRows(accumulator::accept);
    return accumulator.report();
  }
}
```

Creates a fresh `SpendingAccumulator`, then `source.readRows(accumulator::accept)` runs the whole read: the source calls `accumulator.accept` for each row (a method reference used as the consumer). When the source returns, `accumulator.report()` gives the finished report. The final `}` closes the class.

#### `src/main/java/com/plainround/spend/infrastructure/Settings.java`

The program's configuration, read from environment variables and validated. It is in the `infrastructure` layer because reading the environment is a technical concern.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.infrastructure;
import java.util.Currency;
import java.util.Map;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `Currency` | `java.util.Currency` | an ISO 4217 currency such as USD, with its number of minor-unit digits |
| `Map` | `java.util.Map` | a collection of key-value pairs |

```java
/**
 * Configuration read from the environment (twelve-factor) with validated defaults.
 *
 * @param reportCurrency the currency of the report; rows in other currencies are rejected
 * @param largestCount how many largest purchases to list
 * @param maxRejectedKept how many rejected rows to keep for display
 */
public record Settings(Currency reportCurrency, int largestCount, int maxRejectedKept) {

  private static final String DEFAULT_CURRENCY = "USD";
```

A record of three values: the report currency, how many largest purchases to list, and how many rejected rows to keep. The first lines of the body declare `DEFAULT_CURRENCY = "USD"`, a constant (`private static final`) used when the variable is missing.

```java
  /** Validates the values. */
  public Settings {
    if (largestCount < 1) {
      throw new IllegalArgumentException("SPEND_TOP must be at least 1: " + largestCount);
    }
    if (maxRejectedKept < 0) {
      throw new IllegalArgumentException(
          "SPEND_MAX_REJECTED must be 0 or more: " + maxRejectedKept);
    }
  }
```

The compact constructor validates: the largest-purchase count must be at least 1 and the rejected-rows limit must not be negative. The messages name the environment variable (`SPEND_TOP`, `SPEND_MAX_REJECTED`) so the person can find the setting that is wrong.

```java
  /**
   * Reads {@code SPEND_CURRENCY} (default USD), {@code SPEND_TOP} (default 5) and {@code
   * SPEND_MAX_REJECTED} (default 1000).
   *
   * @param environment the variables, normally {@link System#getenv()}
   * @return validated settings
   * @throws IllegalArgumentException when a value is malformed
   */
  public static Settings fromEnvironment(Map<String, String> environment) {
    return new Settings(
        currency(environment.getOrDefault("SPEND_CURRENCY", DEFAULT_CURRENCY)),
        integer(environment, "SPEND_TOP", 5),
        integer(environment, "SPEND_MAX_REJECTED", 1000));
  }
```

A **static** factory method that builds settings from a `Map<String, String>` of **environment variable**s (the real program passes `System.getenv()`; a test passes an ordinary map). `getOrDefault("SPEND_CURRENCY", DEFAULT_CURRENCY)` returns the value if the variable is set, otherwise `USD`. The two numbers are read by the helper `integer` with defaults 5 and 1000.

> New words:
>
> - **static**: a member that belongs to the class itself and not to any one object. You call it as `ClassName.member`, without `new`.
> - **environment variable**: a named text setting that a program inherits from the process that started it, for example `LOG_LEVEL=DEBUG`. Programs read them to be configured without changing code.

```java
  /** Returns these settings with another currency. */
  public Settings withCurrency(String code) {
    return new Settings(currency(code), largestCount, maxRejectedKept);
  }
```

Returns a *new* `Settings` with a different currency; records cannot be changed, so a command-line option produces a copy. This and the next method are used for the `--currency` and `--top` options.

```java
  /** Returns these settings with another largest-purchase count. */
  public Settings withLargestCount(int count) {
    return new Settings(reportCurrency, count, maxRejectedKept);
  }
```

Same idea for the largest-purchase count: copy with one value replaced.

```java
  private static Currency currency(String code) {
    try {
      return Currency.getInstance(code);
    } catch (IllegalArgumentException problem) {
      throw new IllegalArgumentException("unknown currency code '" + code + "'", problem);
    }
  }
```

Looks up a currency by its three-letter code with `Currency.getInstance`. That method throws `IllegalArgumentException` with no message at all for an unknown code, so the **try/catch** catches it and throws a new one that says `unknown currency code 'XYZ'` and keeps the original as the cause (the second argument).

> New words:
>
> - **try/catch**: `try { ... }` runs code; if it throws, Java looks through the `catch` blocks in order and runs the first one whose type matches the exception.

```java
  private static int integer(Map<String, String> environment, String name, int fallback) {
    String text = environment.get(name);
    try {
      return text == null ? fallback : Integer.parseInt(text.trim());
    } catch (NumberFormatException problem) {
      throw new IllegalArgumentException(name + " is not a whole number: '" + text + "'", problem);
    }
  }
}
```

Reads one whole number from the map. `environment.get(name)` returns null when the variable is missing, in which case the `fallback` is used. `text == null ? fallback : Integer.parseInt(text.trim())` is a **conditional expression**: if null use the fallback, otherwise remove surrounding spaces with `trim()` and convert the text to a number. If the text is not a number `parseInt` throws `NumberFormatException`, which is caught and rethrown as an `IllegalArgumentException` naming the variable. The last `}` closes the record.

> New words:
>
> - **conditional expression**: `condition ? a : b` gives `a` if the condition is true and `b` otherwise.

#### `src/main/java/com/plainround/spend/infrastructure/csv/CsvFieldSplitter.java`

Splits one line of a CSV file into its fields, keeping commas that are inside double quotes. This is the code that fixes the lesson's first bug (`line.split(",")` breaks on `"1,204.50"`).

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.infrastructure.csv;
import java.util.ArrayList;
import java.util.List;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `ArrayList` | `java.util.ArrayList` | a growable list backed by an array |
| `List` | `java.util.List` | an ordered collection of elements |

```java
/** Splits one CSV line on commas that are not inside double quotes. */
final class CsvFieldSplitter {

  private CsvFieldSplitter() {}
```

No `public` word: the class is visible only inside its own package (`infrastructure.csv`). It is `final`. The constructor is `private` and empty, which prevents anyone from creating an instance; the class is only a holder for one static method.

```java
  /**
   * Splits a line into trimmed fields; quote characters are removed from the result.
   *
   * @param line one line of CSV text
   * @return the fields, never empty (an empty line is one empty field)
   */
  static List<String> split(String line) {
    List<String> fields = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean insideQuotes = false;
    for (char symbol : line.toCharArray()) {
      if (symbol == '"') {
        insideQuotes = !insideQuotes;
      } else if (symbol == ',' && !insideQuotes) {
        fields.add(current.toString().trim());
        current.setLength(0);
      } else {
        current.append(symbol);
      }
    }
    fields.add(current.toString().trim());
    return fields;
  }
}
```

`fields` is the result list. `current` is a **StringBuilder** that collects the characters of the field being read. `insideQuotes` is a `boolean` flag that starts `false`. The `for` loop walks the line character by character (`line.toCharArray()` turns the text into an array of **char**s; this is a **for-each loop** loop). Three cases: a double quote (`'"'`) flips the flag (`!insideQuotes` is "not insideQuotes") and is not copied; a comma while the flag is false ends the field: the field text is trimmed of spaces, added to the list, and the builder is emptied with `setLength(0)`; any other character is appended. After the loop the last field is added, because the line does not end with a comma. The quote characters never reach the result, so `"1,204.50"` becomes `1,204.50`. `&&` means "and".

> New words:
>
> - **StringBuilder**: a text buffer you can append to cheaply, used to build one string out of many pieces.
> - **char**: a single character, written in single quotes such as `','`.
> - **for-each loop**: `for (Type x : collection)` runs the body once for each element, naming it `x`.

#### `src/main/java/com/plainround/spend/infrastructure/csv/CsvRowParser.java`

Turns one text line into a `ParsedRow`: a good `Transaction` or a `Rejected` with a reason. It never throws for bad input.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.infrastructure.csv;
import com.plainround.spend.domain.ParsedRow;
import com.plainround.spend.domain.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Currency;
import java.util.List;
import java.util.regex.Pattern;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `ParsedRow` | `com.plainround.spend.domain.ParsedRow` | this repository's own `ParsedRow` (file `src/main/java/com/plainround/spend/domain/ParsedRow.java`, explained in its own section) |
| `Transaction` | `com.plainround.spend.domain.Transaction` | this repository's own `Transaction` (file `src/main/java/com/plainround/spend/domain/Transaction.java`, explained in its own section) |
| `BigDecimal` | `java.math.BigDecimal` | a decimal number with exact digits, used for money |
| `LocalDate` | `java.time.LocalDate` | a calendar date without a time or a time zone |
| `DateTimeFormatter` | `java.time.format.DateTimeFormatter` | a pattern for reading and writing dates |
| `DateTimeParseException` | `java.time.format.DateTimeParseException` | the error for text that is not a valid date |
| `Currency` | `java.util.Currency` | an ISO 4217 currency such as USD, with its number of minor-unit digits |
| `List` | `java.util.List` | an ordered collection of elements |
| `Pattern` | `java.util.regex.Pattern` | a compiled regular expression |

```java
/**
 * Turns one CSV line into a {@link ParsedRow}. Never throws for bad input: every failure becomes a
 * {@link ParsedRow.Rejected} whose reason a person can act on.
 */
public final class CsvRowParser {

  private static final int COLUMNS = 5;

  /** Plain decimals only: BigDecimal also accepts exponents such as 1E+999999999, a memory bomb. */
  private static final Pattern PLAIN_DECIMAL = Pattern.compile("[+-]?\\d+(\\.\\d+)?");

  private final Currency reportCurrency;
```

Three fields: `COLUMNS` (the number of columns a good row must have, 5), `PLAIN_DECIMAL`, and `reportCurrency`. `PLAIN_DECIMAL` is a compiled **regular expression**: `[+-]?\d+(\.\d+)?` means an optional sign, one or more digits, and optionally a dot followed by digits. (In Java source each backslash is written twice.) It accepts `42.17` and `-8.00` and refuses `sixty`, an empty text and an exponent such as `1E+999999999`, which `BigDecimal` would accept but which could allocate a huge number.

> New words:
>
> - **regular expression**: a short pattern language for text shapes: `\d` is a digit, `+` means one or more, `?` optional, `[+-]` one of the two signs, `(...)` a group, `.` any character. A `Pattern` is a compiled one and `matcher(text).matches()` asks whether the whole text fits.

```java
  /**
   * Creates a parser.
   *
   * @param reportCurrency the only currency accepted; other currencies are rejected
   */
  public CsvRowParser(Currency reportCurrency) {
    this.reportCurrency = reportCurrency;
  }
```

The constructor remembers the one currency the report accepts.

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

First it splits the line with the splitter and checks the count; a wrong number of columns is rejected at once with a message that includes the count. Then comes the try/catch. `new ParsedRow.Ok(transaction(fields))` tries to build the good value. The `catch` blocks are checked in order and the first matching type wins, so the order matters (the comment says so): `DateTimeParseException` is a bad date; `NumberFormatException` is a bad number, and it is a *subclass* of `IllegalArgumentException`, so it must come before the general `IllegalArgumentException` block, which receives everything else (for example the messages thrown by `Transaction`'s constructor, or by `currency`). Every branch returns a `Rejected` built by the helper `reject`. `problem.getMessage()` is the text of the exception.

```java
  private Transaction transaction(List<String> fields) {
    LocalDate date = LocalDate.parse(fields.get(0), DateTimeFormatter.ISO_LOCAL_DATE);
    // The String constructor keeps the digits typed; new BigDecimal(double) would not.
    BigDecimal amount = amount(fields.get(2));
    Currency currency = currency(fields.get(3));
    return new Transaction(date, fields.get(1), amount, currency, fields.get(4));
  }
```

Reads the five fields by position (`fields.get(0)` is the first). `LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE)` reads a date of the form `2026-01-31` and throws `DateTimeParseException` for anything else, including `31/02/2026`. The amount and the currency use the helpers below. The `Transaction` constructor then applies its own checks.

```java
  private static BigDecimal amount(String text) {
    String digits = text.replace(",", "");
    if (!PLAIN_DECIMAL.matcher(digits).matches()) {
      throw new NumberFormatException("not a plain decimal: " + text);
    }
    return new BigDecimal(digits);
  }
```

`text.replace(",", "")` removes the thousands comma that survived inside a quoted field. `PLAIN_DECIMAL.matcher(digits).matches()` asks whether the *whole* text fits the pattern; if not, it throws `NumberFormatException` with a message, which the `catch` in `parse` turns into `amount is not a number`. Only then does `new BigDecimal(digits)` build the exact decimal. The `String` form of the constructor keeps the digits as typed; `new BigDecimal(double)` would carry the binary error.

```java
  private Currency currency(String code) {
    Currency currency;
    try {
      currency = Currency.getInstance(code);
    } catch (IllegalArgumentException problem) {
      // Currency.getInstance throws with no message at all, so say what is wrong.
      throw new IllegalArgumentException("unknown currency code '" + code + "'", problem);
    }
    if (!currency.equals(reportCurrency)) {
      throw new IllegalArgumentException(
          "currency " + currency + " is not the report currency " + reportCurrency);
    }
    return currency;
  }
```

Declares `currency` without a value, then tries `Currency.getInstance(code)`. For a code such as `US$` that method throws without a message, so the `catch` throws a new error that says `unknown currency code 'US$'` (keeping the original as the cause). Then `currency.equals(reportCurrency)` compares with the report currency; a different valid currency (EUR in a USD report) is rejected with a clear sentence. `!` means "not".

```java
  private static ParsedRow reject(int lineNumber, String line, String reason) {
    return new ParsedRow.Rejected(lineNumber, line, reason);
  }
}
```

A one-line helper that builds a `Rejected`. It exists so the `return` lines in `parse` stay short and readable. The last `}` closes the class.

#### `src/main/java/com/plainround/spend/infrastructure/csv/CsvFileRowSource.java`

The **adapter** that implements the `RowSource` port (in the design sense) with a real file. It reads the file lazily and hands each line to the parser.

> New words:
>
> - **adapter**: a class that implements a port using a real technology, such as a file or a network.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.infrastructure.csv;
import com.plainround.spend.application.InputUnreadableException;
import com.plainround.spend.application.RowSource;
import com.plainround.spend.domain.ParsedRow;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `InputUnreadableException` | `com.plainround.spend.application.InputUnreadableException` | this repository's own `InputUnreadableException` (file `src/main/java/com/plainround/spend/application/InputUnreadableException.java`, explained in its own section) |
| `RowSource` | `com.plainround.spend.application.RowSource` | this repository's own `RowSource` (file `src/main/java/com/plainround/spend/application/RowSource.java`, explained in its own section) |
| `ParsedRow` | `com.plainround.spend.domain.ParsedRow` | this repository's own `ParsedRow` (file `src/main/java/com/plainround/spend/domain/ParsedRow.java`, explained in its own section) |
| `IOException` | `java.io.IOException` | the standard error type for failures while reading or writing (a file, a network connection) |
| `UncheckedIOException` | `java.io.UncheckedIOException` | an unchecked wrapper around an `IOException`; lazy file streams throw it because they cannot throw the checked kind |
| `CharacterCodingException` | `java.nio.charset.CharacterCodingException` | the error for bytes that are not valid in the chosen character encoding |
| `StandardCharsets` | `java.nio.charset.StandardCharsets` | constants for the standard character encodings, such as UTF-8 |
| `Files` | `java.nio.file.Files` | static helper methods for reading and writing files |
| `Path` | `java.nio.file.Path` | a location of a file or folder |
| `Iterator` | `java.util.Iterator` | an object that hands out the elements of a collection one at a time |
| `Consumer` | `java.util.function.Consumer` | a function that takes a value and returns nothing |
| `Stream` | `java.util.stream.Stream` | a sequence of elements processed step by step |
| `Logger` | `org.slf4j.Logger` | the logger type of the SLF4J logging interface |
| `LoggerFactory` | `org.slf4j.LoggerFactory` | creates loggers |

```java
/**
 * Reads a UTF-8 CSV file lazily: the header is skipped, a byte-order mark is stripped, blank lines
 * are ignored and line numbers count the header as line 1.
 */
public final class CsvFileRowSource implements RowSource {

  private static final Logger LOG = LoggerFactory.getLogger(CsvFileRowSource.class);
  private static final String BYTE_ORDER_MARK = "﻿";

  private final Path path;
  private final CsvRowParser parser;
```

The class **implements** `RowSource`. `LOG` is a **logger** named after the class. `BYTE_ORDER_MARK` is a text holding one invisible character, U+FEFF: some programs put it at the very start of a UTF-8 file. The two `private final` fields are the file `Path` and the `CsvRowParser`.

> New words:
>
> - **implements**: the word with which a class promises to provide every method of an interface.
> - **logger**: an object that writes log lines with a level (`INFO`, `WARN`, ...). The log engine decides where they go and how they look.

```java
  /**
   * Creates a source.
   *
   * @param path the CSV file
   * @param parser parses one data line
   */
  public CsvFileRowSource(Path path, CsvRowParser parser) {
    this.path = path;
    this.parser = parser;
  }
```

The constructor stores the path and the parser it will use.

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

`@Override` is the **@Override** annotation. The method logs the file name with `LOG.info`; `{}` in the message is replaced by the next argument. The comment explains the next line: `Files.lines(path, StandardCharsets.UTF_8)` opens the file as a **lazy stream of lines** and names the **character encoding** explicitly, because the machine's default differs between computers. It sits in a **try-with-resources** header, so the file is closed at the end of the block. `emit(lines.iterator(), consumer)` does the work. Two `catch` blocks follow: `UncheckedIOException` (what a lazy stream throws when reading fails halfway, for example on bytes that are not valid UTF-8) is unwrapped with `getCause()`, and a plain `IOException` (for example, the file does not exist) is passed on as is. Both become an `InputUnreadableException` through `unreadable`.

> New words:
>
> - **@Override**: an annotation that says "this method replaces a method of the parent class or interface". The compiler reports an error if no such method exists.
> - **lazy stream of lines**: `Files.lines` reads a file one line at a time as it is needed instead of loading it whole, so a huge file uses little memory.
> - **character encoding**: the rule that turns bytes into text. UTF-8 is the common one. If a program does not name it, it uses the machine's default, which differs between computers.
> - **try-with-resources**: `try (Resource r = ...) { ... }` closes the resource automatically when the block ends, even if an exception is thrown. It is how files, streams and servers are closed safely.

```java
  private void emit(Iterator<String> lines, Consumer<ParsedRow> consumer) {
    int lineNumber = 0;
    while (lines.hasNext()) {
      String line = lines.next().replace(BYTE_ORDER_MARK, "");
      lineNumber++;
      if (lineNumber > 1 && !line.isBlank()) {
        consumer.accept(parser.parse(lineNumber, line));
      }
    }
  }
```

Walks the lines with an **iterator**. `lineNumber` counts from 1 (the header is line 1). `lines.next().replace(BYTE_ORDER_MARK, "")` takes the next line and removes the invisible mark if it is there. The `if` skips line 1 (the header) and blank lines (`!line.isBlank()`, `!` means not) but the counter still counts them, so a rejected row reports the true line number. Every other line goes to `parser.parse(lineNumber, line)` and the result is handed to the consumer.

> New words:
>
> - **iterator**: an object with `hasNext()` and `next()` that hands out the elements of a collection one by one.

```java
  private InputUnreadableException unreadable(IOException cause) {
    String detail =
        cause instanceof CharacterCodingException
            ? "the file is not valid UTF-8"
            : cause.getClass().getSimpleName() + ": " + cause.getMessage();
    return new InputUnreadableException("cannot read " + path + " (" + detail + ")", cause);
  }
}
```

Builds the error with a sentence a person can act on. The conditional expression chooses the detail: `cause instanceof CharacterCodingException` tests the type of the error and, if it is the bytes-not-valid error, says `the file is not valid UTF-8`; otherwise it uses the error's class name and message. The result is `cannot read PATH (DETAIL)`. The final `}` closes the class.

#### `src/main/java/com/plainround/spend/interfaces/cli/ReportPrinter.java`

Prints a `SpendingReport` as text. It is in the outermost layer, `interfaces`. It is the only place where money is rounded.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.interfaces.cli;
import com.plainround.spend.domain.CategoryTotal;
import com.plainround.spend.domain.MonthlyTotals;
import com.plainround.spend.domain.ParsedRow;
import com.plainround.spend.domain.SpendingReport;
import com.plainround.spend.domain.Transaction;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `CategoryTotal` | `com.plainround.spend.domain.CategoryTotal` | this repository's own `CategoryTotal` (file `src/main/java/com/plainround/spend/domain/CategoryTotal.java`, explained in its own section) |
| `MonthlyTotals` | `com.plainround.spend.domain.MonthlyTotals` | this repository's own `MonthlyTotals` (file `src/main/java/com/plainround/spend/domain/MonthlyTotals.java`, explained in its own section) |
| `ParsedRow` | `com.plainround.spend.domain.ParsedRow` | this repository's own `ParsedRow` (file `src/main/java/com/plainround/spend/domain/ParsedRow.java`, explained in its own section) |
| `SpendingReport` | `com.plainround.spend.domain.SpendingReport` | this repository's own `SpendingReport` (file `src/main/java/com/plainround/spend/domain/SpendingReport.java`, explained in its own section) |
| `Transaction` | `com.plainround.spend.domain.Transaction` | this repository's own `Transaction` (file `src/main/java/com/plainround/spend/domain/Transaction.java`, explained in its own section) |
| `PrintStream` | `java.io.PrintStream` | an output stream with `print`, `println` and `printf` methods; `System.out` and `System.err` are print streams |
| `BigDecimal` | `java.math.BigDecimal` | a decimal number with exact digits, used for money |
| `RoundingMode` | `java.math.RoundingMode` | the choice of how to round a decimal number (for example half-even) |
| `Locale` | `java.util.Locale` | a language and region setting; fixing it makes number formatting identical on every machine |

```java
/**
 * Renders a {@link SpendingReport} as text. This is the only place money is rounded: once, at print
 * time, half-even, so that the rounding error of a long column of values does not lean one way.
 */
final class ReportPrinter {

  private final PrintStream out;
  private final int fractionDigits;
  private final String currencyCode;
```

Package-private and final. Three **private** fields: the `PrintStream out` where text goes (a `PrintStream` is an output stream with `println` and `printf`), the number of decimal places of the currency, and the currency code.

> New words:
>
> - **private**: visible only inside this class. Other code cannot touch it. `public` means visible everywhere; no word at all means visible only inside the same package.

```java
  ReportPrinter(PrintStream out, SpendingReport report) {
    this.out = out;
    this.fractionDigits = Math.max(report.currency().getDefaultFractionDigits(), 0);
    this.currencyCode = report.currency().getCurrencyCode();
  }
```

The constructor reads the currency's `getDefaultFractionDigits()` (2 for USD, 0 for JPY, -1 for currencies without a fixed number, hence `Math.max(..., 0)`) and its three-letter code, for example `USD`.

```java
  void print(SpendingReport report) {
    out.printf(
        "Spending report (%s): %d rows read, %d rejected%n",
        currencyCode, report.rowsRead(), report.rejectedCount());
    report.months().forEach(this::month);
    if (!report.largest().isEmpty()) {
      largest(report);
    }
    if (report.hasRejects()) {
      rejected(report);
    }
  }
```

Prints the header line with **printf format**: `%s` is the currency code, each `%d` a whole number, `%n` a line break. Then `report.months().forEach(this::month)` calls the `month` method for every month (a method reference). The two `if` statements print the largest purchases when there are any and the rejected rows when there are any.

> New words:
>
> - **printf format**: `out.printf("%d rows%n", 5)` prints a template in which `%d` is a whole number, `%s` text, `%n` a line break, `%10s` text padded to 10 characters on the left, `%-12s` padded on the right and `%,.2f` a number with thousands separators and two decimals.

```java
  private void month(MonthlyTotals month) {
    out.printf("%n%s%n", month.month());
    month.categories().forEach(this::categoryLine);
    out.printf("  %-12s%10s%n", "total", money(month.total()));
  }
```

Prints a blank line and the month (`%n%s%n`), one line per category through `categoryLine`, and the `total` line. `"  %-12s%10s%n"` means: two spaces, the first text left-aligned in 12 characters, the second right-aligned in 10; that is how the columns line up.

```java
  private void categoryLine(CategoryTotal category) {
    out.printf("  %-12s%10s%n", category.category(), money(category.total()));
  }
```

One category row with the same column format, the amount passed through `money`.

```java
  private void largest(SpendingReport report) {
    int count = report.largest().size();
    out.printf("%n%s largest purchases%n", count == 5 ? "Five" : String.valueOf(count));
    report.largest().forEach(this::purchaseLine);
  }
```

Prints the heading. A conditional expression writes `Five` when exactly five purchases are listed (the default) and the number otherwise, then one line for each purchase.

```java
  private void purchaseLine(Transaction transaction) {
    out.printf(
        "%12s  %s  %s (%s)%n",
        money(transaction.amount()),
        transaction.date(),
        transaction.description(),
        transaction.category());
  }
```

One purchase line: amount right-aligned in 12 characters, two spaces, the date, the description and the category in brackets.

```java
  private void rejected(SpendingReport report) {
    int shown = report.rejected().size();
    String suffix = shown < report.rejectedCount() ? ", showing the first " + shown : "";
    out.printf("%nRejected rows (%d%s)%n", report.rejectedCount(), suffix);
    for (ParsedRow.Rejected row : report.rejected()) {
      out.printf("  line %d: %s%n", row.lineNumber(), row.reason());
    }
  }
```

`shown` is how many rejects are stored; `suffix` becomes `, showing the first N` if fewer are shown than were rejected, otherwise empty. The heading prints the exact count, then a for-each loop loop prints `line N: reason` for each stored reject.

```java
  /** Rounds half-even to the currency's minor units, then formats with US grouping everywhere. */
  private String money(BigDecimal amount) {
    BigDecimal rounded = amount.setScale(fractionDigits, RoundingMode.HALF_EVEN);
    return String.format(Locale.US, "%,." + fractionDigits + "f", rounded);
  }
}
```

The only rounding in the program. `amount.setScale(fractionDigits, RoundingMode.HALF_EVEN)` rounds to the currency's decimal places with **half-even rounding**. `String.format(Locale.US, "%,." + fractionDigits + "f", rounded)` builds the template (for two digits, `%,.2f`) and prints the number with thousands separators and a dot as decimal point using **Locale** `Locale.US`, so the output is the same on every machine. The last `}` closes the class.

> New words:
>
> - **half-even rounding**: rounding where an exact tie goes to the nearest even digit (10.005 becomes 10.00, 10.015 becomes 10.02). Over many numbers it does not lean up or down.
> - **Locale**: a language and region. Number and date formatting depend on it (`1,204.50` in the US, `1.204,50` in Germany), so the code fixes it to `Locale.US` to print the same everywhere.

#### `src/main/java/com/plainround/spend/interfaces/cli/SpendCli.java`

The command line: it reads the arguments, wires the layers together, runs the use case, prints, and returns an **exit status**. It is in the `interfaces` layer, which may use every other layer; nothing uses it.

> New words:
>
> - **exit status**: the number a finished program hands back to the shell: 0 means success and any other number means something specific. A script reads it with `$?`.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.interfaces.cli;
import com.plainround.spend.application.BuildSpendingReport;
import com.plainround.spend.application.InputUnreadableException;
import com.plainround.spend.domain.SpendingReport;
import com.plainround.spend.infrastructure.Settings;
import com.plainround.spend.infrastructure.csv.CsvFileRowSource;
import com.plainround.spend.infrastructure.csv.CsvRowParser;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `BuildSpendingReport` | `com.plainround.spend.application.BuildSpendingReport` | this repository's own `BuildSpendingReport` (file `src/main/java/com/plainround/spend/application/BuildSpendingReport.java`, explained in its own section) |
| `InputUnreadableException` | `com.plainround.spend.application.InputUnreadableException` | this repository's own `InputUnreadableException` (file `src/main/java/com/plainround/spend/application/InputUnreadableException.java`, explained in its own section) |
| `SpendingReport` | `com.plainround.spend.domain.SpendingReport` | this repository's own `SpendingReport` (file `src/main/java/com/plainround/spend/domain/SpendingReport.java`, explained in its own section) |
| `Settings` | `com.plainround.spend.infrastructure.Settings` | this repository's own `Settings` (file `src/main/java/com/plainround/spend/infrastructure/Settings.java`, explained in its own section) |
| `CsvFileRowSource` | `com.plainround.spend.infrastructure.csv.CsvFileRowSource` | this repository's own `CsvFileRowSource` (file `src/main/java/com/plainround/spend/infrastructure/csv/CsvFileRowSource.java`, explained in its own section) |
| `CsvRowParser` | `com.plainround.spend.infrastructure.csv.CsvRowParser` | this repository's own `CsvRowParser` (file `src/main/java/com/plainround/spend/infrastructure/csv/CsvRowParser.java`, explained in its own section) |
| `PrintStream` | `java.io.PrintStream` | an output stream with `print`, `println` and `printf` methods; `System.out` and `System.err` are print streams |
| `Path` | `java.nio.file.Path` | a location of a file or folder |
| `List` | `java.util.List` | an ordered collection of elements |
| `Map` | `java.util.Map` | a collection of key-value pairs |
| `Logger` | `org.slf4j.Logger` | the logger type of the SLF4J logging interface |
| `LoggerFactory` | `org.slf4j.LoggerFactory` | creates loggers |

```java
/**
 * Command line entry point: {@code spend [--currency CODE] [--top N] FILE}. Results go to standard
 * output, logs to standard error. Exit status: 0 clean, 1 file unreadable, 2 some rows rejected, 64
 * wrong usage.
 */
public final class SpendCli {

  /** Exit status when every row was read. */
  public static final int EXIT_OK = 0;

  /** Exit status when the file as a whole could not be read. */
  public static final int EXIT_UNREADABLE = 1;

  /** Exit status when at least one row was rejected, so a script can notice. */
  public static final int EXIT_REJECTS = 2;

  /** Exit status for wrong command-line usage or configuration. */
  public static final int EXIT_USAGE = 64;

  private static final Logger LOG = LoggerFactory.getLogger(SpendCli.class);
  private static final String USAGE =
      "usage: spend [--currency CODE] [--top N] FILE   (env: SPEND_CURRENCY, SPEND_TOP,"
          + " SPEND_MAX_REJECTED, LOG_LEVEL)";

  private final Map<String, String> environment;
  private final PrintStream out;
  private final PrintStream err;
```

Four public constants name the exit statuses: `0` all rows read, `1` the file cannot be read, `2` at least one row rejected, `64` wrong usage (64 is the conventional code for a command-line usage error). `LOG` is a logger. `USAGE` is the help text; the `+` joins the two quoted pieces into one text. The three `private final` fields hold the environment variables and the two output streams, standard output and standard error (**standard output and standard error**).

> New words:
>
> - **standard output and standard error**: the two text streams every program has. Standard output (`System.out`) carries the program's result; standard error (`System.err`) carries diagnostics. In a terminal both show on screen but they can be separated.

```java
  SpendCli(Map<String, String> environment, PrintStream out, PrintStream err) {
    this.environment = environment;
    this.out = out;
    this.err = err;
  }
```

The constructor; it is package-private so only this class and the tests in the same package can create one.

```java
  /**
   * Runs the tool.
   *
   * @param args the command-line arguments
   * @param environment the environment variables
   * @param out where the report goes
   * @param err where usage errors go
   * @return the exit status
   */
  public static int run(
      List<String> args, Map<String, String> environment, PrintStream out, PrintStream err) {
    return new SpendCli(environment, out, err).execute(args);
  }
```

The testable entry: given the arguments and the streams it returns the exit status instead of ending the program. It creates an instance and calls `execute`.

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

The **main method** method, called by the JVM. `List.of(args)` turns the array of **command-line arguments** into a list, `System.getenv()` is the map of environment variables, `System.out` and `System.err` are the real output streams. `System.exit(status)` ends the process with the status. Only `main` calls `System.exit`; the logic is in `run`, so tests can check the number without stopping the test program.

> New words:
>
> - **main method**: the method the JVM calls to start a program: `public static void main(String[] args)`. `args` holds the words typed after the program name.
> - **command-line arguments**: the words typed after a program's name. Java hands them to `main` as an array of strings.

```java
  private int execute(List<String> args) {
    if (args.contains("--help") || args.contains("-h")) {
      out.println(USAGE);
      return EXIT_OK;
    }
    try {
      Arguments arguments = Arguments.parse(args, Settings.fromEnvironment(environment));
      return report(arguments);
    } catch (IllegalArgumentException problem) {
      err.println("spend: " + problem.getMessage());
      err.println(USAGE);
      return EXIT_USAGE;
    }
  }
```

If the arguments contain `--help` or `-h`, print the usage on standard output and return 0. Otherwise a try/catch: parse the arguments together with the settings read from the environment, then run `report`. An `IllegalArgumentException` (a bad option, a bad number, a missing file name, or a bad environment value) is reported on standard error with the usage line and the status 64.

```java
  private int report(Arguments arguments) {
    Settings settings = arguments.settings();
    CsvRowParser parser = new CsvRowParser(settings.reportCurrency());
    CsvFileRowSource source = new CsvFileRowSource(arguments.file(), parser);
    try {
      SpendingReport report =
          new BuildSpendingReport(
                  source,
                  settings.reportCurrency(),
                  settings.largestCount(),
                  settings.maxRejectedKept())
              .run();
      new ReportPrinter(out, report).print(report);
      LOG.info("report done rows={} rejected={}", report.rowsRead(), report.rejectedCount());
      return report.hasRejects() ? EXIT_REJECTS : EXIT_OK;
    } catch (InputUnreadableException problem) {
      err.println("spend: " + problem.getMessage());
      return EXIT_UNREADABLE;
    }
  }
```

Wires the layers: the parser for the report currency, the file source for the file, then the use case `BuildSpendingReport` (the long expression ends with `.run()`), then the printer prints the report, a log line records the counts, and the status is 2 if there were rejects, else 0 (a conditional expression). If the source throws `InputUnreadableException`, a short message goes to standard error and the status is 1; no stack trace appears.

```java
  /** The parsed command line. */
  private record Arguments(Path file, Settings settings) {

    static Arguments parse(List<String> args, Settings defaults) {
      Settings settings = defaults;
      Path file = null;
      for (int index = 0; index < args.size(); index++) {
        String argument = args.get(index);
        switch (argument) {
          case "--currency" -> settings = settings.withCurrency(value(args, ++index, argument));
          case "--top" ->
              settings = settings.withLargestCount(count(value(args, ++index, argument)));
          default -> file = single(file, argument);
        }
      }
      if (file == null) {
        throw new IllegalArgumentException("missing FILE");
      }
      return new Arguments(file, settings);
    }
```

A private record nested inside the class: the parsed command line, a file and the settings. The static method `parse` loops over the arguments with an index. `switch (argument)` is a **switch with arrows** over text: `--currency` replaces the settings with a copy using the next argument as the currency code (`++index` moves to the next argument first), `--top` does the same for the count, and `default` means "anything else": it must be the file name. If no file was given the method throws `missing FILE`.

> New words:
>
> - **switch with arrows**: a `switch` whose cases are written `case "x" -> action;`. Only the matching case runs; there is no fall-through to the next one.

```java
    private static String value(List<String> args, int index, String option) {
      if (index >= args.size()) {
        throw new IllegalArgumentException(option + " needs a value");
      }
      return args.get(index);
    }
```

Returns the argument after an option, or throws `... needs a value` if the command line ends there.

```java
    private static int count(String text) {
      try {
        return Integer.parseInt(text);
      } catch (NumberFormatException problem) {
        throw new IllegalArgumentException("--top is not a whole number: '" + text + "'", problem);
      }
    }
```

Converts the `--top` argument to a number or throws an error that says it is not a whole number.

```java
    private static Path single(Path current, String argument) {
      if (current != null || argument.startsWith("--")) {
        throw new IllegalArgumentException("unexpected argument '" + argument + "'");
      }
      return Path.of(argument);
    }
  }
}
```

Accepts the file name: it refuses a second file name (`current != null`) and anything that starts with `--` (an unknown option), otherwise returns `Path.of(argument)`. The last two `}` close the nested record and the class.

### Part B. The tests

#### `src/test/java/com/plainround/spend/ArchitectureTest.java`

Tests the *shape* of the code, not what it computes. It uses ArchUnit to read the compiled classes and fail the build if a package uses a package it should not. This is how the layer rules in ARCHITECTURE.md are enforced by a machine and not by good intentions.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `noClasses` (static) | `com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses` | starts a rule of the form "no classes that ... should ..." |
| `layeredArchitecture` (static) | `com.tngtech.archunit.library.Architectures.layeredArchitecture` | starts a layered-architecture rule |
| `ImportOption` | `com.tngtech.archunit.core.importer.ImportOption` | options that say which compiled classes ArchUnit should read (for example: leave out the tests) |
| `AnalyzeClasses` | `com.tngtech.archunit.junit.AnalyzeClasses` | an annotation on a test class that tells ArchUnit which packages to analyze |
| `ArchTest` | `com.tngtech.archunit.junit.ArchTest` | an annotation that marks a field holding an architecture rule so JUnit runs it as a test |
| `ArchRule` | `com.tngtech.archunit.lang.ArchRule` | the type of one architecture rule |

```java
/** The dependency rule from the README's "Project structure", enforced on every test run. */
@AnalyzeClasses(
    packages = "com.plainround.spend",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
```

`@AnalyzeClasses` is an **annotation** on the class. `packages` says which compiled classes ArchUnit reads (everything under `com.plainround.spend`), and `importOptions = ImportOption.DoNotIncludeTests.class` leaves the test classes out, so only the real program is judged. The class is package-private (no `public`), which JUnit allows.

> New words:
>
> - **annotation**: a label written with `@` before a declaration, such as `@Override` or `@Test`. Tools and the compiler read it; it does not change what the code does by itself.

```java
  private static final String DOMAIN = "com.plainround.spend.domain..";
  private static final String APPLICATION = "com.plainround.spend.application..";
  private static final String INFRASTRUCTURE = "com.plainround.spend.infrastructure..";
  private static final String INTERFACES = "com.plainround.spend.interfaces..";
```

Four static final texts name the layers as ArchUnit package patterns. The two dots at the end (`domain..`) mean "this package and every package below it".

```java
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
```

`@ArchTest` marks a *field* whose value is an **architecture rule**; ArchUnit runs each such field as a test. `layeredArchitecture()` starts a rule about layers. `consideringOnlyDependenciesInLayers()` means classes that belong to none of the four layers (the JDK, SLF4J) are ignored. Each `.layer("name").definedBy(PATTERN)` names a layer and says which packages belong to it. Then four `whereLayer` clauses give the access rules: nothing may use `interfaces`; only `interfaces` may use `infrastructure`; only `infrastructure` and `interfaces` may use `application`; `domain` may be used by all three outer layers. Read together: dependencies point inward, never outward. The long chain is one expression; each method returns the rule being built, so the calls are joined by dots.

> New words:
>
> - **architecture rule**: a test of the code's structure instead of its behavior: ArchUnit reads the compiled classes, builds a graph of which class uses which, and fails when a rule such as "the domain package must not use the infrastructure package" is broken.

```java
  @ArchTest
  static final ArchRule domainUsesOnlyTheJdk =
      noClasses()
          .that()
          .resideInAPackage(DOMAIN)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(APPLICATION, INFRASTRUCTURE, INTERFACES, "org.slf4j..", "ch.qos..");
```

`noClasses().that().resideInAPackage(DOMAIN).should().dependOnClassesThat().resideInAnyPackage(...)` reads almost as a sentence: no class in the domain may depend on a class in any of the listed packages, which are the three outer layers plus SLF4J (`org.slf4j..`) and Logback (`ch.qos..`). The domain therefore cannot log or know about files, and stays testable with no setup.

```java
  @ArchTest
  static final ArchRule applicationDoesNotKnowTheFileFormat =
      noClasses()
          .that()
          .resideInAPackage(APPLICATION)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("java.nio.file..", "java.io..");
}
```

The use-case layer may not use `java.nio.file..` or `java.io..`, the packages for files and streams. If someone writes file code into `application`, this test fails; file reading belongs to the adapter in `infrastructure`. The last `}` closes the class.

#### `src/test/java/com/plainround/spend/application/BuildSpendingReportTest.java`

Tests the use case alone. It gives the use case a **fake** source with two prepared rows, with no file at all, and checks that the report counts them.

> New words:
>
> - **fake**: a small stand-in written for a test, such as an in-memory port (in the design sense) implementation. It lets the test run one class on its own, with no file, network or clock.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.application;
import static org.assertj.core.api.Assertions.assertThat;
import com.plainround.spend.domain.ParsedRow;
import com.plainround.spend.domain.SpendingReport;
import com.plainround.spend.domain.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `assertThat` (static) | `org.assertj.core.api.Assertions.assertThat` | the entry point of every AssertJ assertion |
| `ParsedRow` | `com.plainround.spend.domain.ParsedRow` | this repository's own `ParsedRow` (file `src/main/java/com/plainround/spend/domain/ParsedRow.java`, explained in its own section) |
| `SpendingReport` | `com.plainround.spend.domain.SpendingReport` | this repository's own `SpendingReport` (file `src/main/java/com/plainround/spend/domain/SpendingReport.java`, explained in its own section) |
| `Transaction` | `com.plainround.spend.domain.Transaction` | this repository's own `Transaction` (file `src/main/java/com/plainround/spend/domain/Transaction.java`, explained in its own section) |
| `BigDecimal` | `java.math.BigDecimal` | a decimal number with exact digits, used for money |
| `LocalDate` | `java.time.LocalDate` | a calendar date without a time or a time zone |
| `Currency` | `java.util.Currency` | an ISO 4217 currency such as USD, with its number of minor-unit digits |
| `List` | `java.util.List` | an ordered collection of elements |
| `Test` | `org.junit.jupiter.api.Test` | marks a method as a test |

```java
class BuildSpendingReportTest {

  private static final Currency USD = Currency.getInstance("USD");
```

`USD` is a constant: `Currency.getInstance("USD")` looks up the currency object for the code.

```java
  /** In-memory fake of the port; no file is touched. */
  private static RowSource rows(ParsedRow... rows) {
    return consumer -> List.of(rows).forEach(consumer);
  }
```

A helper that builds the fake port (in the design sense) implementation. `ParsedRow... rows` is **varargs**: the caller lists rows and Java makes an array. The returned value is a lambda, `consumer -> List.of(rows).forEach(consumer)`: because `RowSource` has exactly one method, a lambda can stand for an object that implements it; the body hands every given row to the consumer, which is what a real file reader does.

> New words:
>
> - **varargs**: a parameter written `Type... name` accepts any number of arguments of that type, including none; inside the method it is an array. Callers write `rows(a, b)` and Java builds the array.

```java
  @Test
  void everyRowFromTheSourceReachesTheReport() {
    ParsedRow good =
        new ParsedRow.Ok(
            new Transaction(LocalDate.of(2026, 3, 1), "Cafe", new BigDecimal("3.20"), USD, "Food"));
    ParsedRow bad = new ParsedRow.Rejected(3, "x", "broken");

    SpendingReport report = new BuildSpendingReport(rows(good, bad), USD, 5, 10).run();

    assertThat(report.rowsRead()).isEqualTo(1);
    assertThat(report.rejectedCount()).isEqualTo(1);
    assertThat(report.currency()).isEqualTo(USD);
  }
}
```

A **test method**. `good` is an `Ok` holding a transaction made with `new Transaction(...)`; `LocalDate.of(2026, 3, 1)` is 1 March 2026 and `new BigDecimal("3.20")` the exact amount. `bad` is a `Rejected` for line 3. `new BuildSpendingReport(rows(good, bad), USD, 5, 10).run()` builds the use case with the fake and runs it. Three **assertion**s follow: one good row read, one rejected, currency USD. Writing the test first on the use case proves that it works with any source, not only files.

> New words:
>
> - **test method**: a method marked `@Test`. The test runner (JUnit) finds every such method, runs each one on a fresh object of the test class, and reports it as passed if it finishes without an error and failed if it throws one. The method name is a sentence that says what must be true.
> - **assertion**: a statement of what must be true. `assertThat(actual).isEqualTo(expected)` (from the library AssertJ) does nothing when the two match and throws an `AssertionError` with a readable message when they do not, which makes the test fail.

#### `src/test/java/com/plainround/spend/domain/SpendingAccumulatorTest.java`

The most important test class: it pins down the arithmetic, the ordering rules and the limits of the accumulator. Each test builds a small accumulator, feeds it a few rows and checks the report.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.domain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import org.junit.jupiter.api.Test;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `assertThat` (static) | `org.assertj.core.api.Assertions.assertThat` | the entry point of every AssertJ assertion |
| `assertThatThrownBy` (static) | `org.assertj.core.api.Assertions.assertThatThrownBy` | runs code and asserts that it throws |
| `BigDecimal` | `java.math.BigDecimal` | a decimal number with exact digits, used for money |
| `LocalDate` | `java.time.LocalDate` | a calendar date without a time or a time zone |
| `YearMonth` | `java.time.YearMonth` | a year and a month, such as 2026-01 |
| `Currency` | `java.util.Currency` | an ISO 4217 currency such as USD, with its number of minor-unit digits |
| `Test` | `org.junit.jupiter.api.Test` | marks a method as a test |

```java
class SpendingAccumulatorTest {

  private static final Currency USD = Currency.getInstance("USD");
```

The constant `USD` is shared by all tests of the class.

```java
  private static ParsedRow ok(String date, String description, String amount, String category) {
    return new ParsedRow.Ok(
        new Transaction(LocalDate.parse(date), description, new BigDecimal(amount), USD, category));
  }
```

Helper that builds a good row from four short texts, so each test line stays one line. `LocalDate.parse(date)` reads the ISO form; `new BigDecimal(amount)` keeps the digits exactly.

```java
  private static ParsedRow rejected(int line) {
    return new ParsedRow.Rejected(line, "raw", "reason " + line);
  }
```

Helper that builds a rejected row for a given line number with a reason `"reason " + line` (joining text and number with `+`).

```java
  @Test
  void tenCentsPlusTwentyCentsIsExactlyThirtyCents() {
    SpendingAccumulator accumulator = new SpendingAccumulator(USD, 5, 10);
    accumulator.accept(ok("2026-01-01", "a", "0.10", "Misc"));
    accumulator.accept(ok("2026-01-02", "b", "0.20", "Misc"));

    BigDecimal total = accumulator.report().months().getFirst().total();

    assertThat(total).isEqualByComparingTo("0.30");
  }
```

The lesson's first money bug as a test method. In `double` arithmetic `0.10 + 0.20` is `0.30000000000000004`. Here two rows of 0.10 and 0.20 are accepted, and `report().months().getFirst().total()` (the first month's sum) is compared with `isEqualByComparingTo("0.30")`. That assertion compares numeric value, so `0.3` and `0.30` count as equal; plain `isEqualTo` on a BigDecimal would treat different scales as different.

```java
  @Test
  void totalsAreGroupedPerMonthAndSortedLargestCategoryFirst() {
    SpendingAccumulator accumulator = new SpendingAccumulator(USD, 5, 10);
    accumulator.accept(ok("2026-02-01", "a", "5.00", "Books"));
    accumulator.accept(ok("2026-01-01", "b", "1.00", "Books"));
    accumulator.accept(ok("2026-01-02", "c", "9.00", "Travel"));
    accumulator.accept(ok("2026-01-03", "d", "2.50", "Books"));

    SpendingReport report = accumulator.report();

    assertThat(report.months())
        .extracting(MonthlyTotals::month)
        .containsExactly(YearMonth.of(2026, 1), YearMonth.of(2026, 2));
    assertThat(report.months().getFirst().categories())
        .extracting(CategoryTotal::category)
        .containsExactly("Travel", "Books");
    assertThat(report.months().getFirst().total()).isEqualByComparingTo("12.50");
  }
```

Four rows over two months. `extracting(MonthlyTotals::month)` takes the `month` value of each element of the list (a method reference) and `containsExactly(...)` checks the result has exactly those values *in that order*: January then February. The second check: in January Travel (9.00) comes before Books (1.00 + 2.50 = 3.50), largest first. The third: the January total is 12.50.

```java
  @Test
  void equalCategoryTotalsAreOrderedByName() {
    SpendingAccumulator accumulator = new SpendingAccumulator(USD, 5, 10);
    accumulator.accept(ok("2026-01-01", "a", "3.00", "Zoo"));
    accumulator.accept(ok("2026-01-02", "b", "3.00", "Art"));

    assertThat(accumulator.report().months().getFirst().categories())
        .extracting(CategoryTotal::category)
        .containsExactly("Art", "Zoo");
  }
```

Two categories with the same total, 3.00. The sort has a tie-break by name, so the order is `Art`, `Zoo` on every run, never random.

```java
  @Test
  void onlyTheLargestPurchasesAreKeptBiggestFirstWithTiesByEarlierDate() {
    SpendingAccumulator accumulator = new SpendingAccumulator(USD, 2, 10);
    accumulator.accept(ok("2026-01-05", "late tie", "50.00", "A"));
    accumulator.accept(ok("2026-01-01", "small", "1.00", "A"));
    accumulator.accept(ok("2026-01-02", "early tie", "50.00", "A"));
    accumulator.accept(ok("2026-01-03", "huge", "99.00", "A"));

    assertThat(accumulator.report().largest())
        .extracting(Transaction::description)
        .containsExactly("huge", "early tie");
  }
```

The accumulator keeps only 2 purchases. Four rows are added, among them two of 50.00. The result must be `huge` (99.00) then `early tie` (the 50.00 one with the earlier date). It proves both the limit and the tie rule of the comparator.

```java
  @Test
  void rejectedRowsAreCountedAndOnlyTheFirstOnesKept() {
    SpendingAccumulator accumulator = new SpendingAccumulator(USD, 5, 2);
    accumulator.accept(rejected(2));
    accumulator.accept(rejected(3));
    accumulator.accept(rejected(4));

    SpendingReport report = accumulator.report();

    assertThat(report.rejectedCount()).isEqualTo(3);
    assertThat(report.rejected()).extracting(ParsedRow.Rejected::lineNumber).containsExactly(2, 3);
    assertThat(report.hasRejects()).isTrue();
  }
```

Keeps at most 2 rejects; three are fed. `rejectedCount()` is 3 (exact), the kept list holds lines 2 and 3 (`extracting(ParsedRow.Rejected::lineNumber)`), and `hasRejects()` is `isTrue()`.

```java
  @Test
  void anEmptyRunHasNoRejectsAndNoMonths() {
    SpendingReport report = new SpendingAccumulator(USD, 5, 10).report();

    assertThat(report.hasRejects()).isFalse();
    assertThat(report.months()).isEmpty();
    assertThat(report.rowsRead()).isZero();
  }
```

A report built with no input at all: `isFalse()`, `isEmpty()` and `isZero()` are the matching assertions for a boolean, a list and a number. It proves an empty file is a valid input.

```java
  @Test
  void invalidLimitsAreRefused() {
    assertThatThrownBy(() -> new SpendingAccumulator(USD, 0, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new SpendingAccumulator(USD, 5, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }
```

Uses **expected failure**: a largest-count of 0 and a rejected-keep of -1 must each throw `IllegalArgumentException`, which the constructor does.

> New words:
>
> - **expected failure**: `assertThatThrownBy(() -> code)` runs the code inside the lambda and checks that it throws; the next calls on the result check the type and the message of the error. A test needs this to prove that bad input is refused.

```java
  @Test
  void reportListsAreImmutableCopies() {
    SpendingReport report = new SpendingAccumulator(USD, 5, 10).report();

    assertThatThrownBy(() -> report.months().add(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
```

Tries to `add(null)` to the report's month list and expects `UnsupportedOperationException`, the error an immutable list throws. This proves the `List.copyOf` in the record's constructor. The last `}` closes the class.

#### `src/test/java/com/plainround/spend/domain/TransactionTest.java`

Checks every validation rule of `Transaction`, one test for each, with the exact messages.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.domain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import org.junit.jupiter.api.Test;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `assertThat` (static) | `org.assertj.core.api.Assertions.assertThat` | the entry point of every AssertJ assertion |
| `assertThatThrownBy` (static) | `org.assertj.core.api.Assertions.assertThatThrownBy` | runs code and asserts that it throws |
| `BigDecimal` | `java.math.BigDecimal` | a decimal number with exact digits, used for money |
| `LocalDate` | `java.time.LocalDate` | a calendar date without a time or a time zone |
| `Currency` | `java.util.Currency` | an ISO 4217 currency such as USD, with its number of minor-unit digits |
| `Test` | `org.junit.jupiter.api.Test` | marks a method as a test |

```java
class TransactionTest {

  private static final Currency USD = Currency.getInstance("USD");
  private static final LocalDate DAY = LocalDate.of(2026, 1, 3);
```

Two shared constants: `USD` and `DAY` (3 January 2026).

```java
  @Test
  void aNegativeAmountIsRejectedWithAMessageNamingTheField() {
    assertThatThrownBy(() -> new Transaction(DAY, "Ride", new BigDecimal("-8.00"), USD, "Travel"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("amount must not be null or negative: -8.00");
  }
```

expected failure: creating a transaction with -8.00 must throw `IllegalArgumentException` whose message is exactly `amount must not be null or negative: -8.00`. `hasMessage` compares the whole text.

```java
  @Test
  void aNullAmountIsRejectedWithoutANullPointerException() {
    assertThatThrownBy(() -> new Transaction(DAY, "Ride", null, USD, "Travel"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("amount");
  }
```

Passes null as the amount. Without the explicit check in the constructor, the code would call a method on null and crash with `NullPointerException`, a confusing error. The test demands `IllegalArgumentException` whose message starts with the word `amount`.

```java
  @Test
  void aBlankCategoryIsRejectedEvenWhenItIsOnlySpaces() {
    assertThatThrownBy(() -> new Transaction(DAY, "Ride", BigDecimal.ONE, USD, "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("category must not be blank");
  }
```

A category of three spaces counts as blank, because the constructor uses `isBlank()` and not `isEmpty()`.

```java
  @Test
  void aNullCategoryIsRejected() {
    assertThatThrownBy(() -> new Transaction(DAY, "Ride", BigDecimal.ONE, USD, null))
        .hasMessageStartingWith("category");
  }
```

Null category: the message must start with `category`.

```java
  @Test
  void aNullDateIsRejected() {
    assertThatThrownBy(() -> new Transaction(null, "Ride", BigDecimal.ONE, USD, "Travel"))
        .hasMessage("date must not be null");
  }
```

Null date: the message is exactly `date must not be null`.

```java
  @Test
  void aNullCurrencyIsRejected() {
    assertThatThrownBy(() -> new Transaction(DAY, "Ride", BigDecimal.ONE, null, "Travel"))
        .hasMessage("currency must not be null");
  }
```

Null currency: the message is exactly `currency must not be null`.

```java
  @Test
  void aZeroAmountIsAllowedBecauseAWaivedFeeIsStillARow() {
    Transaction waived = new Transaction(DAY, "Fee", BigDecimal.ZERO, USD, "Fees");

    assertThat(waived.amount()).isEqualByComparingTo("0");
  }
}
```

The only test where creation must *succeed*: zero is a valid amount. `BigDecimal.ZERO` is the built-in constant 0. The last `}` closes the class.

#### `src/test/java/com/plainround/spend/infrastructure/SettingsTest.java`

Tests how environment variables become settings, using an ordinary `Map` in place of the real environment.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.infrastructure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.Map;
import org.junit.jupiter.api.Test;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `assertThat` (static) | `org.assertj.core.api.Assertions.assertThat` | the entry point of every AssertJ assertion |
| `assertThatThrownBy` (static) | `org.assertj.core.api.Assertions.assertThatThrownBy` | runs code and asserts that it throws |
| `Map` | `java.util.Map` | a collection of key-value pairs |
| `Test` | `org.junit.jupiter.api.Test` | marks a method as a test |

```java
class SettingsTest {
```

No fields; each test builds its own map.

```java
  @Test
  void defaultsAreUsdTopFiveAndAThousandKeptRejects() {
    Settings settings = Settings.fromEnvironment(Map.of());

    assertThat(settings.reportCurrency().getCurrencyCode()).isEqualTo("USD");
    assertThat(settings.largestCount()).isEqualTo(5);
    assertThat(settings.maxRejectedKept()).isEqualTo(1000);
  }
```

`Map.of()` is an empty map: no variable set. The defaults must be USD, 5 and 1000. `getCurrencyCode()` returns the three-letter text.

```java
  @Test
  void environmentVariablesOverrideTheDefaults() {
    Settings settings =
        Settings.fromEnvironment(
            Map.of("SPEND_CURRENCY", "EUR", "SPEND_TOP", " 3 ", "SPEND_MAX_REJECTED", "0"));

    assertThat(settings.reportCurrency().getCurrencyCode()).isEqualTo("EUR");
    assertThat(settings.largestCount()).isEqualTo(3);
    assertThat(settings.maxRejectedKept()).isZero();
  }
```

`Map.of("SPEND_CURRENCY", "EUR", "SPEND_TOP", " 3 ", "SPEND_MAX_REJECTED", "0")` is a map with three entries (key, value, key, value...). The number ` 3 ` has spaces around it on purpose: the code trims them. Zero rejects is valid.

```java
  @Test
  void malformedValuesFailWithTheVariableName() {
    assertThatThrownBy(() -> Settings.fromEnvironment(Map.of("SPEND_TOP", "many")))
        .hasMessageContaining("SPEND_TOP");
    assertThatThrownBy(() -> Settings.fromEnvironment(Map.of("SPEND_CURRENCY", "US$")))
        .hasMessageContaining("US$");
    assertThatThrownBy(() -> Settings.fromEnvironment(Map.of("SPEND_TOP", "0")))
        .hasMessageContaining("SPEND_TOP");
    assertThatThrownBy(() -> Settings.fromEnvironment(Map.of("SPEND_MAX_REJECTED", "-1")))
        .hasMessageContaining("SPEND_MAX_REJECTED");
  }
```

Four expected failure checks for four bad values. `hasMessageContaining(...)` checks that the error text includes the given words, so a person who sees the message knows which setting is wrong: `many` is not a number, `US$` is not a currency, `0` and `-1` are out of range.

```java
  @Test
  void withersReplaceOneValue() {
    Settings base = Settings.fromEnvironment(Map.of());

    assertThat(base.withCurrency("JPY").reportCurrency().getCurrencyCode()).isEqualTo("JPY");
    assertThat(base.withLargestCount(9).largestCount()).isEqualTo(9);
  }
}
```

Checks the two **"with" method** methods: the copy has the new currency or count. The last `}` closes the class.

> New words:
>
> - **"with" method**: a method that returns a *copy* of an unchangeable object with one value replaced (`settings.withCurrency("JPY")`), because a record cannot be changed in place.

#### `src/test/java/com/plainround/spend/infrastructure/csv/CsvFileRowSourceTest.java`

Tests the real file adapter against real files, written into a temporary folder.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.infrastructure.csv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.plainround.spend.application.InputUnreadableException;
import com.plainround.spend.domain.ParsedRow;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `assertThat` (static) | `org.assertj.core.api.Assertions.assertThat` | the entry point of every AssertJ assertion |
| `assertThatThrownBy` (static) | `org.assertj.core.api.Assertions.assertThatThrownBy` | runs code and asserts that it throws |
| `InputUnreadableException` | `com.plainround.spend.application.InputUnreadableException` | this repository's own `InputUnreadableException` (file `src/main/java/com/plainround/spend/application/InputUnreadableException.java`, explained in its own section) |
| `ParsedRow` | `com.plainround.spend.domain.ParsedRow` | this repository's own `ParsedRow` (file `src/main/java/com/plainround/spend/domain/ParsedRow.java`, explained in its own section) |
| `IOException` | `java.io.IOException` | the standard error type for failures while reading or writing (a file, a network connection) |
| `StandardCharsets` | `java.nio.charset.StandardCharsets` | constants for the standard character encodings, such as UTF-8 |
| `Files` | `java.nio.file.Files` | static helper methods for reading and writing files |
| `Path` | `java.nio.file.Path` | a location of a file or folder |
| `ArrayList` | `java.util.ArrayList` | a growable list backed by an array |
| `Currency` | `java.util.Currency` | an ISO 4217 currency such as USD, with its number of minor-unit digits |
| `List` | `java.util.List` | an ordered collection of elements |
| `Test` | `org.junit.jupiter.api.Test` | marks a method as a test |
| `TempDir` | `org.junit.jupiter.api.io.TempDir` | marks a field or parameter that JUnit fills with a fresh temporary folder |

```java
class CsvFileRowSourceTest {

  private static final String HEADER = "date,description,amount,currency,category\n";

  @TempDir Path folder;
```

`HEADER` is the first line of every test file. `@TempDir Path folder;` is a **temporary folder** injected by JUnit.

> New words:
>
> - **temporary folder**: a folder that JUnit creates fresh for each test and deletes afterwards. A field marked `@TempDir` receives it. Tests write files there so they never touch real data and never leave anything behind.

```java
  private List<ParsedRow> read(Path file) {
    List<ParsedRow> rows = new ArrayList<>();
    new CsvFileRowSource(file, new CsvRowParser(Currency.getInstance("USD"))).readRows(rows::add);
    return rows;
  }
```

Helper: creates the source with a parser for USD and reads every row into a list. `rows::add` is a method reference that serves as the consumer, so each row is appended to the list.

```java
  private Path write(byte[] content) throws IOException {
    return Files.write(folder.resolve("input.csv"), content);
  }
```

Helper: writes a **byte array** into a file called `input.csv` inside the temporary folder and returns its path. `throws IOException` is the **throws clause** clause.

> New words:
>
> - **byte array**: `byte[]` is a fixed-length sequence of bytes, the raw 8-bit values a file is made of. A string becomes bytes through a character encoding such as UTF-8.
> - **throws clause**: `throws IOException` after a method's parameter list says the method may fail with that checked error. Java makes the caller either handle it or declare it as well, so a test that touches files declares it and lets the test runner report the failure.

```java
  @Test
  void aByteOrderMarkAndAnAccentedDescriptionBothReadCorrectly() throws IOException {
    String text = "﻿" + HEADER + "2026-03-01,Café Noir,3.20,USD,Eating Out\n";

    List<ParsedRow> rows = read(write(text.getBytes(StandardCharsets.UTF_8)));

    assertThat(rows)
        .singleElement()
        .isInstanceOfSatisfying(
            ParsedRow.Ok.class,
            ok -> assertThat(ok.transaction().description()).isEqualTo("Café Noir"));
  }
```

The text begins with the invisible byte order mark character (U+FEFF, typed here as a literal invisible character) and has the description `Café Noir` with an accented letter. `text.getBytes(StandardCharsets.UTF_8)` turns it into bytes the way a real file would hold it. Result: exactly one row (`singleElement()`) that is an `Ok` (`isInstanceOfSatisfying` checks the type and then runs the given check with the typed variable `ok`) and whose description equals `Café Noir`. If the mark were not removed, the date of line 1 would not parse.

```java
  @Test
  void lineNumbersCountTheHeaderAndBlankLinesAreSkipped() throws IOException {
    String text = HEADER + "\n2026-03-01,Cafe,3.20,USD,Food\nbroken line\n";

    List<ParsedRow> rows = read(write(text.getBytes(StandardCharsets.UTF_8)));

    assertThat(rows).hasSize(2);
    assertThat(rows.get(1))
        .isInstanceOfSatisfying(
            ParsedRow.Rejected.class, rejected -> assertThat(rejected.lineNumber()).isEqualTo(4));
  }
```

The file has a header, a blank line, a good row and a broken line. Two rows come out (the blank line is skipped), and the second is a `Rejected` with line number 4: the header is line 1, the blank line is 2, the good row is 3, the broken line is 4.

```java
  @Test
  void windowsLineEndingsAreAccepted() throws IOException {
    String text = HEADER.replace("\n", "\r\n") + "2026-03-01,Cafe,3.20,USD,Food\r\n";

    assertThat(read(write(text.getBytes(StandardCharsets.UTF_8))))
        .singleElement()
        .isInstanceOf(ParsedRow.Ok.class);
  }
```

Replaces every `\n` by `\r\n` (the Windows line ending) and checks the row still parses. Java's line reader accepts both.

```java
  @Test
  void aMissingFileIsReportedAsAnUnreadableInputNotAStackTrace() {
    assertThatThrownBy(() -> read(folder.resolve("nope.csv")))
        .isInstanceOf(InputUnreadableException.class)
        .hasMessageContaining("cannot read")
        .hasMessageContaining("NoSuchFileException");
  }
```

Reads a file that does not exist. The expected error is `InputUnreadableException` (not the raw system error) with a message containing `cannot read` and the system's reason `NoSuchFileException`.

```java
  @Test
  void bytesThatAreNotUtf8AreReportedAsAnUnreadableFile() throws IOException {
    byte[] invalid = new byte[] {'d', 'a', 't', 'e', '\n', (byte) 0xC3, (byte) 0x28, '\n'};

    Path file = write(invalid);

    assertThatThrownBy(() -> read(file))
        .isInstanceOf(InputUnreadableException.class)
        .hasMessageContaining("not valid UTF-8");
  }
}
```

`new byte[] {'d', 'a', 't', 'e', '\n', (byte) 0xC3, (byte) 0x28, '\n'}` builds an array: the letters of `date`, a line break, then two bytes `C3 28`, which is not valid UTF-8 (C3 must be followed by a byte from 80 to BF). `(byte)` converts the number to a byte. The expected message contains `not valid UTF-8`. The last `}` closes the class.

#### `src/test/java/com/plainround/spend/infrastructure/csv/CsvRowParserEdgeCasesTest.java`

The edge cases of row parsing. The first test is a table: each row of the table is one input line and the exact reason it must be rejected with. These are the planted bad rows of the lesson's sample file.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.infrastructure.csv;
import static org.assertj.core.api.Assertions.assertThat;
import com.plainround.spend.domain.ParsedRow;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `assertThat` (static) | `org.assertj.core.api.Assertions.assertThat` | the entry point of every AssertJ assertion |
| `ParsedRow` | `com.plainround.spend.domain.ParsedRow` | this repository's own `ParsedRow` (file `src/main/java/com/plainround/spend/domain/ParsedRow.java`, explained in its own section) |
| `Currency` | `java.util.Currency` | an ISO 4217 currency such as USD, with its number of minor-unit digits |
| `Test` | `org.junit.jupiter.api.Test` | marks a method as a test |
| `ParameterizedTest` | `org.junit.jupiter.params.ParameterizedTest` | marks a test that runs once for each set of arguments |
| `CsvSource` | `org.junit.jupiter.params.provider.CsvSource` | supplies a parameterized test with rows of comma-separated arguments written in the annotation |

```java
class CsvRowParserEdgeCasesTest {

  private final CsvRowParser parser = new CsvRowParser(Currency.getInstance("USD"));
```

`parser` is a field set once for all tests of the class (a new test object is made for each test, so each test gets its own parser).

```java
  private String reasonFor(String line) {
    return ((ParsedRow.Rejected) parser.parse(9, line)).reason();
  }

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
```

Helper: parse the line as line 9, **cast** the result to `Rejected` and return its reason. If the line turned out to be good, the cast would fail and the test would fail with a clear error.

> New words:
>
> - **cast**: writing `(Type) value` to tell Java to treat a value as a more specific type. If the value is not of that type at run time, Java throws `ClassCastException`.

```java
  void everyPlantedBadRowOfTheLessonGetsAnActionableReason(String line, String reason) {
    assertThat(reasonFor(line)).isEqualTo(reason);
  }
```

A **parameterized test** test. `@CsvSource(delimiter = '|', value = {...})` gives seven rows; each row has two values separated by `|`: the input line and the expected reason. (The pipe is used as the separator because the input lines themselves contain commas.) JUnit calls the method once per row with `line` and `reason`. The seven cases: a negative amount, a missing category, a word where a number belongs, six columns because of an unquoted thousands comma, a month 13, a currency `US$`, an empty amount.

> New words:
>
> - **parameterized test**: one test method run once for every row of a table of inputs. `@ParameterizedTest` says so, and `@CsvSource` supplies the rows; each row's values are passed as the method's arguments. The test report shows each row as its own test.

```java
  @Test
  void anExponentAmountIsNotANumberSoItCannotBlowUpMemory() {
    assertThat(reasonFor("2026-01-01,Odd,1E+999999999,USD,Misc"))
        .isEqualTo("amount is not a number: '1E+999999999'");
  }
```

`1E+999999999` means 1 times ten to the power 999,999,999. `BigDecimal` would accept it and later try to use that huge exponent. The regex check makes the parser reject it with `amount is not a number`.

```java
  @Test
  void aCategoryOfSpacesIsBlank() {
    assertThat(reasonFor("2026-01-01,Odd,1.00,USD,   ")).isEqualTo("category must not be blank");
  }
```

A category of three spaces ends up as an empty text after the splitter trims it, and is refused.

```java
  @Test
  void aZeroAmountIsAGoodRow() {
    assertThat(parser.parse(2, "2026-01-01,Waived,0.00,USD,Fees")).isInstanceOf(ParsedRow.Ok.class);
  }
```

`0.00` parses to a good row.

```java
  @Test
  void theSplitterKeepsAQuotedCommaInsideOneField() {
    assertThat(CsvFieldSplitter.split("a,\"b,c\",d")).containsExactly("a", "b,c", "d");
  }
```

Calls the splitter directly: `a,"b,c",d` must produce three fields `a`, `b,c`, `d`. `containsExactly` also checks the order.

```java
  @Test
  void anEmptyLineIsOneEmptyField() {
    assertThat(CsvFieldSplitter.split("")).containsExactly("");
  }
}
```

An empty line splits into one empty field, which the parser then rejects as `expected 5 columns, found 1`. The last `}` closes the class.

#### `src/test/java/com/plainround/spend/infrastructure/csv/CsvRowParserTest.java`

The five main behaviors of the parser, each as a readable sentence, written first when the parser was built.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.infrastructure.csv;
import static org.assertj.core.api.Assertions.assertThat;
import com.plainround.spend.domain.ParsedRow;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `assertThat` (static) | `org.assertj.core.api.Assertions.assertThat` | the entry point of every AssertJ assertion |
| `ParsedRow` | `com.plainround.spend.domain.ParsedRow` | this repository's own `ParsedRow` (file `src/main/java/com/plainround/spend/domain/ParsedRow.java`, explained in its own section) |
| `BigDecimal` | `java.math.BigDecimal` | a decimal number with exact digits, used for money |
| `Currency` | `java.util.Currency` | an ISO 4217 currency such as USD, with its number of minor-unit digits |
| `Test` | `org.junit.jupiter.api.Test` | marks a method as a test |

```java
/**
 * The five parser tests the lesson asks for, one fault each. {@code scripts/prove-quote-test.sh}
 * deletes the quote handling and expects exactly one of these to fail.
 */
class CsvRowParserTest {

  private final CsvRowParser parser = new CsvRowParser(Currency.getInstance("USD"));
```

A parser for USD shared by the tests.

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

Parses a good line as line 2. The result must be an `Ok` whose amount equals `new BigDecimal("42.17")` exactly and whose category is `Groceries`. A block lambda `ok -> { ... }` holds two assertions.

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

The lesson's first bug: the amount `"1,204.50"` is quoted. In the Java source each inner quote is written `\"`. A plain `split(",")` would cut it in two. The test demands one field with amount 1204.50.

```java
  @Test
  void aDateInTheWrongFormatIsRejectedWithTheTextThatWasRead() {
    ParsedRow result = parser.parse(6, "31/02/2026,Gym Fees,35.00,USD,Health");

    assertThat(result)
        .isEqualTo(
            new ParsedRow.Rejected(
                6,
                "31/02/2026,Gym Fees,35.00,USD,Health",
                "date must look like 2026-01-31, got '31/02/2026'"));
  }
```

`31/02/2026` is not an ISO date. The expected result is one exact `Rejected` record compared by `isEqualTo`: records compare all their components, so line number, raw line and reason must all match.

```java
  @Test
  void aMissingColumnIsRejectedWithTheColumnCount() {
    ParsedRow result = parser.parse(16, "2026-02-02,Utility Bill,89.10,USD");

    assertThat(result)
        .isInstanceOfSatisfying(
            ParsedRow.Rejected.class,
            rejected -> assertThat(rejected.reason()).isEqualTo("expected 5 columns, found 4"));
  }
```

A line with four columns; the reason must be `expected 5 columns, found 4`.

```java
  @Test
  void aCurrencyOtherThanTheReportCurrencyIsRejectedByName() {
    ParsedRow result = parser.parse(11, "2026-01-15,Hotel Stay,310.00,EUR,Travel");

    assertThat(result)
        .isInstanceOfSatisfying(
            ParsedRow.Rejected.class,
            rejected ->
                assertThat(rejected.reason())
                    .isEqualTo("currency EUR is not the report currency USD"));
  }
}
```

A EUR row in a USD report is rejected with `currency EUR is not the report currency USD`. The last `}` closes the class.

#### `src/test/java/com/plainround/spend/interfaces/cli/LessonScenarioTest.java`

The end-to-end test: the real sample file goes through the whole program and the printed report must equal the stored `expected-report.txt` character for character.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.interfaces.cli;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `assertThat` (static) | `org.assertj.core.api.Assertions.assertThat` | the entry point of every AssertJ assertion |
| `ByteArrayOutputStream` | `java.io.ByteArrayOutputStream` | an output stream that collects everything written to it in memory, used by tests to capture printed output |
| `IOException` | `java.io.IOException` | the standard error type for failures while reading or writing (a file, a network connection) |
| `PrintStream` | `java.io.PrintStream` | an output stream with `print`, `println` and `printf` methods; `System.out` and `System.err` are print streams |
| `StandardCharsets` | `java.nio.charset.StandardCharsets` | constants for the standard character encodings, such as UTF-8 |
| `Files` | `java.nio.file.Files` | static helper methods for reading and writing files |
| `Path` | `java.nio.file.Path` | a location of a file or folder |
| `List` | `java.util.List` | an ordered collection of elements |
| `Map` | `java.util.Map` | a collection of key-value pairs |
| `Test` | `org.junit.jupiter.api.Test` | marks a method as a test |

```java
/** End to end: the lesson's 25-row file through the command line, compared with its output. */
class LessonScenarioTest {

  private static final Path SAMPLE = Path.of("sample/transactions.csv");
  private static final Path EXPECTED = Path.of("sample/expected-report.txt");
```

Two **Path** constants: the sample CSV and the expected report, relative to the repository root, where Maven runs the tests.

> New words:
>
> - **Path**: an object for a file or folder location, such as `sample/transactions.csv`.

```java
  @Test
  void theSampleFileProducesTheLessonsReportAndExitStatusTwo() throws IOException {
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8);

    int status = SpendCli.run(List.of(SAMPLE.toString()), Map.of(), out, System.err);

    assertThat(status).isEqualTo(2);
    assertThat(stdout.toString(StandardCharsets.UTF_8))
        .isEqualTo(Files.readString(EXPECTED, StandardCharsets.UTF_8));
  }
```

Uses **captured output**: `new PrintStream(stdout, true, StandardCharsets.UTF_8)` prints into memory (the `true` flushes after each line). `SpendCli.run(List.of(SAMPLE.toString()), Map.of(), out, System.err)` runs the program with one argument, an empty environment, the captured output and the real error stream. The status must be 2 (some rows are rejected) and the printed text must equal `Files.readString(EXPECTED, UTF_8)`.

> New words:
>
> - **captured output**: a `ByteArrayOutputStream` is an in-memory place that bytes can be written to; wrapping it in a `PrintStream` lets code `print` into it exactly as it would print to the screen. The test then reads back what was printed as text and compares it.

```java
  @Test
  void januaryGroceriesMatchTheHandCalculationToTheCent() throws IOException {
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    SpendCli.run(
        List.of(SAMPLE.toString()),
        Map.of(),
        new PrintStream(stdout, true, StandardCharsets.UTF_8),
        System.err);

    // 42.17 + 55.03 + 48.60 = 145.80
    assertThat(stdout.toString(StandardCharsets.UTF_8)).contains("  Groceries       145.80");
  }
}
```

Runs the same program, then checks the output *contains* the line `  Groceries       145.80`. The comment holds the hand calculation `42.17 + 55.03 + 48.60 = 145.80`: the expected number comes from a person with a calculator, not from the program. The last `}` closes the class.

#### `src/test/java/com/plainround/spend/interfaces/cli/SpendCliTest.java`

Tests the command line from the outside: it feeds arguments and environment, captures both output streams and checks text and exit status. This covers the wiring and all four exit codes.

**Header.** The first lines are the package declaration and the import list. Each import brings one class into reach so the code can use its short name.

```java
package com.plainround.spend.interfaces.cli;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
```

| Short name | Full name | What it is |
| --- | --- | --- |
| `assertThat` (static) | `org.assertj.core.api.Assertions.assertThat` | the entry point of every AssertJ assertion |
| `ByteArrayOutputStream` | `java.io.ByteArrayOutputStream` | an output stream that collects everything written to it in memory, used by tests to capture printed output |
| `IOException` | `java.io.IOException` | the standard error type for failures while reading or writing (a file, a network connection) |
| `PrintStream` | `java.io.PrintStream` | an output stream with `print`, `println` and `printf` methods; `System.out` and `System.err` are print streams |
| `StandardCharsets` | `java.nio.charset.StandardCharsets` | constants for the standard character encodings, such as UTF-8 |
| `Files` | `java.nio.file.Files` | static helper methods for reading and writing files |
| `Path` | `java.nio.file.Path` | a location of a file or folder |
| `List` | `java.util.List` | an ordered collection of elements |
| `Map` | `java.util.Map` | a collection of key-value pairs |
| `Test` | `org.junit.jupiter.api.Test` | marks a method as a test |
| `TempDir` | `org.junit.jupiter.api.io.TempDir` | marks a field or parameter that JUnit fills with a fresh temporary folder |

```java
class SpendCliTest {

  private static final String HEADER = "date,description,amount,currency,category\n";

  @TempDir Path folder;

  private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
  private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
```

`HEADER` is the CSV header line. `@TempDir` gives a temporary folder. `stdout` and `stderr` are two in-memory buffers (captured output); one pair is made per test because JUnit creates a new test object for each test method.

```java
  private int run(Map<String, String> environment, String... args) {
    return SpendCli.run(
        List.of(args),
        environment,
        new PrintStream(stdout, true, StandardCharsets.UTF_8),
        new PrintStream(stderr, true, StandardCharsets.UTF_8));
  }
```

Helper that calls `SpendCli.run` with captured streams and returns the status. `String... args` is varargs, so tests write `run(Map.of(), "--top", "x")`.

```java
  private Path csv(String rows) throws IOException {
    return Files.writeString(folder.resolve("t.csv"), HEADER + rows);
  }
```

Helper: writes `HEADER + rows` into `t.csv` in the temporary folder and returns its path.

```java
  private String out() {
    return stdout.toString(StandardCharsets.UTF_8);
  }
```

Helper: the captured standard output as text.

```java
  @Test
  void aCleanFileExitsZeroAndPrintsNoRejectSection() throws IOException {
    int status = run(Map.of(), csv("2026-01-03,Grocer,10.005,USD,Food\n").toString());

    assertThat(status).isZero();
    assertThat(out()).contains("1 rows read, 0 rejected").doesNotContain("Rejected rows");
  }
```

A file with one good row. The status is zero (`isZero()`), the output contains `1 rows read, 0 rejected` and does *not* contain `Rejected rows`. The amount 10.005 is intentional: later tests use it for rounding.

```java
  @Test
  void halfEvenRoundingIsAppliedOnlyWhenPrinting() throws IOException {
    run(Map.of(), csv("2026-01-03,A,10.005,USD,Food\n2026-01-04,B,10.015,USD,Fun\n").toString());

    assertThat(out())
        .containsPattern("Food\\s+10\\.00\\n")
        .containsPattern("Fun\\s+10\\.02\\n")
        .containsPattern("total\\s+20\\.02\\n");
  }
```

Two rows: 10.005 (Food) and 10.015 (Fun). Half-even rounding sends 10.005 to 10.00 (the previous digit 0 is even) and 10.015 to 10.02 (1 is odd, round up to even 2). The total is the exact sum 20.020, printed as 20.02. `containsPattern` matches a regular expression; `\\s+` means one or more spaces, `\\.` a literal dot, `\\n` a line break.

```java
  @Test
  void aRejectedRowMakesTheExitStatusTwo() throws IOException {
    int status = run(Map.of(), csv("2026-01-03,Grocer,oops,USD,Food\n").toString());

    assertThat(status).isEqualTo(SpendCli.EXIT_REJECTS);
    assertThat(out()).contains("line 2: amount is not a number: 'oops'");
  }
```

The amount `oops` is rejected. The status must be `SpendCli.EXIT_REJECTS` (2) and the output must contain `line 2: amount is not a number: 'oops'`.

```java
  @Test
  void rejectsBeyondTheKeptLimitAreCountedAndTheListSaysSo() throws IOException {
    String rows = "bad\nbad\nbad\n";

    run(Map.of("SPEND_MAX_REJECTED", "1"), csv(rows).toString());

    assertThat(out()).contains("Rejected rows (3, showing the first 1)");
  }
```

Sets the environment variable `SPEND_MAX_REJECTED` to 1 and feeds three bad lines. The header must say `Rejected rows (3, showing the first 1)`: the count is exact, the list is shortened.

```java
  @Test
  void aMissingFileExitsOneWithOneLineOnStandardError() {
    int status = run(Map.of(), folder.resolve("missing.csv").toString());

    assertThat(status).isEqualTo(SpendCli.EXIT_UNREADABLE);
    assertThat(stderr.toString(StandardCharsets.UTF_8))
        .contains("cannot read")
        .doesNotContain("at ");
  }
```

A path that does not exist: status `EXIT_UNREADABLE` (1). The error text contains `cannot read` and must not contain `at ` (that would be a stack trace line such as `at java.base/...`).

```java
  @Test
  void usageMistakesExitSixtyFourWithTheUsageLine() {
    assertThat(run(Map.of())).isEqualTo(SpendCli.EXIT_USAGE);
    assertThat(run(Map.of(), "a.csv", "b.csv")).isEqualTo(SpendCli.EXIT_USAGE);
    assertThat(run(Map.of(), "--top")).isEqualTo(SpendCli.EXIT_USAGE);
    assertThat(run(Map.of(), "--top", "x", "a.csv")).isEqualTo(SpendCli.EXIT_USAGE);
    assertThat(run(Map.of("SPEND_TOP", "x"), "a.csv")).isEqualTo(SpendCli.EXIT_USAGE);
    assertThat(stderr.toString(StandardCharsets.UTF_8)).contains("usage: spend");
  }
```

Five wrong command lines, each expected to return 64: no arguments; two files; `--top` without a value; `--top x` (not a number); and a bad environment value `SPEND_TOP=x`. The error stream must contain `usage: spend`.

```java
  @Test
  void helpPrintsTheUsageLineAndExitsZero() {
    assertThat(run(Map.of(), "--help")).isZero();
    assertThat(out()).startsWith("usage: spend");
  }
```

`--help` returns 0 and prints on standard output (not on the error stream) a text that `startsWith("usage: spend")`.

```java
  @Test
  void theCurrencyAndTopFlagsChangeTheReport() throws IOException {
    Path file = csv("2026-01-03,A,5,EUR,Food\n2026-01-04,B,6,EUR,Food\n");

    int status = run(Map.of(), "--currency", "EUR", "--top", "1", file.toString());

    assertThat(status).isZero();
    assertThat(out()).contains("Spending report (EUR)").contains("1 largest purchases");
  }
```

Two EUR rows with `--currency EUR --top 1`. The report heading must say `Spending report (EUR)` and `1 largest purchases`.

```java
  @Test
  void aCurrencyWithoutMinorUnitsPrintsWholeNumbers() throws IOException {
    run(Map.of(), "--currency", "JPY", csv("2026-01-03,A,1500,JPY,Food\n").toString());

    assertThat(out()).containsPattern("Food\\s+1,500\\n");
  }
```

The yen has no minor unit, so `1500` must print as `1,500` with no decimals: the printer reads the currency's own number of decimal places.

```java
  @Test
  void anEmptyFileStillPrintsTheHeaderLine() throws IOException {
    int status = run(Map.of(), csv("").toString());

    assertThat(status).isZero();
    assertThat(out()).isEqualTo("Spending report (USD): 0 rows read, 0 rejected\n");
  }
}
```

A file with only a header. Status 0 and the exact output `Spending report (USD): 0 rows read, 0 rejected` followed by a line break. The last `}` closes the class.

### Glossary

| Word | Meaning |
| --- | --- |
| **"with" method** | a method that returns a *copy* of an unchangeable object with one value replaced (`settings.withCurrency("JPY")`), because a record cannot be changed in place |
| **@Override** | an annotation that says "this method replaces a method of the parent class or interface". The compiler reports an error if no such method exists |
| **accessor** | a method that returns one field's value. A record gives each component an accessor with the component's own name, for example `amount()` |
| **adapter** | a class that implements a port using a real technology, such as a file or a network |
| **annotation** | a label written with `@` before a declaration, such as `@Override` or `@Test`. Tools and the compiler read it; it does not change what the code does by itself |
| **architecture rule** | a test of the code's structure instead of its behavior: ArchUnit reads the compiled classes, builds a graph of which class uses which, and fails when a rule such as "the domain package must not use the infrastructure package" is broken |
| **assertion** | a statement of what must be true. `assertThat(actual).isEqualTo(expected)` (from the library AssertJ) does nothing when the two match and throws an `AssertionError` with a readable message when they do not, which makes the test fail |
| **BigDecimal** | a decimal number with exact digits. A `double` stores binary fractions (so 0.1 + 0.2 is 0.30000000000000004); `BigDecimal` keeps the digits you typed, which is why money uses it |
| **byte array** | `byte[]` is a fixed-length sequence of bytes, the raw 8-bit values a file is made of. A string becomes bytes through a character encoding such as UTF-8 |
| **captured output** | a `ByteArrayOutputStream` is an in-memory place that bytes can be written to; wrapping it in a `PrintStream` lets code `print` into it exactly as it would print to the screen. The test then reads back what was printed as text and compares it |
| **cast** | writing `(Type) value` to tell Java to treat a value as a more specific type. If the value is not of that type at run time, Java throws `ClassCastException` |
| **cause** | the original exception wrapped inside a new one, so the full story stays available when the new exception is reported |
| **char** | a single character, written in single quotes such as `','` |
| **character encoding** | the rule that turns bytes into text. UTF-8 is the common one. If a program does not name it, it uses the machine's default, which differs between computers |
| **class** | a blueprint for objects. It lists the data each object holds (fields) and the actions it can do (methods). The program creates objects from it with `new` |
| **command-line arguments** | the words typed after a program's name. Java hands them to `main` as an array of strings |
| **compact constructor** | the constructor of a record written without a parameter list. It runs before the fields are assigned, so it is the place to check or adjust the values |
| **comparator** | an object that decides which of two values comes first. Comparators can be chained: order by this, and if equal, by that |
| **conditional expression** | `condition ? a : b` gives `a` if the condition is true and `b` otherwise |
| **constructor** | the special method that sets up a new object. It has the class's name, no return type, and runs when you write `new ClassName(...)` |
| **consumer** | a function that takes a value and returns nothing, standard interface `Consumer<T>`; it is how code hands values to someone else one at a time |
| **dependency injection** | giving an object the things it needs through its constructor instead of letting it create them. A test can then hand in a fake |
| **environment variable** | a named text setting that a program inherits from the process that started it, for example `LOG_LEVEL=DEBUG`. Programs read them to be configured without changing code |
| **exception** | an object that represents something that went wrong. Code that detects a problem `throw`s one; the nearest `catch` that matches handles it; if none does, the program stops with an error message |
| **exit status** | the number a finished program hands back to the shell: 0 means success and any other number means something specific. A script reads it with `$?` |
| **expected failure** | `assertThatThrownBy(() -> code)` runs the code inside the lambda and checks that it throws; the next calls on the result check the type and the message of the error. A test needs this to prove that bad input is refused |
| **fake** | a small stand-in written for a test, such as an in-memory port (in the design sense) implementation. It lets the test run one class on its own, with no file, network or clock |
| **field** | a variable that belongs to an object (or, when `static`, to the class). It holds one piece of the object's data |
| **final** | can be set once and never changed. On a field: assigned in the constructor and then fixed. On a class: no other class may extend it |
| **for-each loop** | `for (Type x : collection)` runs the body once for each element, naming it `x` |
| **generics** | type parameters in angle brackets: `List<String>` is a list that holds only strings. The compiler checks the types so a wrong element cannot slip in |
| **half-even rounding** | rounding where an exact tie goes to the nearest even digit (10.005 becomes 10.00, 10.015 becomes 10.02). Over many numbers it does not lean up or down |
| **immutable** | never changed after creation. Immutable objects can be shared freely because nobody can alter them |
| **implements** | the word with which a class promises to provide every method of an interface |
| **import** | a line that brings a class from another package into reach so that code can write its short name. Without it the code would have to write the full name every time |
| **interface** | a list of method names that a class promises to provide, without the code. Other code can use the interface and not care which class stands behind it |
| **iterator** | an object with `hasNext()` and `next()` that hands out the elements of a collection one by one |
| **lambda** | a short function written inline: `month -> new TreeMap<>()` means "given `month`, return a new map". The part before `->` names the parameters |
| **lazy stream of lines** | `Files.lines` reads a file one line at a time as it is needed instead of loading it whole, so a huge file uses little memory |
| **list** | an ordered collection. Elements keep their position and can repeat. `List.of(...)` builds a list that cannot be changed |
| **LocalDate** | a calendar date (year, month, day) without a time or time zone |
| **Locale** | a language and region. Number and date formatting depend on it (`1,204.50` in the US, `1.204,50` in Germany), so the code fixes it to `Locale.US` to print the same everywhere |
| **logger** | an object that writes log lines with a level (`INFO`, `WARN`, ...). The log engine decides where they go and how they look |
| **main method** | the method the JVM calls to start a program: `public static void main(String[] args)`. `args` holds the words typed after the program name |
| **map** | a collection of key-value pairs: you give a key and get the value stored under it. A key appears at most once |
| **method** | a named block of code that belongs to a class. It can take parameters and return a value; `void` means it returns nothing |
| **method reference** | a short way to pass an existing method as a function: `BigDecimal::add` means "the `add` method of `BigDecimal`" |
| **null** | the value that means "no object". Calling a method on it throws `NullPointerException`, so code checks for it first |
| **package** | a named group of classes. The name matches a folder path under `src/main/java`: `com.plainround.spend.domain` is the folder `com/plainround/spend/domain`. A package keeps names from clashing and lets a class hide things from other packages |
| **parameterized test** | one test method run once for every row of a table of inputs. `@ParameterizedTest` says so, and `@CsvSource` supplies the rows; each row's values are passed as the method's arguments. The test report shows each row as its own test |
| **Path** | an object for a file or folder location, such as `sample/transactions.csv` |
| **pattern-matching switch** | a `switch` that tests the type of a value: `case ParsedRow.Ok ok ->` runs when the value is an `Ok` and gives it the name `ok`. With a sealed type the compiler checks that every case is handled |
| **port (in the design sense)** | an interface that the inner part of the program owns and calls (for example "where do rows come from"); the outer part supplies an implementation, called an adapter |
| **printf format** | `out.printf("%d rows%n", 5)` prints a template in which `%d` is a whole number, `%s` text, `%n` a line break, `%10s` text padded to 10 characters on the left, `%-12s` padded on the right and `%,.2f` a number with thousands separators and two decimals |
| **priority queue** | a collection whose head is always the smallest element according to its ordering. Adding and removing the head are fast |
| **private** | visible only inside this class. Other code cannot touch it. `public` means visible everywhere; no word at all means visible only inside the same package |
| **record** | a short way to write a class that only carries data. You list the components once; Java writes the constructor, the accessor methods, `equals`, `hashCode` and `toString` for you, and the fields are `final` |
| **regular expression** | a short pattern language for text shapes: `\d` is a digit, `+` means one or more, `?` optional, `[+-]` one of the two signs, `(...)` a group, `.` any character. A `Pattern` is a compiled one and `matcher(text).matches()` asks whether the whole text fits |
| **sealed interface** | an interface that names, after `permits`, every class allowed to implement it. The compiler then knows all the cases, which makes a `switch` over it checkable |
| **serialVersionUID** | a version number Java asks for on exception classes because they can be written to a stream. The compiler flag `-Xlint:all` would warn without it, and `-Werror` turns warnings into errors |
| **standard output and standard error** | the two text streams every program has. Standard output (`System.out`) carries the program's result; standard error (`System.err`) carries diagnostics. In a terminal both show on screen but they can be separated |
| **static** | a member that belongs to the class itself and not to any one object. You call it as `ClassName.member`, without `new` |
| **stream** | a sequence of values processed step by step: `filter` keeps some, `map` changes each, `sorted` orders them, and a last step such as `toList()` collects the result. A stream does not change the collection it came from |
| **StringBuilder** | a text buffer you can append to cheaply, used to build one string out of many pieces |
| **switch with arrows** | a `switch` whose cases are written `case "x" -> action;`. Only the matching case runs; there is no fall-through to the next one |
| **temporary folder** | a folder that JUnit creates fresh for each test and deletes afterwards. A field marked `@TempDir` receives it. Tests write files there so they never touch real data and never leave anything behind |
| **test method** | a method marked `@Test`. The test runner (JUnit) finds every such method, runs each one on a fresh object of the test class, and reports it as passed if it finishes without an error and failed if it throws one. The method name is a sentence that says what must be true |
| **throws clause** | `throws IOException` after a method's parameter list says the method may fail with that checked error. Java makes the caller either handle it or declare it as well, so a test that touches files declares it and lets the test runner report the failure |
| **TreeMap** | a map that keeps its keys sorted, so iterating gives them in order |
| **try-with-resources** | `try (Resource r = ...) { ... }` closes the resource automatically when the block ends, even if an exception is thrown. It is how files, streams and servers are closed safely |
| **try/catch** | `try { ... }` runs code; if it throws, Java looks through the `catch` blocks in order and runs the first one whose type matches the exception |
| **unchecked exception** | an exception type (a subclass of `RuntimeException`) that methods do not have to declare. Checked ones (such as `IOException`) must be declared or caught |
| **varargs** | a parameter written `Type... name` accepts any number of arguments of that type, including none; inside the method it is an array. Callers write `rows(a, b)` and Java builds the array |

## 6. Try it yourself

- `java -version`. Expected: `openjdk version "25..."`.
- `./mvnw -q -B clean package -DskipTests -Djacoco.skip=true && javap -cp target/classes com.plainround.spend.domain.Transaction`. Expected: the members of the compiled record, including `public final java.lang.String toString();`, `public final int hashCode();` and `public final boolean equals(java.lang.Object);`, which nobody wrote: Java generated them. `javap` is the JDK tool that shows what is inside a `.class` file.
- `find src/main/java -name '*.java' | head -30`. Expected: the file list that matches the layers in the README's project-structure tree.
- `jshell`, then `record P(int x, int y) {}` and `new P(1,2).equals(new P(1,2))`. Expected: `$2 ==> true` (the number after `$` is the snippet number).
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
