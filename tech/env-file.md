# .env.example (environment variables)

## 1. What it is

A program often needs settings: a port number, a time limit, a password. Putting them into the code means rebuilding to change them. An **environment variable** is a named value that the operating system hands to every program it starts, so the same program can behave differently without changing it. The convention is to list all of them in a file named `.env.example`, one `NAME=value` per line, and to copy it to `.env` (which git ignores) for personal values.

## 2. Why this repository uses it

The repository promises that every configuration value is listed with a default, that none is a secret, and that settings come from the environment (the twelve-factor way). `.env.example` is that list.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `.env.example` | the template, walked through below |
| `.env` | optional personal copy, ignored by git, read by the Makefile (`-include .env`) |
| `src/main/java/**/Settings.java` | reads the variables with defaults |
| `docker-compose.yml` | `Makefile` |

## 4. How it works underneath

1. The operating system gives each process a table of `NAME=value` texts. A child process inherits its parent's table. In a shell you set one for a single command with `NAME=value command` or for the session with `export NAME=value`.
2. `make` reads `.env` through `-include .env` and `export`, which turns the variables into part of the environment of every command it runs.
3. Java reads them with `System.getenv()` (a map of all) or `System.getenv("NAME")`. In this repository only `main` calls `System.getenv()`, and `Settings.fromEnvironment(Map)` receives the map, so tests can pass their own.
4. A missing variable gets the default written in code; a malformed one fails at start-up with a message that names the variable.
5. Variable names are case-sensitive and by convention upper case.

## 5. Walkthrough of this repository's use

### `.env.example`, line by line

```properties
# Every variable the tool reads. None is a secret. Copy to .env to use with make (optional).
```

A comment: every variable the tool reads, none is a secret, copy to `.env` to use with `make` (optional). A line `NAME=value` sets an **environment variable**; a line starting with `#` is a comment.

> New words:
>
> - **environment variable**: a named text setting that a program inherits from the process that started it, for example `LOG_LEVEL=DEBUG`. Programs read them to be configured without changing code.

```properties
# Report currency (ISO 4217). Rows in any other currency are rejected.
SPEND_CURRENCY=USD
```

`SPEND_CURRENCY`: the report currency as a three-letter ISO 4217 code. Rows in any other currency are rejected. Read by `Settings.fromEnvironment`; the `--currency` option overrides it.

```properties
# How many largest purchases to list.
SPEND_TOP=5
```

`SPEND_TOP`: how many largest purchases to list. Read by `Settings`; `--top` overrides it. It must be at least 1.

```properties
# Rejected rows kept for display; the count is always exact.
SPEND_MAX_REJECTED=1000
```

`SPEND_MAX_REJECTED`: how many rejected rows are kept for display. The printed count is always exact. 0 is allowed; negative values are refused.

```properties
# Log level on standard error: TRACE, DEBUG, INFO, WARN, ERROR.
LOG_LEVEL=INFO
```

`LOG_LEVEL`: the level of the log lines on standard error (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`), read by `logback.xml` (see `logback.md`).

## 6. Try it yourself

- `cp .env.example .env`, change a value, run `make run` (spending/link) and watch the effect. Remove `.env` afterwards.
- `env | grep -i spend` (or `LINKCHECK`, `CACHE_MODE`). Expected: nothing, until you export a variable.
- `SPEND_TOP=2 make run` (spending report) or `LINKCHECK_PERMITS=1 make run` (link checker). Expected: the setting takes effect for that one run.

## 7. Mistakes people make

- Putting a real secret in `.env.example`: it is committed and public. None of the variables here is a secret.
- Setting a variable in one terminal and running the program in another: environments are per process.
- Quoting: `NAME="a b"` in a `.env` read by Make keeps the quotes as part of the value; values here need no spaces.
- Expecting a change of `.env` to affect a running program: it is read at start-up.

## 8. Check yourself

1. What is an environment variable?
   **Answer:** A named text value that the operating system passes to a program when it starts.
2. Why is `.env.example` committed but `.env` not?
   **Answer:** The example lists the settings with safe defaults; `.env` may hold personal values.
3. Which Java call reads the environment?
   **Answer:** `System.getenv()`.
4. What happens if a value is malformed?
   **Answer:** The program refuses to start and names the variable in the message.
5. How does `make` pass `.env` to the programs it runs?
   **Answer:** `-include .env` reads it and `export` makes the variables part of the environment of every command.

## 9. Further reading

- The Twelve-Factor App, config: https://12factor.net/config
- Java `System.getenv`: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/System.html#getenv()
