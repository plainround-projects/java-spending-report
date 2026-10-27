# Checkstyle (engine 14.3.0, Maven plugin 3.6.0)

## 1. What it is

A **linter** is a program that reads source code and reports problems that are not about layout: names that are too short, public things without documentation, risky patterns, leftover notes to self. **Checkstyle** is a linter for Java. You give it a rule file (XML); it checks every Java file against the rules and reports each violation with the file and line.

## 2. Why this repository uses it

The repository rule asks for a linter run in CI. Checkstyle enforces the house style that neither the compiler nor the formatter does: names of at least two characters, Javadoc on the public surface, no `TODO`, no empty `catch`.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `config/checkstyle.xml` | the rule file (103 lines, walked through below) |
| `pom.xml` | the Maven plugin and the engine version |
| `Makefile` | `lint` runs `checkstyle:check` |
| `src/main/java/**` | the code that is checked |

## 4. How it works underneath

1. Checkstyle parses each `.java` file into a **syntax tree**, a structure of the code (classes, methods, statements). A **check** is a small program that visits tree nodes of one kind and reports a violation.
2. `TreeWalker` is the module that does the tree visiting for all the checks nested in it. Modules directly under `Checker` read the raw text of the file instead.
3. Each `<module name="...">` is one check; its `<property>` children tune it.
4. The Maven goal `checkstyle:check` fails the build if any violation has at least the configured severity.

## 5. Walkthrough of this repository's use

### The plugin entry in `pom.xml`

```xml
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-checkstyle-plugin</artifactId>
        <version>${checkstyle-plugin.version}</version>
```

The plugin entry: group `org.apache.maven.plugins`, artifact `maven-checkstyle-plugin`, version 3.6.0 from the property. There is no `<executions>` block: it runs only when the Makefile calls `checkstyle:check`.

```xml
        <dependencies>
          <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <version>${checkstyle.version}</version>
          </dependency>
        </dependencies>
```

Dependencies of the *plugin*, not of the project. Checkstyle's rule engine is a separate library; this block chooses its version, 14.3.0, from the property `checkstyle.version`, independent of the plugin's own version.

```xml
        <configuration>
          <configLocation>config/checkstyle.xml</configLocation>
          <consoleOutput>true</consoleOutput>
          <failsOnError>true</failsOnError>
          <violationSeverity>warning</violationSeverity>
          <includeTestSourceDirectory>false</includeTestSourceDirectory>
        </configuration>
      </plugin>
```

The settings: `configLocation` is the rule file (`config/checkstyle.xml`, explained below); `consoleOutput` `true` prints each violation in the terminal; `failsOnError` `true` fails the build on an error; `violationSeverity` `warning` means a violation of level warning (or worse) fails the build, so nothing is ever "just a warning"; `includeTestSourceDirectory` `false` checks only `src/main/java`, because Javadoc on every test method would be noise.

### `config/checkstyle.xml`, line by line

```xml
<?xml version="1.0"?>
```

The first line is the XML declaration (XML version 1.0). Checkstyle's rule file is an XML document made of nested `<module>` elements: each module is one check or one container of checks.

```xml
<!DOCTYPE module PUBLIC "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">
```

The **document type declaration**. It names the formal grammar (a DTD, *document type definition*) that this file follows: `Checkstyle Configuration 1.3`, with the web address of the grammar file. Checkstyle uses it to recognize the file's format version; the tool has this DTD built in, so nothing is downloaded when it runs.

```xml
<!-- Rules that a formatter cannot enforce: naming, Javadoc on the public surface, dead code and
     leftovers. Layout is owned by Spotless (google-java-format). -->
```

An **XML comment** stating the file's purpose: these are the rules that a formatter cannot enforce (naming, documentation, leftovers); layout belongs to Spotless.

> New words:
>
> - **XML comment**: text between `<!--` and `-->`. Tools ignore it; it is there for the human reader.

```xml
<module name="Checker">
  <property name="charset" value="UTF-8"/>
  <property name="severity" value="warning"/>
  <property name="fileExtensions" value="java"/>
```

`Checker` is the root module. It runs on every file. Its three `<property>` children set defaults: `charset` is the text encoding of the files (UTF-8); `severity` `warning` is the level reported for every violation (the Maven plugin is set to fail the build on warnings, see `pom.xml`); `fileExtensions` `java` limits the check to files ending in `.java`.

```xml
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
```

Four checks that read the raw text of each file, line by line (they sit directly under `Checker`). `NewlineAtEndOfFile` demands that the file ends with a line break. `FileTabCharacter` forbids the tab character (indentation is spaces). The first `RegexpSingleline` flags any line that matches the **regular expression** `\s+$`: one or more white-space characters right before the end of the line, that is, trailing spaces; its `message` is the text shown. The second `RegexpSingleline` flags the section-sign character, written as the escape `\u00A7` so that this file itself does not contain the symbol; the message explains the house rule to write the word `Section` instead.

> New words:
>
> - **regular expression**: a short pattern language for text shapes: `\d` is a digit, `+` means one or more, `?` optional, `[+-]` one of the two signs, `(...)` a group, `.` any character. A `Pattern` is a compiled one and `matcher(text).matches()` asks whether the whole text fits.

```xml
  <module name="TreeWalker">
```

`TreeWalker` first parses each file into a **syntax tree** (a structure of classes, methods, statements and expressions) and then lets the checks nested inside visit the tree. All the checks below are children of it.

```xml
    <module name="OuterTypeFilename"/>
    <module name="AvoidStarImport"/>
    <module name="UnusedImports"/>
    <module name="RedundantImport"/>
    <module name="IllegalImport"/>
```

Names and imports. `OuterTypeFilename`: the file name must equal the name of the top-level type in it. `AvoidStarImport`: no `import java.util.*;` (always import single classes). `UnusedImports`: no import that nothing uses. `RedundantImport`: no import twice, none from the same package, none from `java.lang`. `IllegalImport`: no import from packages that are not meant to be used (by default `sun.*` internals).

```xml
    <module name="PackageName">
      <property name="format" value="^[a-z]+(\.[a-z][a-z0-9]*)*$"/>
    </module>
```

`PackageName` checks every package name against the given regular expression: `^[a-z]+(\.[a-z][a-z0-9]*)*$` means lowercase words separated by dots, each part starting with a letter.

```xml
    <module name="TypeName"/>
    <module name="MethodName"/>
    <module name="MemberName"/>
    <module name="ConstantName"/>
    <module name="StaticVariableName"/>
```

Five naming checks with Checkstyle's built-in patterns: `TypeName` (classes, interfaces, records and enums are `UpperCamelCase`), `MethodName` (`lowerCamelCase`), `MemberName` (instance fields `lowerCamelCase`), `ConstantName` (`static final` fields are `UPPER_SNAKE_CASE`) and `StaticVariableName` (non-final static fields `lowerCamelCase`).

```xml
    <module name="ParameterName">
      <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
    </module>
```

Variable-name checks with a custom pattern. `^[a-z][a-zA-Z0-9]+$` means a lowercase letter followed by **one or more** letters or digits, so a name needs at least two characters: this is how the house rule "no one-letter names" is enforced. `ParameterName` applies it to method parameters.

```xml
    <module name="LocalVariableName">
      <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
      <property name="allowOneCharVarInForLoop" value="false"/>
    </module>
```

The same for local variables. `allowOneCharVarInForLoop` `false` removes the usual exception that allows `i` in a `for` loop.

```xml
    <module name="LambdaParameterName">
      <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
    </module>
```

The same pattern for lambda parameters (`link -> ...`).

```xml
    <module name="CatchParameterName">
      <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
    </module>
```

The same pattern for the variable of a `catch` block (`catch (IOException problem)`).

```xml
    <module name="RecordComponentName">
      <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
    </module>
```

The same pattern for the components of a record.

```xml
    <module name="ModifierOrder"/>
    <module name="RedundantModifier"/>
```

Code-quality checks begin. `ModifierOrder`: modifiers appear in the order the Java language recommends (`public static final`, not `final static public`). `RedundantModifier`: no modifier that has no effect (for example `public` on an interface method).

```xml
    <module name="NeedBraces"/>
```

`NeedBraces`: every `if`, `else`, `for`, `while` body must have curly braces, even for a single statement.

```xml
    <module name="EmptyBlock">
      <property name="option" value="text"/>
    </module>
```

`EmptyBlock` with `option` `text`: a block (for example an `else` or a `finally`) may be empty only if it contains some text, i.e. a comment. By default it does not look at `catch` blocks, which have their own check.

```xml
    <module name="EmptyCatchBlock">
      <property name="exceptionVariableName" value="expected|ignored"/>
    </module>
```

`EmptyCatchBlock` allows an empty `catch` block only if the exception variable's name matches `expected|ignored` (a regular expression with two alternatives), or if the block contains a comment saying why. Silent swallowing of errors is the thing it prevents.

```xml
    <module name="OneStatementPerLine"/>
    <module name="MultipleVariableDeclarations"/>
    <module name="ArrayTypeStyle"/>
    <module name="UpperEll"/>
    <module name="MissingOverride"/>
```

Statement hygiene. `OneStatementPerLine`: one statement per line. `MultipleVariableDeclarations`: `int a, b;` is not allowed, one variable per declaration. `ArrayTypeStyle`: write `String[] args`, not `String args[]`. `UpperEll`: a long literal ends in capital `L` (`500L`), because a lowercase `l` looks like the digit 1. `MissingOverride`: a method that overrides another must carry `@Override`.

```xml
    <module name="InnerAssignment"/>
    <module name="SimplifyBooleanExpression"/>
    <module name="SimplifyBooleanReturn"/>
    <module name="EqualsHashCode"/>
    <module name="FallThrough"/>
```

Classic mistakes. `InnerAssignment`: no assignment inside an expression (`while ((x = next()) != null)` style). `SimplifyBooleanExpression`: no `x == true` or `!(!y)`. `SimplifyBooleanReturn`: do not write `if (c) return true; else return false;`, return `c`. `EqualsHashCode`: a class that overrides `equals` must override `hashCode`, and vice versa (this is the rule behind the lab's first planted bug). `FallThrough`: a `switch` case must not run on into the next without a comment.

```xml
    <module name="FinalClass"/>
    <module name="HideUtilityClassConstructor"/>
    <module name="StringLiteralEquality"/>
```

Design checks. `FinalClass`: a class whose constructors are all private must be `final`. `HideUtilityClassConstructor`: a class with only static methods must have a private constructor, so nobody creates an object of it. `StringLiteralEquality`: no `==` or `!=` with a string literal (use `equals`).

```xml
    <module name="MethodLength">
      <property name="max" value="25"/>
      <property name="countEmpty" value="false"/>
    </module>
```

`MethodLength` `max` 25: a method may have at most 25 lines; `countEmpty` `false` means blank lines and comment-only lines are not counted.

```xml
    <module name="ParameterNumber">
      <property name="max" value="7"/>
      <property name="ignoreOverriddenMethods" value="true"/>
    </module>
```

`ParameterNumber` `max` 7: at most seven parameters; `ignoreOverriddenMethods` `true` exempts methods that override a parent's method, whose signature is not the author's choice.

```xml
    <module name="TodoComment">
      <property name="format" value="TODO|FIXME|XXX"/>
    </module>
```

`TodoComment`: any comment that matches the regular expression `TODO|FIXME|XXX` is a violation, so unfinished work cannot be committed.

```xml
    <module name="MissingJavadocType">
      <property name="scope" value="public"/>
    </module>
```

Documentation checks. `MissingJavadocType` with `scope` `public`: every public class, interface, enum and record needs a Javadoc comment (`/** ... */`).

```xml
    <module name="MissingJavadocMethod">
      <property name="scope" value="public"/>
      <property name="allowMissingPropertyJavadoc" value="true"/>
      <property name="allowedAnnotations" value="Override"/>
    </module>
```

`MissingJavadocMethod`: every public method needs a Javadoc comment, except simple getters and setters (`allowMissingPropertyJavadoc` `true`) and methods marked `@Override` (`allowedAnnotations`).

```xml
    <module name="JavadocMethod">
      <property name="accessModifiers" value="public"/>
      <property name="allowMissingParamTags" value="true"/>
      <property name="allowMissingReturnTag" value="true"/>
      <property name="validateThrows" value="false"/>
    </module>
```

`JavadocMethod` checks the *contents* of a method's Javadoc comment, for public methods: `allowMissingParamTags` and `allowMissingReturnTag` `true` mean the `@param` and `@return` tags need not be present (but if present they must be right), and `validateThrows` `false` skips the check of `@throws` tags.

```xml
    <module name="SummaryJavadoc"/>
  </module>
</module>
```

`SummaryJavadoc`: the first sentence of a Javadoc comment must be a real summary (it must exist and end with a period). The two closing tags end `TreeWalker` and `Checker`.

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
