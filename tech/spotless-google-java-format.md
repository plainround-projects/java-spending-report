# Spotless (3.10.4) and google-java-format (1.36.1)

## 1. What it is

A **formatter** is a program that rewrites source code into one fixed layout (indentation, line breaks, spaces, order of imports), so that people never argue about it and every file looks as if one person wrote it. **google-java-format** is Google's formatter for Java. **Spotless** is a build plugin that runs formatters as part of the Maven build, in one of two modes: `check` (fail if any file would change) or `apply` (rewrite the files).

## 2. Why this repository uses it

The repository rule is "formatter and linter configured in the repository and run in CI". The formatter handles layout; Checkstyle (see `checkstyle.md`) handles the rules a formatter cannot (naming, Javadoc, leftovers).

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | plugin and formatter versions, the `<java>` steps |
| `.mvn/jvm.config` | opens JDK internals the formatter needs |
| `Makefile` | `lint` runs `spotless:check`, `format` runs `spotless:apply` |
| `.editorconfig` | the same indentation (2 spaces) for editors |
| `.github/workflows/ci.yml` | runs `make lint` |

## 4. How it works underneath

1. Spotless loads the formatter jar in its own class loader (a separate area of the JVM for loading classes), finds all `src/**/*.java` files, runs the formatter on each in memory and compares the result with the file on disk.
2. `check` reports the files that differ and fails the build; `apply` writes the formatted text back to the files.
3. The formatter never changes what the code does, only its layout. It does not run the compiler, so it cannot find type errors.
4. To parse Java, google-java-format uses the JDK compiler's internal classes; `.mvn/jvm.config` opens them (see `maven-wrapper.md`).

## 5. Walkthrough of this repository's use

### The Makefile targets that call Spotless

`make lint` runs `$(MVN) -B spotless:check checkstyle:check clean compile test-compile`: `spotless:check` comes first so formatting problems are found before anything else. `make format` runs `$(MVN) -B spotless:apply` to fix the files in place. The CI runs `make lint`, so unformatted code cannot be merged.

### The plugin entry in `pom.xml`

```xml
      <plugin>
        <groupId>com.diffplug.spotless</groupId>
        <artifactId>spotless-maven-plugin</artifactId>
        <version>${spotless-plugin.version}</version>
```

The plugin entry: group `com.diffplug.spotless`, artifact `spotless-maven-plugin`, version from the property `spotless-plugin.version` (3.10.4). It has no `<executions>` block, so it does nothing during a normal build; it runs only when the Makefile names its goals.

```xml
        <configuration>
          <java>
```

The settings. `<java>` configures the steps Spotless applies to Java files; by default it covers `src/main/java/**/*.java` and `src/test/java/**/*.java`.

```xml
            <googleJavaFormat>
              <version>${google-java-format.version}</version>
              <style>GOOGLE</style>
            </googleJavaFormat>
```

The first step: run **google-java-format**. `<version>` pins the formatter to 1.36.1 (a separate download from the plugin); `<style>GOOGLE</style>` selects Google's layout (2-space indent, 100-column lines). The alternative style `AOSP` uses 4 spaces.

```xml
            <removeUnusedImports/>
```

The second step deletes `import` lines that nothing in the file uses.

```xml
            <importOrder/>
          </java>
        </configuration>
      </plugin>
```

The third step sorts the imports in the plugin's standard order (an empty element means: default settings). The remaining closing tags end `<java>`, `<configuration>` and the plugin.

## 6. Try it yourself

- `./mvnw -q spotless:check`. Expected: no output when all files are formatted.
- Put an extra blank line and wrong indentation in a Java file in a scratch copy, then `./mvnw -q spotless:check`. Expected: `The following files had format violations` and a diff.
- `./mvnw -q spotless:apply`, then `git diff`. Expected: the file is back to the standard layout.
- `make lint`. Expected: Spotless and Checkstyle pass, then `BUILD SUCCESS`.

## 7. Mistakes people make

- Fighting the formatter by hand. Run `make format` and commit the result.
- Mixing formatter versions between machines: the version is pinned in the pom, so the output is the same everywhere.
- Using a formatter version that the plugin cannot load: 1.37.0 failed here with an `InvocationTargetException` and no message; 1.36.1 works.
- Expecting the formatter to enforce naming or Javadoc. That is Checkstyle's job.

## 8. Check yourself

1. What is the difference between `spotless:check` and `spotless:apply`?
   **Answer:** `check` fails if any file would change; `apply` rewrites the files.
2. Which style does this repository use?
   **Answer:** `GOOGLE` (2-space indent, 100-column lines).
3. Why is the formatter version pinned?
   **Answer:** Different versions format differently; pinning makes every machine produce the same layout, and 1.37.0 does not work with this Spotless on Java 25.
4. Why does `.mvn/jvm.config` matter for formatting?
   **Answer:** It opens JDK compiler internals that the formatter's parser needs.
5. Can the formatter find a type error?
   **Answer:** No. It only changes layout; the compiler finds type errors.

## 9. Further reading

- Spotless Maven plugin 3.10.4: https://github.com/diffplug/spotless/tree/main/plugin-maven
- google-java-format 1.36.1: https://github.com/google/google-java-format
- Google Java Style Guide: https://google.github.io/styleguide/javaguide.html
