# Maven (3.9.16, through the wrapper)

## 1. What it is

Writing a program is only the first step. Before it can run, someone has to turn the source text into a form the computer can run (**compiling**), run the checks that prove it works (**testing**), and put the result into a single file that can be copied around (**packaging**). Doing these steps by hand, in the right order, with the right libraries, on every computer, is slow and error-prone. A **build tool** does all of it from one command. **Maven** is the most widely used build tool for Java. You describe the project once in a text file named `pom.xml` ("Project Object Model"), and Maven works out what to do. Maven is itself a Java program, so it needs a Java installation (a JDK) to run.

## 2. Why this repository uses it

The lesson asks for a project that anyone can build with one command and get the same result. Maven provides this: the same `pom.xml` produces the same jar, the same test results and the same quality checks on any computer with a JDK. The pom also states, in one place, which libraries the project uses and in which versions, which is what makes the build repeatable months later. The checked-in *wrapper* (see `maven-wrapper.md`) removes the need to install Maven at all.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | the whole build description: name, libraries, plugins and their settings (walked through line by line below) |
| `mvnw`, `mvnw.cmd`, `.mvn/` | the Maven wrapper that downloads and starts the pinned Maven (see `maven-wrapper.md`) |
| `Makefile` | every target that builds or checks calls `./mvnw` (listed in the table below) |
| `Dockerfile` | its build stage calls `./mvnw` twice |
| `src/main/java`, `src/test/java`, `src/main/resources`, `src/test/resources` | the standard folders Maven expects, so the pom does not have to name them |
| `target/` | everything Maven produces (classes, reports, the jar); deleted by `clean`, never committed (it is in `.gitignore`) |

## 4. How it works underneath

1. **You type `./mvnw ...`.** The wrapper script starts a JVM (a running Java) that runs Maven. Maven reads `pom.xml`. It also reads the built-in "super pom", a pom that every pom silently inherits from; it supplies defaults such as the standard folder names and the plugins for the default lifecycle. The combination is the *effective pom*.
2. **Maven builds a plan.** The default **lifecycle** is a fixed list of steps, each called a **phase**: `validate`, `compile`, `test`, `package`, `verify`, `install`, `deploy` (and a few others in between). Asking for a phase runs it and every earlier phase. A phase is only a name; the work is done by the jobs of **plugin**s, each job being a **goal**, *bound* to phases. For a project with `<packaging>jar</packaging>`, Maven binds the standard ones: `resources:resources` and `compiler:compile` to `process-resources` and `compile`, `resources:testResources` and `compiler:testCompile`, `surefire:test` to `test`, and `jar:jar` to `package`. The pom adds more: Enforcer, JaCoCo, Shade.
3. **The real order for `./mvnw -B clean verify` in this repository** (taken from an actual run): `clean:clean` (delete `target/`); `enforcer:enforce` (check the build rules); `jacoco:prepare-agent` (prepare the coverage option); `resources:resources` (copy `src/main/resources`, such as `logback.xml`, to `target/classes`); `compiler:compile` (compile the program); `resources:testResources` and `compiler:testCompile` (the same for the tests); `surefire:test` (run all tests); `jacoco:report` (write the coverage report); `jar:jar` (make the plain jar); `shade:shade` (replace it by the single jar with all libraries); `jacoco:check` (fail if coverage rules are broken). The lines `Rule 0` to `Rule 4 ... passed` come from Enforcer, the lines `Tests run: N, Failures: 0, Errors: 0, Skipped: 0` from Surefire, and `All coverage checks have been met.` from JaCoCo.
4. **Libraries.** Maven reads the **dependency** list, takes the **coordinates (groupId, artifactId, version)** of each, and looks in the local repository (`~/.m2/repository`) for the file. If it is missing it downloads it from **Maven Central** over HTTPS, together with the libraries that library needs (**transitive dependency**). If two paths want different versions of the same library, Maven uses the one nearest to your project in the dependency tree, and the Enforcer rule `requireUpperBoundDeps` checks that this never means a lower version than someone asked for.
5. **Output.** Everything written goes to `target/`: `target/classes` (compiled program), `target/test-classes`, `target/surefire-reports` (test results), `target/site/jacoco` (coverage) and the jar `target/spend.jar`.
6. **Command-line options** used in this repository: `-q` (quiet), `-B` (batch mode: plain text without colors or progress bars, best for logs and CI), `-D name=value` (sets a property, for example `-DskipTests`), `-Dtest=ClassName` (runs only that test class), `-o` (offline: do not touch the network).

> New words:
>
> - **lifecycle**: the fixed ordered list of phases. `mvn package` runs validate, compile, test and package in that order.
> - **phase**: a named step in Maven's fixed sequence: `validate`, `compile`, `test`, `package`, `verify`, `install`, `deploy`. Asking Maven for a phase runs all the earlier phases first. A plugin goal is *bound* to a phase to say when it runs.
> - **plugin**: a program that Maven runs to do one kind of work (compile, test, package, format, check). Plugins are Maven's only way of doing anything; Maven itself only reads the pom and runs plugins in order.
> - **goal**: one named job of a plugin, written `plugin:goal`, such as `spotless:check` or `jacoco:report`.
> - **dependency**: a library your code needs. You list its coordinates and Maven downloads it. The libraries *it* needs are fetched too (transitive dependencies).
> - **coordinates (groupId, artifactId, version)**: the three values that identify a library or plugin in the Maven world: `groupId` says who made it (a reversed web name such as `org.slf4j`), `artifactId` is its own name (`slf4j-api`), `version` says which release. Together they are written `groupId:artifactId:version` and tell Maven exactly which file to fetch from the repository.
> - **Maven Central**: the big public warehouse of Java libraries on the internet (`repo.maven.apache.org`). Maven downloads the files named by your coordinates from it, and keeps a copy in the folder `~/.m2/repository` on your computer, so each file is downloaded only once.
> - **transitive dependency**: a library that one of your libraries needs. You do not list it, but Maven finds it in that library's own pom and downloads it too.

## 5. Walkthrough of this repository's use

### Every Maven command line in this repository

| Command | Where | What each part means |
| --- | --- | --- |
| `./mvnw -q -B clean package -DskipTests -Djacoco.skip=true` | `Makefile` targets `up` (spending) / `build` (the other two); the Dockerfile build stage runs it without `clean` | `-q` quiet (only errors are printed). `-B` batch mode (no colors, no download progress bars). `clean` deletes `target/` first. `package` runs every phase up to `package`, so the compiled, shaded jar is made. `-DskipTests` compiles the tests but does not run them. `-Djacoco.skip=true` tells the JaCoCo plugin to do nothing, because the tests are not run and there would be nothing to measure. |
| `./mvnw -B clean verify` | `Makefile` target `test` | `verify` runs every phase up to `verify`: validate (Enforcer), compile, test (all tests, with coverage), package (jar and shaded jar) and verify (the JaCoCo coverage rules). Without `-q` the output shows every plugin and the `Tests run:` lines. |
| `./mvnw -B spotless:check checkstyle:check clean compile test-compile` | `Makefile` target `lint` | Several goals in one command, run left to right. `spotless:check` fails if any file is not formatted; `checkstyle:check` fails on any rule violation; `clean compile test-compile` then compiles main and test code from scratch with the strict compiler flags (`-Xlint:all -Werror`). No tests are run. |
| `./mvnw -B spotless:apply` | `Makefile` target `format` | Rewrites the sources into the standard layout. |
| `./mvnw -q -B clean` | `Makefile` target `down` (and `clean`) | Deletes the `target/` folder. |
| `./mvnw -B -q dependency:resolve` | `Dockerfile`, build stage | Downloads every dependency and plugin without compiling anything. The Dockerfile does this in its own step so that Docker can remember (cache) the downloaded libraries as long as `pom.xml` is unchanged. |
| `./mvnw -q -B test -Dtest=CsvRowParserTest -Djacoco.skip=true -Dspotless.check.skip=true` | `scripts/prove-quote-test.sh` | `test` runs up to the test phase. `-Dtest=CsvRowParserTest` runs only that test class. `-Djacoco.skip=true` skips coverage. `-Dspotless.check.skip=true` is passed in case a Spotless check is active, since the script deliberately edits a copy of the code. |

### The pom, line by line

The whole `pom.xml` follows, in pieces, from the first line to the last. Each piece is followed by its explanation. Nothing is left out.

```xml
<?xml version="1.0" encoding="UTF-8"?>
```

The first line is the **XML declaration**: it says this file is XML version 1.0 and is stored as UTF-8 text. XML is a way of writing structured data as nested **XML element**s. A `pom.xml` (Project Object Model) is the file in which Maven reads everything it must know about the project.

> New words:
>
> - **XML element**: a piece of an XML file written as an opening tag, some content, and a closing tag: `<version>0.1.0</version>`. The content may be text or other elements, which is how XML nests. An element with nothing inside can be written as one self-closing tag: `<requirePluginVersions/>`.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
```

`<project>` is the root element; everything else sits inside it, until the closing `</project>` on the last line. The three lines of **XML attribute**s after the name are the same in every pom: `xmlns` names the **XML namespace** of Maven's pom vocabulary, `xmlns:xsi` declares the prefix `xsi` for the standard XML-schema vocabulary, and `xsi:schemaLocation` tells editors where the formal description (a schema) of the pom format is, so an editor can check and complete the file. Maven itself does not download that address.

> New words:
>
> - **XML attribute**: a name="value" pair written inside an opening tag, such as `implementation="..."` in `<transformer implementation="...">`. It adds information to that one element.
> - **XML namespace**: a name, written as a web address, that says which vocabulary an element belongs to. The address (`http://maven.apache.org/POM/4.0.0`) is only a name; nothing is downloaded from it when the file is read.

```xml
  <modelVersion>4.0.0</modelVersion>
```

The version of the pom format. Maven 3.9 understands only `4.0.0`.

```xml
  <groupId>com.plainround</groupId>
  <artifactId>spend</artifactId>
  <version>0.1.0</version>
  <packaging>jar</packaging>
  <name>spend</name>
  <description>Command-line spending report: exact money, bad rows reported instead of crashing.</description>
```

The project's own **coordinates (groupId, artifactId, version)**: group `com.plainround`, artifact `spend`, version `0.1.0`. `<packaging>jar</packaging>` says the build result is a **jar file**. `<name>` and `<description>` are for people; Maven prints the name in its output.

> New words:
>
> - **coordinates (groupId, artifactId, version)**: the three values that identify a library or plugin in the Maven world: `groupId` says who made it (a reversed web name such as `org.slf4j`), `artifactId` is its own name (`slf4j-api`), `version` says which release. Together they are written `groupId:artifactId:version` and tell Maven exactly which file to fetch from the repository.
> - **jar file**: a zip file that holds compiled Java classes (`.class` files) and resources, plus a manifest (`META-INF/MANIFEST.MF`) that describes the jar. `java -jar app.jar` runs the class named in the manifest.

```xml
  <properties>
    <java.release>25</java.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <main.class>com.plainround.spend.interfaces.cli.SpendCli</main.class>
```

The start of the **property** block. The first four lines set values Maven itself understands or the pom reuses. `java.release` (25) is the Java version the code is compiled for, used below by the compiler plugin and by the Enforcer. `project.build.sourceEncoding` and `project.reporting.outputEncoding` say that source files and generated reports are UTF-8 text; without them Maven would use the computer's default encoding, so an accented letter could compile differently on another computer. `main.class` is this project's own property: the class whose `main` method starts the program (see the Java note); the Shade plugin uses it.

> New words:
>
> - **property**: a named value in a `<properties>` block. Writing `${name}` anywhere else in the file inserts the value. It lets a number such as a version appear once.

```xml
    <slf4j.version>2.0.20</slf4j.version>
    <logback.version>1.6.6</logback.version>
    <junit.version>6.1.3</junit.version>
    <assertj.version>3.27.7</assertj.version>
    <archunit.version>1.5.1</archunit.version>
```

Five library versions, each defined once so that nothing else in the file has a number in it: SLF4J (the logging interface), Logback (the logging engine), JUnit (the test framework), AssertJ (readable assertions) and ArchUnit (architecture tests).

```xml
    <compiler-plugin.version>3.16.0</compiler-plugin.version>
    <surefire-plugin.version>3.6.1</surefire-plugin.version>
    <shade-plugin.version>3.6.2</shade-plugin.version>
    <enforcer-plugin.version>3.6.3</enforcer-plugin.version>
    <jacoco-plugin.version>0.8.16</jacoco-plugin.version>
    <spotless-plugin.version>3.10.4</spotless-plugin.version>
```

Versions of Maven's **plugin**s: the compiler, Surefire (runs tests), Shade (builds the single jar), Enforcer (rules about the build), JaCoCo (coverage) and Spotless (code layout).

> New words:
>
> - **plugin**: a program that Maven runs to do one kind of work (compile, test, package, format, check). Plugins are Maven's only way of doing anything; Maven itself only reads the pom and runs plugins in order.

```xml
    <!-- 1.37.0 fails inside Spotless 3.10.4 on Java 25 (InvocationTargetException); 1.36.1 works. -->
    <google-java-format.version>1.36.1</google-java-format.version>
    <checkstyle-plugin.version>3.6.0</checkstyle-plugin.version>
    <checkstyle.version>14.3.0</checkstyle.version>
  </properties>
```

The comment records a real finding of the author: google-java-format 1.37.0 crashes inside this Spotless version on Java 25, 1.36.1 works, so it is pinned. A comment like this is the place to write *why* a number is what it is. The same block also pins the Checkstyle plugin (3.6.0) and the Checkstyle engine it runs (14.3.0), which are separate downloads.

```xml
  <dependencies>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>${slf4j.version}</version>
    </dependency>
    <dependency>
```

The start of the list of **dependency** libraries. The first, `slf4j-api` (group `org.slf4j`), has no `<scope>`, so it has the default scope `compile`: the code is compiled against it and it is needed when the program runs. Its version is written `${slf4j.version}`, a reference to the property above.

> New words:
>
> - **dependency**: a library your code needs. You list its coordinates and Maven downloads it. The libraries *it* needs are fetched too (transitive dependencies).

```xml
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <version>${logback.version}</version>
      <scope>runtime</scope>
    </dependency>

    <dependency>
```

`logback-classic` has **scope** `runtime`: the code never mentions Logback by name (it only calls the SLF4J interface), so it is not needed to compile, only to run. Maven also downloads Logback's own needs (`logback-core`) as **transitive dependency** dependencies.

> New words:
>
> - **scope**: says when a dependency is needed: `compile` (the default: to compile and to run), `runtime` (only to run), `test` (only when compiling and running the tests). A `test` library is never put into the shipped jar.
> - **transitive dependency**: a library that one of your libraries needs. You do not list it, but Maven finds it in that library's own pom and downloads it too.

```xml
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>${junit.version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.assertj</groupId>
      <artifactId>assertj-core</artifactId>
      <version>${assertj.version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>com.tngtech.archunit</groupId>
      <artifactId>archunit-junit5</artifactId>
      <version>${archunit.version}</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
```

Three libraries with scope `test`, used only by the tests and never put into the shipped jar: `junit-jupiter` (the test framework, which includes the engine and the parameterized-test support), `assertj-core` (fluent assertions) and `archunit-junit5` (architecture rules that run as JUnit tests). The blank line between groups is for the human reader.

```xml
  <build>
    <finalName>spend</finalName>
```

The start of the `<build>` section, where plugins live. `<finalName>spend</finalName>` makes the jar `target/spend.jar` instead of `spend-0.1.0.jar`, so the Makefile and the Dockerfile can refer to one fixed file name.

```xml
    <pluginManagement>
      <plugins>
        <!-- Lifecycle plugins pinned so a Maven upgrade cannot change the build silently. -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-clean-plugin</artifactId>
          <version>3.5.0</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-resources-plugin</artifactId>
          <version>3.5.0</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-jar-plugin</artifactId>
          <version>3.5.1</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-install-plugin</artifactId>
          <version>3.2.0</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-deploy-plugin</artifactId>
          <version>3.2.0</version>
        </plugin>
```

**pluginManagement**: the first comment says its purpose (the six lifecycle plugins are pinned so that a Maven upgrade cannot silently change the build). Six `<plugin>` entries follow, each with the group `org.apache.maven.plugins`, an artifact and an exact version: `maven-clean-plugin` (deletes `target/`), `maven-resources-plugin` (copies resource files), `maven-jar-plugin` (creates the jar), `maven-install-plugin` and `maven-deploy-plugin` (copy the jar to a local or remote repository; not used here but part of the lifecycle) and `maven-site-plugin` (documentation site; also unused). They are listed only because the Enforcer rule `requirePluginVersions` demands an explicit version for every plugin Maven would use.

> New words:
>
> - **pluginManagement**: a section that only fixes versions and settings of plugins; it does not make any plugin run. Plugins that Maven uses on its own (clean, resources, jar, install, deploy, site) get their versions from it.

```xml
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-site-plugin</artifactId>
          <version>3.22.0</version>
        </plugin>
      </plugins>
    </pluginManagement>
    <plugins>
```

The last pinned plugin, `maven-site-plugin` 3.22.0, then the end of `<plugins>` and `</pluginManagement>`. Everything from here down is configuration that actually runs.

```xml
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
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

The **Enforcer** plugin. It has one **execution**, with the id `enforce-build-rules` and the single **goal** `enforce`. Inside `<configuration><rules>` each element is one rule: `requireJavaVersion` with the **version range** `[${java.release},)` (the running JDK must be 25 or newer), `requireMavenVersion` with `[3.9,)`, `banDuplicatePomDependencyVersions` (the same library may not be declared twice with different versions), `requireUpperBoundDeps` (when two libraries need different versions of a third, the highest must win) and `requirePluginVersions` (every plugin needs a pinned version). The self-closing elements like `<requireUpperBoundDeps/>` are rules without settings. They are explained in depth in `maven-enforcer.md`. No `<phase>` is given, so the goal uses its default, `validate`, the very first phase.

> New words:
>
> - **execution**: one configured run of a plugin goal inside a `<plugin>`: an optional `<id>` (its name), the `<phase>` it is bound to, its `<goals>` and its own `<configuration>`.
> - **goal**: one named job of a plugin, written `plugin:goal`, such as `spotless:check` or `jacoco:report`.
> - **version range**: `[25,)` means "25 or higher": a square bracket includes the end, a round bracket leaves it open, and nothing after the comma means no upper limit.

```xml
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>${compiler-plugin.version}</version>
        <configuration>
          <release>${java.release}</release>
          <showWarnings>true</showWarnings>
          <compilerArgs>
            <arg>-Xlint:all</arg>
            <arg>-Werror</arg>
          </compilerArgs>
        </configuration>
      </plugin>
```

The **compiler** plugin turns `.java` files into `.class` files. `<release>${java.release}</release>` compiles for exactly the Java 25 language and library, whatever newer JDK runs Maven. `<showWarnings>true</showWarnings>` prints the compiler's warnings. The two `<arg>` lines go to the Java compiler `javac`: `-Xlint:all` switches on every optional warning and `-Werror` turns each warning into an error, so a warning can never be committed.

```xml
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>${surefire-plugin.version}</version>
        <configuration>
          <argLine>@{argLine} -Dfile.encoding=UTF-8</argLine>
        </configuration>
      </plugin>
```

**Surefire** runs the tests in a separate JVM. Its `<argLine>` is the text placed on that JVM's command line. `@{argLine}` is a **@{...} placeholder**: JaCoCo sets `argLine` to the option that attaches its coverage agent, and Surefire inserts it here; `-Dfile.encoding=UTF-8` makes the test JVM read and write UTF-8 whatever the machine default is.

> New words:
>
> - **@{...} placeholder**: `${name}` is replaced when Maven reads the pom, before anything runs. `@{name}` is replaced later, by the plugin itself, just before it uses the value, so it can pick up a value that another plugin set meanwhile (JaCoCo sets `argLine` after the pom is read).

```xml
      <plugin>
        <groupId>org.jacoco</groupId>
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
```

The **JaCoCo** coverage plugin has three executions. `prepare-agent` (goal of the same name, default phase `initialize`) sets `argLine` so the tests are measured. `report` (goal `report`, phase `test`) writes the readable report after the tests. `check` (goal `check`, phase `verify`) fails the build if the rules below are not met.

```xml
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

The rules of the `check` run. Rule 1: `<element>PACKAGE</element>` applies to each package matching `<includes>`, here only `com.plainround.spend.domain`. Its two `<limit>`s demand that at least 95% of the *lines* (`<counter>LINE</counter>` with `<value>COVEREDRATIO</value>` and `<minimum>0.95</minimum>`) and 90% of the *branches* (the two ways of an `if`) be run by the tests. Rule 2: `<element>BUNDLE</element>` is the whole project: at least 85% of the lines. The domain, which holds the business rules, is therefore held to a stricter standard than the rest. `jacoco.md` explains the counters.

```xml
      <plugin>
        <groupId>com.diffplug.spotless</groupId>
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

**Spotless** with the formatter google-java-format. Inside `<java>`: `<googleJavaFormat>` with the pinned `<version>` and `<style>GOOGLE</style>` (2-space indentation, 100-column lines); `<removeUnusedImports/>` deletes imports nothing uses; `<importOrder/>` sorts imports in the standard order. There is no `<executions>` block, so Spotless does nothing during a normal build; the Makefile calls `spotless:check` and `spotless:apply` explicitly.

```xml
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-checkstyle-plugin</artifactId>
        <version>${checkstyle-plugin.version}</version>
        <dependencies>
          <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <version>${checkstyle.version}</version>
          </dependency>
        </dependencies>
        <configuration>
          <configLocation>config/checkstyle.xml</configLocation>
          <consoleOutput>true</consoleOutput>
          <failsOnError>true</failsOnError>
          <violationSeverity>warning</violationSeverity>
          <includeTestSourceDirectory>false</includeTestSourceDirectory>
        </configuration>
      </plugin>
```

The **Checkstyle** plugin. Its own `<dependencies>` block (inside the plugin, not the project's) chooses the Checkstyle engine version 14.3.0, which is separate from the plugin version. In `<configuration>`: `configLocation` is the rule file `config/checkstyle.xml`; `consoleOutput` prints violations to the terminal; `failsOnError` fails the build on errors; `violationSeverity` `warning` makes even warnings fail it; `includeTestSourceDirectory` false limits the checks to `src/main/java`. Like Spotless, it runs only when the Makefile calls `checkstyle:check`.

```xml
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>${shade-plugin.version}</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <createDependencyReducedPom>false</createDependencyReducedPom>
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>${main.class}</mainClass>
                  <manifestEntries>
                    <Multi-Release>true</Multi-Release>
                  </manifestEntries>
                </transformer>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
              </transformers>
              <filters>
                <filter>
                  <artifact>*:*</artifact>
                  <excludes>
                    <exclude>META-INF/*.SF</exclude>
                    <exclude>META-INF/*.DSA</exclude>
                    <exclude>META-INF/*.RSA</exclude>
                    <exclude>META-INF/MANIFEST.MF</exclude>
                    <exclude>module-info.class</exclude>
                    <exclude>META-INF/versions/*/module-info.class</exclude>
                  </excludes>
                </filter>
              </filters>
            </configuration>
          </execution>
        </executions>
      </plugin>
```

The **Shade** plugin builds the single runnable jar. Its one execution runs the goal `shade` in phase `package`. `createDependencyReducedPom` false stops it from writing an extra pom file. The `<transformers>` rewrite or merge certain files while copying: `ManifestResourceTransformer` writes the jar manifest with `Main-Class` set to `${main.class}` and the entry `Multi-Release: true`; `ServicesResourceTransformer` merges the `META-INF/services` files of all libraries. The `<filters>` entry for `*:*` (every library) leaves out signature files (`*.SF`, `*.DSA`, `*.RSA`), old manifests and `module-info.class` files, which would be wrong inside a merged jar. `maven-shade.md` explains each.

```xml
    </plugins>
  </build>
</project>
```

The closing tags: the end of `<plugins>`, of `<build>` and finally of `<project>`. Everything between them has been explained above.

## 6. Try it yourself

- Build and run the whole check: `./mvnw -B clean verify`. Expected: the lines `Rule 0` to `Rule 4 ... passed`, `Tests run: 57, Failures: 0, Errors: 0, Skipped: 0`, then `All coverage checks have been met.` and `BUILD SUCCESS`.
- Package only: `./mvnw -q -B clean package -DskipTests -Djacoco.skip=true`. Expected: no output, and the file `target/spend.jar` exists (`ls -l target/spend.jar`).
- See the order of plugins: `./mvnw -B clean verify | grep -- '--- '`. Expected: the twelve lines listed in the walkthrough above, from `clean:3.5.0:clean` to `jacoco:0.8.16:check`.
- See the dependency tree: `./mvnw dependency:tree`. Expected: a tree starting with `com.plainround:spend:jar:0.1.0` with `slf4j-api`, `logback-classic` (and its `logback-core`) and the test libraries below it.
- Print the effective settings: `./mvnw -q -B help:effective-pom -Doutput=target/effective-pom.xml && head -12 target/effective-pom.xml`. Expected: a file that begins with an XML declaration and the comment `Effective POM for project 'com.plainround:' + r.art + ':jar:0.1.0'`, followed by the pom with every default that Maven filled in.

## 7. Mistakes people make

- Forgetting `clean`: old files in `target/` can hide a problem (a deleted class still in `target/classes`). The Makefile uses `clean` in the targets that matter.
- Letting versions float. A missing version, or a range, makes the build change without a commit. Here every version is an exact number, plugins included, and the Enforcer plugin checks it.
- Putting a test-only library in the default **scope**, so it ends up inside the shipped jar. Here the test libraries have `<scope>test</scope>`.
- Running Maven with a Java older than the `release`. The error is `release version 25 not supported`; the Enforcer rule `requireJavaVersion` prints a clearer message first.
- Editing files in `target/`. The next build overwrites them and git ignores them.

> New words:
>
> - **scope**: says when a dependency is needed: `compile` (the default: to compile and to run), `runtime` (only to run), `test` (only when compiling and running the tests). A `test` library is never put into the shipped jar.

## 8. Check yourself

1. What is the difference between a phase and a goal?
   **Answer:** A phase is a step in the fixed lifecycle (`compile`, `test`, `package`); a goal is one task of a plugin (`compiler:compile`). Plugins attach goals to phases, and asking for a phase runs all earlier phases.
2. Which scope does `logback-classic` have here, and why?
   **Answer:** `runtime`. The code only calls the SLF4J interface, so it compiles without Logback; Logback must be present when the program runs.
3. What does `-Werror` do and where is it set?
   **Answer:** It makes any compiler warning an error. It is a `javac` argument set under `compilerArgs` in the compiler plugin configuration in `pom.xml`.
4. Why are plugin versions pinned in `pluginManagement`?
   **Answer:** So a new Maven release cannot silently change which plugin versions run; the Enforcer rule `requirePluginVersions` requires it.
5. Where do downloaded libraries go?
   **Answer:** Into `~/.m2/repository`, once; later builds reuse them.

## 9. Further reading

- Maven 3.9.x documentation: https://maven.apache.org/guides/ (the wrapper in this repository downloads 3.9.16)
- Build lifecycle: https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html
- POM reference: https://maven.apache.org/pom.html
- Compiler plugin 3.16.0: https://maven.apache.org/plugins/maven-compiler-plugin/ ; Surefire 3.6.1: https://maven.apache.org/surefire/maven-surefire-plugin/
