# SLF4J (2.0.20)

## 1. What it is

SLF4J (Simple Logging Facade for Java) is a **facade**: a small library with the logging calls (`info`, `warn`, `debug`) but no code that prints anything. At run time it finds a real logging engine on the class path (here Logback) and hands the messages to it. The code stays the same when the engine changes.

## 2. Why this repository uses it

The repository wants structured logging with levels and no secrets. The code writes events through SLF4J; the format and the destination (standard error, `key=value` lines) are decided in one configuration file.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | `org.slf4j:slf4j-api:${slf4j.version}`, compile scope |
| `src/main/java/**` (the files that log) | each declares `Logger LOG = LoggerFactory.getLogger(...)` |
| `src/test/resources/logback-test.xml` | turns logging down to WARN in tests |

Files that log: `CsvFileRowSource.java`, `SpendCli.java`.

## 4. How it works underneath

- At start-up, SLF4J looks for a **provider** on the class path using Java's `ServiceLoader` (a file under `META-INF/services/` that names the implementation). Logback provides one. The Shade plugin keeps those service files when it builds the single jar.
- `LoggerFactory.getLogger(...)` returns a logger object. `info(...)` asks the engine: is INFO enabled for this logger? If yes, format the message (replace `{}` with the arguments) and write it.
- If no provider is found, SLF4J prints a warning once and logs nothing (a "no-operation" logger).

## 5. Walkthrough of this repository's use

Each class that logs declares one logger at the top:

```java
  private static final Logger LOG = LoggerFactory.getLogger(CsvFileRowSource.class);
```

`Logger` and `LoggerFactory` come from SLF4J (`org.slf4j`). `getLogger(X.class)` returns the logger named after the class, so every line says where it came from (the pattern prints the short name). The field is `private static final`: one per class, never changed.

A call:

```java
    LOG.info("reading file path={}", path);
```

Every log call in this repository is a key=value pair so a tool can split the line. `{}` is a placeholder: SLF4J puts the value of `path` there only if the INFO level is on, so a disabled call costs almost nothing.

Levels, from loudest to quietest: `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`. Setting a logger to a level shows that level and everything louder. In this repository the root level comes from the `LOG_LEVEL` environment variable (see `logback.md`), default `INFO`.

The `slf4j-api` jar in the pom is the only logging library the code compiles against. No class imports Logback. Which engine prints the lines is decided at run time by what else is on the class path (see `logback.md`), and an ArchUnit rule keeps logging out of the `domain` package.

## 6. Try it yourself

- `grep -rn 'LoggerFactory' src/main/java | head`. Expected: one `Logger LOG = LoggerFactory.getLogger(...)` line per class that logs.
- Run the program and look at standard error (see the README for the command), for example `LOG_LEVEL=DEBUG` before the command. Expected: extra `level=DEBUG` lines appear.
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
