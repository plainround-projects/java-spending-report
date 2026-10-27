# JaCoCo (0.8.16)

## 1. What it is

**Code coverage** tells you how much of the code the tests actually ran. **JaCoCo** (Java Code Coverage) measures it. A Java **agent** is a small program attached to the JVM at start-up (`-javaagent:`); it watches every class as it loads and records which lines and branches execute.

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

- JaCoCo **instruments** classes on the fly: when a class loads, the agent adds counters to it. Nothing is changed on disk.
- When the test JVM ends, the agent writes all counters to `target/jacoco.exec`.
- The `report` goal reads that file plus `target/classes` and computes, per package and class, the covered and missed **instructions**, **lines**, **branches** and **methods**.
- `check` compares those numbers with the rules and fails the build with `Coverage checks have not been met` if one rule fails.

## 5. Walkthrough of this repository's use

```xml
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>${jacoco-plugin.version}</version>
        <executions>
          <execution>
            <id>prepare-agent</id>
            <goals>
              <goal>prepare-agent</goal>
            </goals>
          </execution>
          <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
              <goal>report</goal>
            </goals>
          </execution>
          <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals>
              <goal>check</goal>
            </goals>
            <configuration>
              <rules>
                <rule>
                  <element>PACKAGE</element>
                  <includes>
                    <include>com.plainround.spend.domain</include>
                  </includes>
                  <limits>
                    <limit>
                      <counter>LINE</counter>
                      <value>COVEREDRATIO</value>
                      <minimum>0.95</minimum>
                    </limit>
                    <limit>
                      <counter>BRANCH</counter>
                      <value>COVEREDRATIO</value>
                      <minimum>0.90</minimum>
                    </limit>
                  </limits>
                </rule>
                <rule>
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

- Three **executions** (named runs of the plugin) with their goals:
- `prepare-agent` runs before the tests. It sets the Maven property `argLine` to `-javaagent:.../org.jacoco.agent-0.8.16-runtime.jar=destfile=target/jacoco.exec`. Surefire (see `maven.md`) puts `@{argLine}` on the test JVM's command line, so the **agent** (a program that watches the JVM) records which lines run.
- `report` runs in the `test` phase after the tests and turns `target/jacoco.exec` into a readable report: `target/site/jacoco/index.html` for people, `jacoco.csv` and `jacoco.xml` for programs.
- `check` runs in `verify` and fails the build if the coverage rules below are not met.

The rules: 

- Rule 1, `element` `PACKAGE` including only `com.plainround.spend.domain`: at least 95% of lines (`LINE`, `COVEREDRATIO` 0.95) and 90% of branches (`BRANCH`) covered. A **branch** is one way a decision can go (the `if` side or the `else` side).
- Rule 2, `element` `BUNDLE` (the whole project): at least 85% of lines.

So the domain, the code with the business rules, must be almost fully tested, and the rest at least mostly.

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
