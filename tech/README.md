# Technology notes for java-spending-report

These notes teach every technology this repository uses, from zero, through the repository's own files. Each note has the same nine parts: what it is, why this repository uses it, where it appears, how it works underneath, a walkthrough of this repository's own use with the real lines quoted and explained, commands to try, common mistakes, five questions to check yourself, and links to the official documentation for the pinned version. You need no earlier knowledge.

There are 27 notes. If you have only ten minutes, read these three in order:

1. [`java.md`](java.md): the language: every source file in src/main/java and src/test/java.
1. [`csv.md`](csv.md): the input format of the spending report.
1. [`junit.md`](junit.md): the test framework: finds and runs the 57 tests.

Java is the language of the whole program, CSV is the input that makes the lesson hard, JUnit is how the lesson's Done-when lines are proved.

## Reading order

Read the language first, then the build, then the runtime pieces, then the tests, then the commands and the files around the code. Inside a group the order below is the order to read in.

### The language

- [`java.md`](java.md): the language: every source file in src/main/java and src/test/java.

### The build

- [`maven.md`](maven.md): the build tool: compiles, tests, packages and runs every check.
- [`maven-wrapper.md`](maven-wrapper.md): mvnw and .mvn: the build runs the pinned Maven without installing it.
- [`maven-enforcer.md`](maven-enforcer.md): rules about the build itself: Java and Maven versions, pinned plugins, no version conflicts.
- [`maven-shade.md`](maven-shade.md): builds the single runnable jar with all libraries inside.
- [`spotless-google-java-format.md`](spotless-google-java-format.md): one fixed code layout, checked in make lint and CI.
- [`checkstyle.md`](checkstyle.md): the lint rules a formatter cannot enforce: naming, Javadoc, leftovers.
- [`jacoco.md`](jacoco.md): measures which lines the tests run and fails the build below the set coverage.

### The runtime pieces

- [`slf4j.md`](slf4j.md): the logging interface the code calls.
- [`logback.md`](logback.md): the logging engine: prints key=value lines to standard error.
- [`csv.md`](csv.md): the input format of the spending report.

### The tests

- [`junit.md`](junit.md): the test framework: finds and runs the 57 tests.
- [`assertj.md`](assertj.md): readable assertions in the tests.
- [`archunit.md`](archunit.md): tests that fail if a package depends on one it must not.

### The commands and the packaging

- [`bash.md`](bash.md): the scripts under scripts/ and the mvnw wrapper.
- [`unix-text-tools.md`](unix-text-tools.md): the small programs that the Makefile and scripts chain together.
- [`make.md`](make.md): the Makefile: one short command per job.
- [`dockerfile.md`](dockerfile.md): the recipe for the container image.
- [`docker.md`](docker.md): containers for the image build, the dependency scan.
- [`github-actions.md`](github-actions.md): runs format, lint, tests and the dependency audit on every push.
- [`osv-scanner.md`](osv-scanner.md): the dependency audit: known-vulnerability scan of the pinned libraries.

### The files around the code

- [`git.md`](git.md): version control: what is tracked, and how the history is written.
- [`editorconfig.md`](editorconfig.md): tells editors the encoding, line endings and indentation.
- [`env-file.md`](env-file.md): documents every setting and its default; read by Make and Docker Compose.
- [`xml.md`](xml.md): the format of pom.xml, config/checkstyle.xml and logback.xml.
- [`yaml.md`](yaml.md): the format of the GitHub Actions workflow.
- [`markdown-and-mermaid.md`](markdown-and-mermaid.md): the format of README.md, ARCHITECTURE.md, the decision records and the diagram.

## How to use the notes

- Open the repository next to the note and follow the file paths in the "Where it appears" table.
- Run the commands in "Try it yourself" and compare with the expected output.
- Answer the five questions at the end of each note before you read the answers.
- The notes describe the code as committed. If the code changes, the notes change with it.
