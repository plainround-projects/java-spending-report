# Logback (1.6.6)

## 1. What it is

Logback is a **logging engine**: it receives the events that SLF4J forwards and writes them somewhere in some format. It is configured by an XML file. The pieces are an **appender** (where to write), an **encoder** with a **pattern** (what each line looks like) and a **level** (how much to write).

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

- At the first log call SLF4J loads Logback's provider; Logback then looks for `logback-test.xml`, then `logback.xml`, on the class path and builds its appenders from it.
- Each log call becomes an event with a time, level, logger name, message and thread. The root logger passes it to its appenders if the level is enabled.
- The console appender formats the event with the pattern and writes the line to `System.err`.
- The configuration file is read once, at start-up.

## 5. Walkthrough of this repository's use

```xml
<configuration>
  <appender name="STDERR" class="ch.qos.logback.core.ConsoleAppender">
    <target>System.err</target>
    <encoder>
      <pattern>ts=%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} level=%level logger=%logger{0} msg="%msg"%n%ex{short}</pattern>
    </encoder>
  </appender>
  <root level="${LOG_LEVEL:-INFO}">
    <appender-ref ref="STDERR"/>
  </root>
</configuration>
```

- `<configuration>` is the root of a Logback config file. Logback finds `logback.xml` on the class path at start-up (our file is in `src/main/resources`, so it ends up in the jar).
- `<appender>` names a destination. `ConsoleAppender` writes to the console; `<target>System.err</target>` selects **standard error** (the stream for diagnostics). The default would be standard output, which this program keeps clean for its real output.
- `<encoder>` decides how an event becomes text. `<pattern>` is the template. `%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX}` is the time in ISO form with milliseconds and the zone offset (`Z` for UTC); `%level` the level; `%logger{0}` the logger name shortened to the class name only; `%msg` the message; `%n` a newline; `%ex{short}` the exception, if any, cut to the first lines. The fixed words `ts=`, `level=`, `logger=`, `msg=` make every line a set of `key=value` pairs, the **logfmt** style that tools can split.
- `<root level="${LOG_LEVEL:-INFO}">` sets the level for every logger. `${NAME:-default}` reads the variable `NAME` from the system properties or the environment, and uses `INFO` when it is not set. So `LOG_LEVEL=DEBUG` in the environment turns debug on without editing a file (twelve-factor configuration).
- `<appender-ref ref="STDERR"/>` connects the root logger to the appender named `STDERR`.

In the tests a second file replaces this one:

```xml
  <root level="WARN">
    <appender-ref ref="STDERR"/>
  </root>
```

Logback looks for `logback-test.xml` before `logback.xml`. With the root level at `WARN`, the test output shows only problems, not hundreds of `INFO` lines.

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
