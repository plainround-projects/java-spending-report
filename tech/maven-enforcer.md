# Maven Enforcer Plugin (3.6.3)

## 1. What it is

The Enforcer plugin is a Maven plugin that checks conditions about the build instead of the code. A **rule** is one condition, such as "Java 25 or newer". If a rule fails, the build stops with a message before anything is compiled.

## 2. Why this repository uses it

The repository promises that builds are reproducible: exact versions, no surprises. Enforcer turns that promise into a check that runs on every build, locally and in CI.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | the plugin, its `enforce-build-rules` execution and the five rules |
| `pom.xml` `pluginManagement` | pins the plugins that rule `requirePluginVersions` would otherwise reject |

## 4. How it works underneath

- Maven starts, reads the pom, and before compiling runs the goal `enforcer:enforce` (it is bound to the `validate` phase by default).
- Each rule inspects the loaded model: the running Java, the running Maven, the declared dependencies, the resolved dependency graph, the plugin list.
- The first failing rule stops the build with `[ERROR] Rule N: ... failed with message:`; passing rules print `Rule N: ... passed` at INFO level.

## 5. Walkthrough of this repository's use

```xml
        <artifactId>maven-enforcer-plugin</artifactId>
        <version>${enforcer-plugin.version}</version>
        <executions>
          <execution>
            <id>enforce-build-rules</id>
            <goals>
              <goal>enforce</goal>
            </goals>
            <configuration>
              <rules>
                <requireJavaVersion>
                  <version>[${java.release},)</version>
                </requireJavaVersion>
                <requireMavenVersion>
                  <version>[3.9,)</version>
                </requireMavenVersion>
                <banDuplicatePomDependencyVersions/>
                <requireUpperBoundDeps/>
                <requirePluginVersions/>
              </rules>
            </configuration>
          </execution>
        </executions>
      </plugin>
```

The Enforcer plugin fails the build when a rule about the build itself is broken. The `execution` named `enforce-build-rules` runs the goal `enforce` early in every build. Each element inside `<rules>` is one rule:

- `requireJavaVersion` with `[25,)`: the JDK running Maven must be 25 or newer. In Maven's range notation `[` includes the number and `)` leaves the upper end open. The text `${java.release}` reuses the property from the top of the pom.
- `requireMavenVersion` with `[3.9,)`: Maven 3.9 or newer.
- `banDuplicatePomDependencyVersions`: the same dependency may not be declared twice with different versions in one pom.
- `requireUpperBoundDeps`: if two libraries need different versions of a third, the build must use the higher one, never a lower one. Here `logback-classic` 1.6.6 itself asks for `slf4j-api` 2.0.19, and the pom asks for 2.0.20; the higher wins and the rule passes. (The stricter rule `dependencyConvergence` was tried and failed on exactly this, so it was replaced.)
- `requirePluginVersions`: every plugin that runs must have an explicit version (no defaults that change with the Maven release). This is why `pluginManagement` pins clean, resources, jar, install, deploy and site.

## 6. Try it yourself

- `./mvnw -B validate | grep Rule`. Expected: lines `Rule 2: ...BanDuplicatePomDependencyVersions passed`, `Rule 3: ...RequireUpperBoundDeps passed`, `Rule 4: ...RequirePluginVersions passed` (rules 0 and 1 are the Java and Maven versions).
- Break a rule on purpose in a scratch copy: remove a `<version>` line from a plugin and run `./mvnw -B validate`. Expected: `RequirePluginVersions failed` naming the plugin.
- Change `<java.release>` to 99 in a scratch copy. Expected: the Java version rule fails with a clear message.

## 7. Mistakes people make

- Using `dependencyConvergence` when a library legitimately pins a lower version than you want: it flags every disagreement, even harmless ones. `requireUpperBoundDeps` only flags the dangerous case (the chosen version is lower than a needed one).
- Letting Maven pick default plugin versions, so a Maven upgrade changes the build.
- Reading the rule list as documentation of the code; it is about the build only.

## 8. Check yourself

1. Does Enforcer check the Java code?
   **Answer:** No. It checks the build setup: versions, dependencies, plugins.
2. What does `requirePluginVersions` need?
   **Answer:** An explicit version for every plugin that runs, including the default lifecycle ones.
3. Why was `dependencyConvergence` replaced by `requireUpperBoundDeps`?
   **Answer:** `logback-classic` asks for `slf4j-api` 2.0.19 and the pom for 2.0.20; `dependencyConvergence` fails on any such difference, the upper-bound rule accepts it because the higher version wins.
4. What does `[25,)` mean?
   **Answer:** 25 or any higher version.
5. When does the rule set run?
   **Answer:** At the start of every Maven build, at the validate phase.

## 9. Further reading

- Maven Enforcer 3.6.3: https://maven.apache.org/enforcer/maven-enforcer-plugin/
- Standard rules: https://maven.apache.org/enforcer/enforcer-rules/index.html
- Version ranges: https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html
