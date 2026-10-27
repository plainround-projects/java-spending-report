# Maven (3.9.16, through the wrapper)

## 1. What it is

Maven is a **build tool**: a command-line program that turns source code into something you can run, and runs the checks on the way. You describe the project once in a file named `pom.xml` (Project Object Model). Maven then follows a fixed **lifecycle**, a list of **phases** in order: `validate`, `compile`, `test`, `package`, `verify`, `install`, `deploy`. Asking for a phase runs every earlier phase too. A **plugin** is a program that Maven runs for a **goal** (a named task); each phase has plugin goals attached to it.

## 2. Why this repository uses it

The lesson asks for a Java project that others can build with one command. Maven is the standard way: the same `pom.xml` gives the same jar, the same tests and the same quality checks on any computer with a JDK, and the checked-in wrapper means nobody has to install Maven first.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `pom.xml` | the whole build: coordinates, libraries, plugins and their settings |
| `mvnw`, `mvnw.cmd`, `.mvn/` | the Maven wrapper (see `maven-wrapper.md`) |
| `Makefile` | every target calls `./mvnw` with a few options |
| `src/main/java`, `src/test/java`, `src/main/resources` | the standard folders Maven expects, so the pom does not have to name them |
| `target/` | build output (classes, reports, the jar); never committed |

## 4. How it works underneath

- Maven is a Java program. `./mvnw` starts it in a JVM. Maven reads `pom.xml` and builds a plan: which plugin goals run in which order.
- Folders by convention: Java sources are in `src/main/java`, tests in `src/test/java`, resources (files copied unchanged onto the class path, such as `logback.xml`) in `src/main/resources`. All output goes to `target/`.
- The phases used here: `compile` writes `target/classes`; `test-compile` compiles the tests; `test` runs them (Surefire); `package` writes the jar and, because of the Shade plugin, replaces it with the fat jar; `verify` runs the checks that need a finished jar or finished test results (the JaCoCo coverage rule). `clean` is a separate lifecycle that deletes `target/`.
- Coordinates: `groupId:artifactId:version` (GAV) names every library and plugin. Maven Central is the public warehouse of such files. The first build downloads what is missing into `~/.m2/repository`; later builds reuse it.
- Options used in this repository: `-q` (quiet, only errors), `-B` (batch mode: no colors or progress bars, good for logs and CI), `-D` name=value (set a property, for example `-DskipTests` or `-Djacoco.skip=true`), `-Dtest=ClassName` (run one test class).

## 5. Walkthrough of this repository's use

A `pom.xml` is one XML file. Read it from the top.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
```

`<project>` is the root element; everything else is inside it. The `xmlns` lines name the XML vocabulary (a namespace, a fixed web address that identifies the Maven file format). They are ceremony: copy them, do not change them. `<modelVersion>4.0.0</modelVersion>` says this file uses version 4.0.0 of the POM format; Maven 3.9 only understands this one.

```xml
  <groupId>com.plainround</groupId>
  <artifactId>spend</artifactId>
  <version>0.1.0</version>
  <packaging>jar</packaging>
  <name>spend</name>
  <description>Command-line spending report: exact money, bad rows reported instead of crashing.</description>
```

These lines are the project's **coordinates**, its address in the Maven world. `groupId` says who made it (`com.plainround` is a reversed web name, the usual style). `artifactId` is the project's own name. `version` is `0.1.0`. `packaging` `jar` means the build produces a `.jar` file (a zip of compiled classes with a manifest). `name` and `description` are for humans.

```xml
  <properties>
    <java.release>25</java.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <main.class>com.plainround.spend.interfaces.cli.SpendCli</main.class>

    <slf4j.version>2.0.20</slf4j.version>
    <logback.version>1.6.6</logback.version>
    <junit.version>6.1.3</junit.version>
    <assertj.version>3.27.7</assertj.version>
    <archunit.version>1.5.1</archunit.version>

    <compiler-plugin.version>3.16.0</compiler-plugin.version>
    <surefire-plugin.version>3.6.1</surefire-plugin.version>
    <shade-plugin.version>3.6.2</shade-plugin.version>
    <enforcer-plugin.version>3.6.3</enforcer-plugin.version>
    <jacoco-plugin.version>0.8.16</jacoco-plugin.version>
    <spotless-plugin.version>3.10.4</spotless-plugin.version>
    <!-- 1.37.0 fails inside Spotless 3.10.4 on Java 25 (InvocationTargetException); 1.36.1 works. -->
    <google-java-format.version>1.36.1</google-java-format.version>
    <checkstyle-plugin.version>3.6.0</checkstyle-plugin.version>
    <checkstyle.version>14.3.0</checkstyle.version>
  </properties>
```

`<properties>` are named values you can reuse as `${name}`. `java.release` is the Java version the code must compile for: 25. `project.build.sourceEncoding` tells the compiler that source files are UTF-8; without it Maven uses the machine's default, and an accented letter could compile differently on another computer. `main.class` is this repository's own property: the class whose `main` method starts the program (the shade plugin reads it later). The `*.version` properties pin every library and plugin to one exact version, in one place, so a version appears once and is easy to change. The comment above `google-java-format.version` records a real finding: a newer version failed inside the pinned Spotless on Java 25.

```xml
  <dependencies>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>${slf4j.version}</version>
    </dependency>
    <dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <version>${logback.version}</version>
      <scope>runtime</scope>
    </dependency>

    <dependency>
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

`<dependencies>` lists the libraries the code needs. Each has coordinates plus a **scope**. No scope means `compile`: needed to compile and to run (`slf4j-api`, the logging interface the code calls). `runtime` means not needed to compile, only to run (`logback-classic`, the logging engine that the code never names). `test` means only the tests use it (JUnit, AssertJ, ArchUnit). Maven downloads each one, and the libraries those need (**transitive dependencies**), from Maven Central into the folder `~/.m2/repository` on your machine, once.

```xml
    <finalName>spend</finalName>
```

`finalName` makes the jar `target/spend.jar` instead of `spend-0.1.0.jar`, so commands in the Makefile do not need the version.

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
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-site-plugin</artifactId>
          <version>3.22.0</version>
        </plugin>
      </plugins>
    </pluginManagement>
```

`pluginManagement` does not run anything. It only fixes the version of plugins that Maven uses on its own for the standard lifecycle (clean, resources, jar, install, deploy, site). Without it, Maven picks a default version that depends on the Maven release, and the Enforcer rule `requirePluginVersions` (see `maven-enforcer.md`) would refuse the build.

```xml
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

The compiler plugin turns `.java` files into `.class` files. `<release>25</release>` compiles against the Java 25 language and library rules, whatever JDK runs Maven. `showWarnings` prints warnings. The two `compilerArgs` are handed to `javac`: `-Xlint:all` switches on every optional warning (unchecked casts, missing `serialVersionUID`, and more) and `-Werror` turns every warning into an error. The effect: no warning can be committed.

```xml
        <artifactId>maven-surefire-plugin</artifactId>
        <version>${surefire-plugin.version}</version>
        <configuration>
          <argLine>@{argLine} -Dfile.encoding=UTF-8</argLine>
        </configuration>
      </plugin>
```

Surefire runs the tests in a separate JVM process. `@{argLine}` is a late-bound placeholder that JaCoCo fills with the `-javaagent:...` option that measures coverage (see `jacoco.md`); `-Dfile.encoding=UTF-8` makes the test JVM read and write UTF-8 whatever the machine default is.

The other plugins have their own files: Enforcer, JaCoCo, Spotless, Checkstyle and Shade.

## 6. Try it yourself

- Build and run the whole check: `./mvnw -B clean verify`. Expected: the lines `Tests run: 57, Failures: 0, Errors: 0, Skipped: 0`, then `All coverage checks have been met.` and `BUILD SUCCESS`.
- Package only: `./mvnw -q -B clean package -DskipTests -Djacoco.skip=true`. Expected: no output, and the file `target/spend.jar` exists (`ls -l target/spend.jar`).
- Run one test class: `./mvnw -q -B test -Dtest=ArchitectureTest -Djacoco.skip=true`. Expected: no output on success; add `-B` without `-q` to see the `Tests run:` line.
- See the dependency tree: `./mvnw dependency:tree`. Expected: a tree starting with `com.plainround:spend:jar:0.1.0` with `slf4j-api`, `logback-classic` (and its `logback-core`) and the test libraries below it.
- Print the effective settings: `./mvnw help:effective-pom -q | head -40`. Expected: the pom with every default Maven filled in.

## 7. Mistakes people make

- Forgetting `clean`: old files in `target/` can hide a problem (a deleted class still in `target/classes`). The Makefile uses `clean` in the targets that matter.
- Letting versions float. A missing version, or a range, makes the build change without a commit. Here every version is an exact number, plugins included, and the Enforcer plugin checks it.
- Putting a test-only library in the default scope, so it ends up inside the shipped jar. Here the test libraries have `<scope>test</scope>`.
- Running Maven with a Java older than the `release`. The error is `release version 25 not supported`; the Enforcer rule `requireJavaVersion` prints a clearer message first.
- Editing `target/`. It is overwritten by the next build and ignored by git.

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
