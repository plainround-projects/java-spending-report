# Spotless (3.10.4) and google-java-format (1.36.1)

## 1. What it is

A **formatter** rewrites source code into one fixed layout, so people never argue about spaces and line breaks. **google-java-format** is Google's formatter for Java. **Spotless** is a build plugin that runs formatters (and other fixers) as part of the Maven build, either to check or to rewrite the files.

## 2. Why this repository uses it

The repository rule is "formatter and linter configured in the repository and run in CI". The formatter handles layout; Checkstyle (see `checkstyle.md`) handles the rules a formatter cannot (naming, Javadoc, leftovers).

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | plugin and formatter versions, the `<java>` settings |
| `.mvn/jvm.config` | opens JDK internals the formatter needs |
| `Makefile` | `lint` runs `spotless:check`, `format` runs `spotless:apply` |
| `.editorconfig` | the same indentation (2 spaces) for editors |
| `.github/workflows/ci.yml` | runs `make lint` |

## 4. How it works underneath

- Spotless loads the formatter jar in its own class loader, finds all `src/**/*.java` files, runs the formatter on each in memory and compares the result with the file on disk.
- `check` reports the files that differ and fails; `apply` writes the formatted text back.
- The formatter never changes what the code does, only its layout. It does not run the compiler, so it cannot find type errors.

## 5. Walkthrough of this repository's use

```xml
        <artifactId>spotless-maven-plugin</artifactId>
        <version>${spotless-plugin.version}</version>
        <configuration>
          <java>
            <googleJavaFormat>
              <version>${google-java-format.version}</version>
              <style>GOOGLE</style>
            </googleJavaFormat>
            <removeUnusedImports/>
            <importOrder/>
          </java>
        </configuration>
      </plugin>
```

- `spotless-maven-plugin` 3.10.4 is the Maven plugin. It has two goals this repository uses: `spotless:check` (fail if any file would change) and `spotless:apply` (rewrite the files).
- `<googleJavaFormat>` picks the formatter **google-java-format**, a program that parses Java source and prints it again in one fixed layout (2-space indent, 100-column lines, one import per line, a fixed blank-line rule). `<style>GOOGLE</style>` is its default style (the other, `AOSP`, uses 4 spaces).
- `<version>${google-java-format.version}</version>` pins it to 1.36.1. The pom comment says why not 1.37.0: it failed inside this Spotless on Java 25.
- `<removeUnusedImports/>` deletes imports nothing uses. `<importOrder/>` sorts imports into the standard order. Both run in addition to the formatter.

```make
lint: ## Spotless (google-java-format) check, Checkstyle, compiler -Xlint:all -Werror
	$(MVN) -B spotless:check checkstyle:check clean compile test-compile
```

`make lint` calls `spotless:check` first, so formatting problems are found before anything else. `make format` calls `spotless:apply` to fix them. The CI runs `make lint`, so unformatted code cannot be merged.

```text
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
```

The formatter parses Java with the JDK compiler's internal classes, so `.mvn/jvm.config` opens them (see `maven-wrapper.md`).

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
