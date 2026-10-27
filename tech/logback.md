# Logback (1.6.6)

## 1. What it is

SLF4J (see `slf4j.md`) only takes the log calls. **Logback** is the **logging engine** behind it: it receives each event and writes it somewhere in some format. It is configured by an XML file with three kinds of pieces: an **appender** (where to write: the console, a file), an **encoder** with a **pattern** (what each line looks like) and a **level** (how much to write).

## 2. Why this repository uses it

The repository requires structured logs with levels. One small file gives every program the same `ts=... level=... logger=... msg="..."` lines on standard error, and an environment variable changes the level.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `src/main/resources/logback.xml` | the real configuration, packaged in the jar |
| `src/test/resources/logback-test.xml` | the quieter test configuration |
| `pom.xml` | `ch.qos.logback:logback-classic:${logback.version}`, `runtime` scope |
| `.env.example` | documents `LOG_LEVEL` |

## 4. How it works underneath

1. At the first log call SLF4J loads Logback's provider; Logback then looks for `logback-test.xml`, then `logback.xml`, on the class path and builds its appenders from it.
2. Each log call becomes an event with a time, level, logger name, message and thread. The root logger passes it to its appenders if the event's level is at least the root level.
3. The console appender formats the event with the pattern and writes the line to `System.err`.
4. The configuration file is read once, at start-up.

## 5. Walkthrough of this repository's use

### `src/main/resources/logback.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
```

The XML declaration: XML version 1.0, UTF-8 text.

```xml
<!-- Structured key=value (logfmt) lines on standard error; standard output carries only the report. -->
```

An **XML comment** describing the file: key=value lines on standard error, and standard output carries only the report.

> New words:
>
> - **XML comment**: text between `<!--` and `-->`. Tools ignore it; it is there for the human reader.

```xml
<configuration>
```

`<configuration>` is the root of a Logback file. Logback finds `logback.xml` on the **class path** (the list of places Java looks for files and classes) at start-up; this one is in `src/main/resources`, so it ends up inside the jar.

```xml
  <appender name="STDERR" class="ch.qos.logback.core.ConsoleAppender">
```

An **appender** is a destination for log lines. `name="STDERR"` is a label chosen here; `class="ch.qos.logback.core.ConsoleAppender"` selects the kind: write to the console.

```xml
    <target>System.err</target>
```

`System.err` chooses **standard error**, the stream meant for diagnostics. The default would be standard output, which this program keeps clean for its real output.

```xml
    <encoder>
      <pattern>ts=%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} level=%level logger=%logger{0} msg="%msg"%n%ex{short}</pattern>
    </encoder>
  </appender>
```

The **encoder** turns an event into text. Its `<pattern>` is the template; each `%word` is replaced by a part of the event:

| Part | Meaning |
| --- | --- |
| `%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX}` | the time: year-month-day, the letter T, hours:minutes:seconds, milliseconds, and the zone offset (`XXX` prints `Z` for UTC) |
| `%level` | the level (`INFO`, `WARN`, ...) |
| `%logger{0}` | the logger name shortened to the class name only (`0` means no package parts) |
| `%msg` | the message, with its `{}` already filled in |
| `%n` | a line break |
| `%ex{short}` | the exception, if there is one, cut down to its first line |

The fixed words `ts=`, `level=`, `logger=` and `msg=` make every line a set of `key=value` pairs, the **logfmt** style that tools can split reliably. Example: `ts=2026-10-27T12:00:03.250Z level=INFO logger=SpendCli msg="report done rows=15 rejected=10"`.

```xml
  <root level="${LOG_LEVEL:-INFO}">
    <appender-ref ref="STDERR"/>
  </root>
</configuration>
```

The **root logger** is the ancestor of every logger, so its level applies everywhere. `${LOG_LEVEL:-INFO}` reads the variable `LOG_LEVEL` from the system properties or the **environment variable**s and uses `INFO` when it is not set. So `LOG_LEVEL=DEBUG` in the environment turns debug on without editing any file. `<appender-ref ref="STDERR"/>` connects the root logger to the appender named `STDERR`. The last tag closes `<configuration>`.

> New words:
>
> - **environment variable**: a named text setting that a program inherits from the process that started it, for example `LOG_LEVEL=DEBUG`. Programs read them to be configured without changing code.

### `src/test/resources/logback-test.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
```

The XML declaration.

```xml
<configuration>
```

Same structure as the main file. Logback looks for `logback-test.xml` on the class path *before* `logback.xml`; during tests both exist, and the test file wins.

```xml
  <appender name="STDERR" class="ch.qos.logback.core.ConsoleAppender">
    <target>System.err</target>
    <encoder>
      <pattern>ts=%d{HH:mm:ss.SSS} level=%level logger=%logger{0} msg="%msg"%n%ex{short}</pattern>
    </encoder>
  </appender>
```

The same console appender writing to standard error. The only difference in the pattern is the shorter time `%d{HH:mm:ss.SSS}` (hours, minutes, seconds, milliseconds), enough for a test run.

```xml
  <root level="WARN">
    <appender-ref ref="STDERR"/>
  </root>
</configuration>
```

The root level is fixed at `WARN`, so the test output shows only problems, not hundreds of `INFO` lines.

## 6. Try it yourself

- Run any program of this repository with a level, e.g. `LOG_LEVEL=DEBUG java -jar target/spend.jar ...` (see the README for the arguments). Expected: more lines, each `ts=... level=DEBUG ...`.
- `unzip -p target/spend.jar logback.xml`. Expected: the same file, inside the jar.
- `LOG_LEVEL=ERROR java -jar ...`. Expected: almost no log lines.

## 7. Mistakes people make

- Leaving the default target (standard output) and mixing logs with the program's real output.
- Putting the level in code. Here it comes from the environment.
- Two engines on the class path: SLF4J then warns about multiple providers. Only Logback is a dependency here.
- Expecting configuration changes without a restart; the file is read once.

## 8. Check yourself

1. What is an appender?
   **Answer:** A destination for log events, such as the console or a file.
2. What does `${LOG_LEVEL:-INFO}` mean?
   **Answer:** Use the variable `LOG_LEVEL` if it is set, otherwise `INFO`.
3. Why standard error?
   **Answer:** Standard output carries the program's results; diagnostics go to standard error.
4. Which file do the tests use?
   **Answer:** `logback-test.xml`, with the root level at `WARN`.
5. What do the `key=value` pairs give us?
   **Answer:** Lines that tools can split reliably (logfmt style).

## 9. Further reading

- Logback 1.6 manual: https://logback.qos.ch/manual/
- Configuration and variables: https://logback.qos.ch/manual/configuration.html
- Layouts and conversion words (`%d`, `%logger`, `%ex`): https://logback.qos.ch/manual/layouts.html
