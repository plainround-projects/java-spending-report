# Checkstyle (engine 14.3.0, Maven plugin 3.6.0)

## 1. What it is

A **linter** is a program that reads source code and reports problems that are not formatting: bad names, missing documentation, risky patterns. **Checkstyle** is a linter for Java. You give it a rule file (XML); it checks every file against the rules and reports each violation with file and line.

## 2. Why this repository uses it

The repository rule asks for a linter run in CI. Checkstyle enforces the house style that no compiler or formatter does: names of at least two characters, Javadoc on the public surface, no `TODO`, no empty `catch`.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `config/checkstyle.xml` | the rule file (about 100 lines) |
| `pom.xml` | the Maven plugin and the engine version |
| `Makefile` | `lint` runs `checkstyle:check` |
| `src/main/java/**` | the code that is checked |

## 4. How it works underneath

- Checkstyle parses each `.java` file into a **syntax tree**, a structure of the code (classes, methods, statements). A **check** is a small program that visits tree nodes of one kind and reports a violation.
- `TreeWalker` is the module that does the tree visiting for all the checks nested in it. Modules directly under `Checker` read raw text instead.
- Each `<module name="...">` is one check; its `<property>` children tune it.
- The Maven goal `checkstyle:check` fails the build if any violation has at least the configured severity.

## 5. Walkthrough of this repository's use

```xml
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

- The Maven plugin is 3.6.0; the Checkstyle engine it uses is a separate download, version 14.3.0, named in the plugin's own `<dependencies>`.
- `configLocation` points at `config/checkstyle.xml`, the rule file below.
- `consoleOutput` prints each violation. `failsOnError` makes errors fail the build. `violationSeverity` `warning` means even warnings fail it, so nothing is ever "just a warning".
- `includeTestSourceDirectory` false: the rules apply to `src/main/java` only; tests are formatted by Spotless but not checked for Javadoc.

```xml
<module name="Checker">
  <property name="charset" value="UTF-8"/>
  <property name="severity" value="warning"/>
  <property name="fileExtensions" value="java"/>

  <module name="NewlineAtEndOfFile"/>
  <module name="FileTabCharacter"/>
  <module name="RegexpSingleline">
    <property name="format" value="\s+$"/>
    <property name="message" value="Trailing whitespace"/>
  </module>
  <module name="RegexpSingleline">
    <property name="format" value="\u00A7"/>
    <property name="message" value="Write 'Section N', never the section sign"/>
  </module>

  <module name="TreeWalker">
```

`Checker` is the root. `severity` `warning` is the level of every finding. `fileExtensions` `java` limits it to Java files. The checks directly under `Checker` look at whole files or lines of text: a newline at the end of the file, no tab characters, no trailing spaces (a regular expression `\s+$`), and one that forbids the section-sign symbol in source (its regular expression is written as the escape `\u00A7` so the file itself does not contain the symbol).

```xml
    <module name="OuterTypeFilename"/>
    <module name="AvoidStarImport"/>
    <module name="UnusedImports"/>
    <module name="RedundantImport"/>
    <module name="IllegalImport"/>
    <module name="PackageName">
      <property name="format" value="^[a-z]+(\.[a-z][a-z0-9]*)*$"/>
    </module>
    <module name="TypeName"/>
    <module name="MethodName"/>
    <module name="MemberName"/>
    <module name="ConstantName"/>
    <module name="StaticVariableName"/>
    <module name="ParameterName">
      <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
    </module>
    <module name="LocalVariableName">
      <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
      <property name="allowOneCharVarInForLoop" value="false"/>
    </module>
    <module name="LambdaParameterName">
      <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
    </module>
    <module name="CatchParameterName">
      <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
    </module>
    <module name="RecordComponentName">
      <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
    </module>
```

`TreeWalker` parses each file into a syntax tree and visits it. The first group is about imports and names. `AvoidStarImport`, `UnusedImports`, `RedundantImport`, `IllegalImport` keep imports tidy. The naming checks (`TypeName`, `MethodName`, `MemberName`, `ConstantName`, ...) use Checkstyle's default patterns, except for variables: `ParameterName`, `LocalVariableName`, `LambdaParameterName`, `CatchParameterName` and `RecordComponentName` use `^[a-z][a-zA-Z0-9]+$`, which needs at least **two** characters. That is how the house rule "no single-letter variables" is enforced.

```xml
    <module name="ModifierOrder"/>
    <module name="RedundantModifier"/>
    <module name="NeedBraces"/>
    <module name="EmptyBlock">
      <property name="option" value="text"/>
    </module>
    <module name="EmptyCatchBlock">
      <property name="exceptionVariableName" value="expected|ignored"/>
    </module>
    <module name="OneStatementPerLine"/>
    <module name="MultipleVariableDeclarations"/>
    <module name="ArrayTypeStyle"/>
    <module name="UpperEll"/>
    <module name="MissingOverride"/>
    <module name="InnerAssignment"/>
    <module name="SimplifyBooleanExpression"/>
    <module name="SimplifyBooleanReturn"/>
    <module name="EqualsHashCode"/>
    <module name="FallThrough"/>
    <module name="FinalClass"/>
    <module name="HideUtilityClassConstructor"/>
    <module name="StringLiteralEquality"/>
    <module name="MethodLength">
      <property name="max" value="25"/>
      <property name="countEmpty" value="false"/>
    </module>
    <module name="ParameterNumber">
      <property name="max" value="7"/>
      <property name="ignoreOverriddenMethods" value="true"/>
    </module>
    <module name="TodoComment">
      <property name="format" value="TODO|FIXME|XXX"/>
    </module>
```

The second group is code quality. `NeedBraces` requires braces on every `if`. `EmptyBlock` and `EmptyCatchBlock` forbid empty blocks unless they hold a comment (or the exception variable is named `expected` or `ignored`). `MissingOverride`, `FinalClass`, `HideUtilityClassConstructor`, `StringLiteralEquality` (no `==` on strings), `EqualsHashCode` (override both or neither), `InnerAssignment` and `SimplifyBooleanExpression` catch classic mistakes. `MethodLength` allows 25 lines per method (blank lines not counted) and `ParameterNumber` 7 parameters. `TodoComment` fails on `TODO`, `FIXME` or `XXX`, which keeps unfinished work out.

```xml
    <module name="MissingJavadocType">
      <property name="scope" value="public"/>
    </module>
    <module name="MissingJavadocMethod">
      <property name="scope" value="public"/>
      <property name="allowMissingPropertyJavadoc" value="true"/>
      <property name="allowedAnnotations" value="Override"/>
    </module>
    <module name="JavadocMethod">
      <property name="accessModifiers" value="public"/>
      <property name="allowMissingParamTags" value="true"/>
      <property name="allowMissingReturnTag" value="true"/>
      <property name="validateThrows" value="false"/>
    </module>
    <module name="SummaryJavadoc"/>
```

The last group is documentation. Every `public` type and method needs a Javadoc comment (`/** ... */`), except methods marked `@Override` and simple getters. `JavadocMethod` checks the comment's tags only when present. `SummaryJavadoc` requires the first sentence of a comment to be a real summary.

## 6. Try it yourself

- `./mvnw -q checkstyle:check`. Expected: no output (quiet) or, without `-q`, `You have 0 Checkstyle violations.`
- In a scratch copy, rename a local variable to `x` and run it again. Expected: `Name 'x' must match pattern '^[a-z][a-zA-Z0-9]+$'`.
- Delete the Javadoc of a public method and run it again. Expected: `Missing a Javadoc comment`.
- `make lint`. Expected: Spotless, Checkstyle and compilation with `-Xlint:all -Werror` all pass.

## 7. Mistakes people make

- Using `JavadocStyle`: Checkstyle 14 removed it; `SummaryJavadoc` replaces it.
- Treating warnings as optional. Here `violationSeverity` is `warning`, so they fail the build.
- Writing a rule file and not running it in CI. It is run by `make lint`, which CI runs.
- Checking tests with the main rules: Javadoc on every test method would be noise, so tests are excluded.

## 8. Check yourself

1. What is a syntax tree?
   **Answer:** A structured view of the code (classes, methods, statements) that checks walk through.
2. How does this repository forbid one-letter variable names?
   **Answer:** The variable-name checks use the pattern `^[a-z][a-zA-Z0-9]+$`, which needs at least two characters.
3. What does `TodoComment` do?
   **Answer:** It fails the build when a comment contains `TODO`, `FIXME` or `XXX`.
4. Which source folder is checked?
   **Answer:** Only `src/main/java`; `includeTestSourceDirectory` is false.
5. Why is `violationSeverity` set to `warning`?
   **Answer:** So warnings also fail the build.

## 9. Further reading

- Checkstyle 14.3.0 checks list: https://checkstyle.org/checks.html
- Maven Checkstyle Plugin 3.6.0: https://maven.apache.org/plugins/maven-checkstyle-plugin/
- Configuration: https://checkstyle.org/config.html
