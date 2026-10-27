# The .env file format (.env.example)

## 1. What it is

A `.env` file is a plain text list of `NAME=value` lines (lines starting with `#` are comments). Programs and tools read it to set **environment variables**: named settings that every process inherits from the process that started it. Applications read them instead of hard-coding values (the *twelve-factor* way of configuring a program).

## 2. Why this repository uses it

The repository promises configuration from the environment with validated defaults, and `.env.example` with every variable and no secret. The example is a safe, committed copy; a real `.env` (ignored by Git) overrides it.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `.env.example` | every variable with a comment and a default |
| `Makefile` | `-include .env` and `export` hand the values to every recipe |
| `Settings.java` (infrastructure) | reads the variables and validates them |
| `docker-compose.yml` | (none in this project) |

## 4. How it works underneath

- Every process has an environment: a table of strings. A child process receives a copy of its parent's table. `NAME=value command` adds one variable for just that command.
- Make's `-include .env` reads the file; `export` passes all Make variables to recipe commands.
- Docker Compose reads a `.env` next to the compose file for `${...}` substitution in the YAML.
- Java reads variables with `System.getenv()`. Each `Settings` class validates the text (a number, a known mode) and stops with a clear message that names the variable.

## 5. Walkthrough of this repository's use

```
# Every variable the tool reads. None is a secret. Copy to .env to use with make (optional).
```

Each block is a comment that says what the variable does, followed by `NAME=default`. The defaults match the defaults coded in `Settings`, so deleting a line changes nothing. None of the values is a secret.

The code side (`Settings.fromEnvironment`):

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

The method takes the environment as a `Map` parameter (tests pass a plain map; the program passes `System.getenv()`), applies a default when a name is missing, and turns a bad value into an error that names the variable.

## 6. Try it yourself

- `cat .env.example`. Expected: the commented variables.
- `cp .env.example .env` then edit one value and run the program. Expected: the new value is used.
- Without a file: `NAME=value ` before a command. For example `LOG_LEVEL=DEBUG java -jar target/spend.jar ...`. Expected: more log lines.
- `env | grep LOG_LEVEL`. Expected: nothing unless you set it.

## 7. Mistakes people make

- Committing `.env` with real secrets. It is in `.gitignore`.
- Spaces around `=` in a `.env` file: `NAME = value` is wrong in many tools.
- Quoting differences between tools (Make takes the text literally; Compose strips quotes). The values here need none.
- Reading variables all over the code. Only `Settings` does.

## 8. Check yourself

1. What is an environment variable?
   **Answer:** A named string setting that a process inherits from its parent.
2. Why commit `.env.example` but not `.env`?
   **Answer:** The example documents names and safe defaults; the real file may contain secrets.
3. What does `-include .env` do?
   **Answer:** Reads the file if it exists and ignores it if it does not.
4. Where does the Java code read settings?
   **Answer:** In `Settings.fromEnvironment`, from a map (normally `System.getenv()`).
5. What happens if a value is not a number?
   **Answer:** `Settings` throws an error that names the variable, and the program reports it and stops.

## 9. Further reading

- The Twelve-Factor App, config: https://12factor.net/config
- Docker Compose `.env` files: https://docs.docker.com/compose/how-tos/environment-variables/set-environment-variables/
- GNU Make `include`: https://www.gnu.org/software/make/manual/make.html#Include
