# Maven Enforcer Plugin (3.6.3)

## 1. What it is

Most checks look at the program: do the tests pass, is the code formatted. The Enforcer plugin checks the **build setup** instead: Is the right Java installed? Are all versions pinned? Do two libraries disagree about a third? A **rule** is one such condition. If a rule fails, the build stops at once with a message, before anything is compiled.

## 2. Why this repository uses it

The repository promises that builds are reproducible: exact versions, no surprises. Enforcer turns that promise into a check that runs on every build, locally and in CI, so the promise cannot silently break.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | the plugin, its `enforce-build-rules` execution and the five rules |
| `pom.xml` `pluginManagement` | pins the plugins that rule `requirePluginVersions` would otherwise reject |

## 4. How it works underneath

1. Maven starts, reads the pom and runs the phases in order. In `validate`, the goal `enforcer:enforce` runs.
2. Each rule inspects something Maven already knows: the running Java and Maven versions, the declared dependencies, the resolved dependency tree, the list of plugins.
3. Passing rules print `Rule N: ... passed` at INFO level (rules are numbered from 0, in the order written). The first failing rule prints `[ERROR] Rule N: ... failed with message:` and its explanation, and the build ends with `BUILD FAILURE`.
4. Because it runs first, a wrong Java version is reported by a clear Enforcer message and not by an obscure compiler error later.

## 5. Walkthrough of this repository's use

```xml
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-enforcer-plugin</artifactId>
        <version>${enforcer-plugin.version}</version>
```

The opening of the plugin entry and its **coordinates (groupId, artifactId, version)**: group `org.apache.maven.plugins`, artifact `maven-enforcer-plugin`, version from the property `enforcer-plugin.version` (3.6.3).

> New words:
>
> - **coordinates (groupId, artifactId, version)**: the three values that identify a library or plugin in the Maven world: `groupId` says who made it (a reversed web name such as `org.slf4j`), `artifactId` is its own name (`slf4j-api`), `version` says which release. Together they are written `groupId:artifactId:version` and tell Maven exactly which file to fetch from the repository.

```xml
        <executions>
          <execution>
            <id>enforce-build-rules</id>
            <goals>
              <goal>enforce</goal>
            </goals>
```

An **execution** is one configured run. This one has the id `enforce-build-rules` (any name; it shows in Maven's output) and one **goal**, `enforce`. No `<phase>` is written, so it uses the goal's default phase, `validate`: the very first phase, so a broken setup stops the build before anything is compiled.

> New words:
>
> - **execution**: one configured run of a plugin goal inside a `<plugin>`: an optional `<id>` (its name), the `<phase>` it is bound to, its `<goals>` and its own `<configuration>`.
> - **goal**: one named job of a plugin, written `plugin:goal`, such as `spotless:check` or `jacoco:report`.

```xml
            <configuration>
              <rules>
```

The settings for this run. `<rules>` lists the rules; every child element is one rule, named after it.

```xml
                <requireJavaVersion>
                  <version>[${java.release},)</version>
                </requireJavaVersion>
```

Rule 0, `requireJavaVersion`: the JDK that runs Maven must satisfy the **version range** in `<version>`. `[${java.release},)` becomes `[25,)`: 25 or newer. The `${java.release}` reuses the property from the top of the pom.

> New words:
>
> - **version range**: `[25,)` means "25 or higher": a square bracket includes the end, a round bracket leaves it open, and nothing after the comma means no upper limit.

```xml
                <requireMavenVersion>
                  <version>[3.9,)</version>
                </requireMavenVersion>
```

Rule 1, `requireMavenVersion`: Maven itself must be 3.9 or newer (`[3.9,)`).

```xml
                <banDuplicatePomDependencyVersions/>
```

Rule 2: the same library may not be declared twice in the pom's dependency list. The self-closing tag means the rule has no settings.

```xml
                <requireUpperBoundDeps/>
```

Rule 3: when two libraries need different versions of a third, Maven picks one; this rule fails the build if the picked version is *lower* than one that is requested somewhere in the tree. In this repository `logback-classic` 1.6.6 itself asks for `slf4j-api` 2.0.19, the pom asks for 2.0.20, and 2.0.20 wins, so the rule passes. (The stricter `dependencyConvergence` rule was tried and failed on exactly this harmless difference.)

```xml
                <requirePluginVersions/>
              </rules>
            </configuration>
          </execution>
        </executions>
      </plugin>
```

Rule 4: every plugin that Maven would run must have an explicit version, so a Maven upgrade cannot change which plugin versions are used. This is why `pluginManagement` pins clean, resources, jar, install, deploy and site. The closing tags end `<rules>`, `<configuration>`, the execution and the plugin.

## 6. Try it yourself

- `./mvnw -B validate | grep Rule`. Expected: `Rule 0` to `Rule 4` each ending `passed` (the names are `RequireJavaVersion`, `RequireMavenVersion`, `BanDuplicatePomDependencyVersions`, `RequireUpperBoundDeps`, `RequirePluginVersions`).
- Break a rule on purpose in a scratch copy: remove a `<version>` line from a plugin in `pluginManagement` and run `./mvnw -B validate`. Expected: `RequirePluginVersions failed` naming the plugin.
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
   **Answer:** At the start of every Maven build, in the validate phase.

## 9. Further reading

- Maven Enforcer 3.6.3: https://maven.apache.org/enforcer/maven-enforcer-plugin/
- Standard rules: https://maven.apache.org/enforcer/enforcer-rules/index.html
- Version ranges: https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html
