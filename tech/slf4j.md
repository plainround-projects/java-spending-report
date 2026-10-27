# SLF4J (2.0.20)

## 1. What it is

A **log** is a running diary a program writes about what it is doing: "started", "read 12 rows", "something unexpected happened". Each entry has a **level** that says how important it is, from `ERROR` (most serious) through `WARN`, `INFO` and `DEBUG` to `TRACE` (most detailed). A person or a tool can then choose how much to see. **SLF4J** (Simple Logging Facade for Java) is a **facade**: a small library that offers the logging calls (`info`, `warn`, `debug`) but contains no code that prints anything. At run time it finds a real logging engine on the class path (here Logback, see `logback.md`) and hands the messages to it. The program's code stays the same if the engine changes.

## 2. Why this repository uses it

The repository wants structured logging with levels and no secrets. The code writes events through SLF4J; the format and the destination (standard error, `key=value` lines) are decided in one configuration file.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | `org.slf4j:slf4j-api:${slf4j.version}`, compile scope |
| `src/main/java/**` (the files that log) | `CsvFileRowSource.java`, `SpendCli.java` |
| `src/test/resources/logback-test.xml` | turns logging down to WARN in tests |

## 4. How it works underneath

1. **Finding the engine.** At start-up SLF4J looks for a **provider** on the class path using Java's `ServiceLoader` (a file under `META-INF/services/` that names the implementation). Logback provides one. The Shade plugin keeps those service files when it builds the single jar.
2. **A logger** is an object with a name. `LoggerFactory.getLogger(CsvFileRowSource.class)` returns the logger named after the class; the log line shows that name so you can see where it came from.
3. **A call** such as `LOG.info("reading file path={}", path)` asks the engine: is INFO enabled for this logger? If yes, format the message (each `{}` is replaced by the next argument, in order) and write it. If not, nothing is built: a disabled call costs almost nothing.
4. **Levels** from loudest to quietest: `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`. Setting a logger to a level shows that level and everything louder. Here the root level comes from the `LOG_LEVEL` **environment variable** (default `INFO`).
5. **Key=value style.** Every message in this repository is written as `words key={} key={}` so that a tool can split a line into fields.
6. If no provider is found, SLF4J prints a warning once and logs nothing (a "no-operation" logger).

> New words:
>
> - **environment variable**: a named text setting that a program inherits from the process that started it, for example `LOG_LEVEL=DEBUG`. Programs read them to be configured without changing code.

## 5. Walkthrough of this repository's use

### The logger declaration

Every class that logs declares one logger at the top: `private static final Logger LOG = LoggerFactory.getLogger(X.class);` (the leak lab's audit classes use the name `"audit"` instead of a class, so that one setting controls both). `Logger` and `LoggerFactory` come from the package `org.slf4j`. The field is `private static final`: one per class, created once, never changed.

### Every log call in the program

| File | Call | What it reports |
| --- | --- | --- |
| `CsvFileRowSource.java` | `LOG.info("reading file path={}"...)` | INFO when the spending report starts reading the input file; the path is the only value. |
| `SpendCli.java` | `LOG.info("report done rows={} rejected={}"...)` | INFO when the report is finished: how many good rows and how many rejected rows. |

The `slf4j-api` jar is the only logging library the code compiles against. No class imports Logback, and an ArchUnit rule keeps logging out of the `domain` package.

## 6. Try it yourself

- `grep -rn 'LoggerFactory' src/main/java | head`. Expected: one `Logger LOG = LoggerFactory.getLogger(...)` line per class that logs.
- `java -jar target/spend.jar sample/transactions.csv >/dev/null`. Expected: two lines on the screen, both `level=INFO`: `reading file path=sample/transactions.csv` and `report done rows=15 rejected=10` (`>/dev/null` hides the report on standard output, so only the log on standard error is left).
- `LOG_LEVEL=ERROR java -jar target/spend.jar sample/transactions.csv >/dev/null`. Expected: no log lines at all, because no ERROR was logged.
- `./mvnw dependency:tree | grep -i slf4j`. Expected: `org.slf4j:slf4j-api:jar:2.0.20:compile`.

## 7. Mistakes people make

- Building the message with `+` before the call: `LOG.debug("x=" + expensive())` does the work even when debug is off. Use `{}` placeholders or the supplier form.
- Logging a secret or a whole request body. The log calls here carry identifiers and counts only.
- Importing the engine (`ch.qos.logback...`) in application code; that couples the code to one engine.
- Logging and also throwing the same error: the error is then reported twice.

## 8. Check yourself

1. What is a facade?
   **Answer:** A small interface layer with no implementation of its own; SLF4J forwards to an engine.
2. What does `{}` do in `LOG.info("a={}", x)`?
   **Answer:** It is replaced by the argument, but only if the level is enabled.
3. Why `private static final Logger LOG`?
   **Answer:** One logger per class, created once, never replaced.
4. Which libraries are on the class path at run time for logging?
   **Answer:** `slf4j-api` and `logback-classic` (with `logback-core`).
5. What happens if no engine is present?
   **Answer:** SLF4J prints one warning and discards every message.

## 9. Further reading

- SLF4J 2.0 manual: https://www.slf4j.org/manual.html
- Fluent logging API (2.0): https://www.slf4j.org/manual.html#fluent
- SLF4J FAQ: https://www.slf4j.org/faq.html
