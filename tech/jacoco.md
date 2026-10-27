# JaCoCo (0.8.16)

## 1. What it is

**Code coverage** tells you how much of the program's code the tests actually ran. If a test run never reaches a line, no test can have caught a mistake on it. **JaCoCo** (Java Code Coverage) measures it. It works through a Java **agent**: a small program attached to the JVM at start-up with the option `-javaagent:`. The agent watches every class as it loads and counts which lines and branches run.

## 2. Why this repository uses it

The repository rule: "coverage reported, with the domain at or near full coverage". JaCoCo reports it and the `check` goal enforces the numbers, so coverage cannot silently drop.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | the plugin and its three executions, the thresholds |
| `pom.xml` Surefire `argLine` | passes the agent option to the test JVM |
| `target/jacoco.exec` | the raw recording (binary, not committed) |
| `target/site/jacoco/` | the HTML, CSV and XML report (not committed) |
| `Makefile` | `test` runs `clean verify`, so the check runs; `up`/`build` pass `-Djacoco.skip=true` |

## 4. How it works underneath

1. JaCoCo **instruments** classes on the fly: when a class loads, the agent adds counters to its bytecode in memory. Nothing is changed on disk.
2. When the test JVM ends, the agent writes all counters to `target/jacoco.exec`.
3. The `report` goal reads that file plus `target/classes` and computes, per package and class, the covered and missed **instructions**, **lines**, **branches** and **methods**.
4. `check` compares those numbers with the rules and fails the build with `Coverage checks have not been met` if one rule fails; otherwise it prints `All coverage checks have been met.`

## 5. Walkthrough of this repository's use

```xml
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>${jacoco-plugin.version}</version>
        <executions>
          <execution>
```

The plugin entry: group `org.jacoco`, artifact `jacoco-maven-plugin`, version 0.8.16 from the property.

```xml
            <id>prepare-agent</id>
            <goals>
              <goal>prepare-agent</goal>
            </goals>
          </execution>
          <execution>
```

Execution 1, `prepare-agent`. The goal of the same name runs early (its default phase is `initialize`). It sets the Maven property `argLine` to `-javaagent:<path to the JaCoCo agent jar>=destfile=target/jacoco.exec`. Surefire (see `maven.md`) puts `@{argLine}` on the test JVM's command line, which attaches the **agent**, a program that watches the JVM, to the tests.

```xml
            <id>report</id>
            <phase>test</phase>
            <goals>
              <goal>report</goal>
            </goals>
          </execution>
          <execution>
```

Execution 2, `report`, bound to the `test` phase, so it runs right after the tests. The goal `report` reads the recording `target/jacoco.exec` and the compiled classes and writes the report: `target/site/jacoco/index.html` for people, plus `jacoco.csv` and `jacoco.xml` for programs.

```xml
            <id>check</id>
            <phase>verify</phase>
            <goals>
              <goal>check</goal>
            </goals>
            <configuration>
```

Execution 3, `check`, bound to `verify`: the last phase of `./mvnw verify`. The goal `check` compares the recording with the rules in its `<configuration>` and fails the build if any is broken.

```xml
              <rules>
                <rule>
                  <element>PACKAGE</element>
                  <includes>
                    <include>com.plainround.spend.domain</include>
                  </includes>
                  <limits>
                    <limit>
```

The first `<rule>` has `<element>PACKAGE</element>`: it applies to each Java package that matches `<includes>`, here only `com.plainround.spend.domain`, the package with the business rules.

```xml
                      <counter>LINE</counter>
                      <value>COVEREDRATIO</value>
                      <minimum>0.95</minimum>
                    </limit>
                    <limit>
```

Its first `<limit>`: the **counter** `LINE` (lines of source that were run), measured as `COVEREDRATIO` (covered lines divided by all lines), must be at least `0.95`, i.e. 95%.

```xml
                      <counter>BRANCH</counter>
                      <value>COVEREDRATIO</value>
                      <minimum>0.90</minimum>
                    </limit>
                  </limits>
                </rule>
                <rule>
```

The second `<limit>`: the counter `BRANCH` (each possible outcome of a decision, such as the `if` side and the `else` side) must be covered to at least `0.90`, 90%.

```xml
                  <element>BUNDLE</element>
                  <limits>
                    <limit>
                      <counter>LINE</counter>
                      <value>COVEREDRATIO</value>
                      <minimum>0.85</minimum>
                    </limit>
                  </limits>
                </rule>
              </rules>
            </configuration>
          </execution>
        </executions>
      </plugin>
```

The second `<rule>`: `BUNDLE` means the whole project. Its one limit demands at least 85% of all lines covered. The domain is therefore held to a stricter standard than the rest. The remaining closing tags end the rules, the execution and the plugin.

## 6. Try it yourself

- `./mvnw -B clean verify | grep -E 'coverage|Tests run:.*Fail'`. Expected: `All coverage checks have been met.`
- Open `target/site/jacoco/index.html` in a browser. Expected: a table by package; the `domain` row is at or near 100%.
- `head -5 target/site/jacoco/jacoco.csv`. Expected: a header line `GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,...` and one row per class.
- Skip it for a quick build: `./mvnw -q -B package -DskipTests -Djacoco.skip=true`. Expected: success without the agent.

## 7. Mistakes people make

- Chasing 100% of lines with tests that assert nothing. Coverage shows what ran, not what was checked; the tests here assert behavior.
- Forgetting the agent option: if Surefire's `argLine` does not contain `@{argLine}`, JaCoCo records nothing and the report is empty.
- Measuring the shaded jar: the report reads `target/classes`; if a stale shaded `target/` exists you get warnings about overlapping classes, so use `clean`.
- Setting a threshold on the whole project only: a well-tested part can hide an untested core. The separate `domain` rule prevents that.

## 8. Check yourself

1. What does a JaCoCo agent do?
   **Answer:** It watches classes as they load and counts which lines and branches run.
2. Where is the raw recording written?
   **Answer:** `target/jacoco.exec`.
3. Which package has the strictest rule here?
   **Answer:** `com.plainround.spend.domain`: 95% of lines, 90% of branches.
4. What is a branch?
   **Answer:** One possible outcome of a decision, such as the `if` side or the `else` side.
5. Which goal makes the build fail?
   **Answer:** `jacoco:check`, in the `verify` phase.

## 9. Further reading

- JaCoCo 0.8.16 Maven plugin: https://www.jacoco.org/jacoco/trunk/doc/maven.html
- check goal and rules: https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html
- Counters (instructions, lines, branches): https://www.jacoco.org/jacoco/trunk/doc/counters.html
