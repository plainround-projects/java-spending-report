# java-spending-report

`spend`, a command-line tool that reads a messy CSV export of card transactions and prints a monthly report: total spent per category per month, the five largest purchases, and every row it could not read with its line number and why. Money stays exact in `BigDecimal`, a bad row is data and not a crash, and a file of any size streams through a small heap.

| | |
| --- | --- |
| **Lesson** | `/prep/java/project-spending-report`, "Spending report" (`data-structure-explainer/src/content/tracks/java/22-project-spending-report.md` in the Plainround app) |
| **Planned hours** | 6 |
| **Level** | Beginner |
| **Java** | 25 (the current long-term-support release; the lesson names no version) |
| **Architecture** | [ARCHITECTURE.md](ARCHITECTURE.md), decisions in [docs/adr](docs/adr) |

## Prerequisites

| Tool | Version | Used for |
| --- | --- | --- |
| JDK | 25 (tested with Temurin 25.0.2) | compiling and running |
| GNU Make | 3.81 or newer (the one on macOS works) | every command below |
| Docker | 24 or newer, only for `make audit`, `make docker-build` and `make docker-run` | the dependency scan and the container image |
| Maven | none to install: the checked-in wrapper `./mvnw` downloads Maven 3.9.16 on first use | the build |

No cloud account, no database, no secrets. Libraries are pinned by exact version in `pom.xml` (Maven has no lock file; the Enforcer plugin bans duplicate versions and requires upper-bound convergence): SLF4J 2.0.20 and Logback 1.6.6 at run time; JUnit 6.1.3, AssertJ 3.27.7 and ArchUnit 1.5.1 for tests.

## Setup, run, test, stop

```bash
make up          # build target/spend.jar (there is no server to start)
make run         # the lesson's 25-row sample: the report, then "exit status: 2"
make cents       # 0.10 + 0.20 prints 0.30
make bom         # a file with a byte-order mark and "Café Noir" reads correctly
make test        # 57 unit, architecture and end-to-end tests, with JaCoCo coverage
make lint        # Spotless (google-java-format), Checkstyle, javac -Xlint:all -Werror
make quote-check # delete the quote handling in a copy: exactly one parser test fails
make stream-check# 3 million rows (125 MB) read with a 32 MB heap
make audit       # known-vulnerability scan of the dependencies (OSV-Scanner in Docker)
make down        # remove the build output
```

`make help` lists every target. To run the tool on your own file: `java -jar target/spend.jar my-export.csv`. Options and environment variables:

| Option or variable | Default | Meaning |
| --- | --- | --- |
| `--currency CODE`, `SPEND_CURRENCY` | `USD` | the report currency; rows in any other currency are rejected |
| `--top N`, `SPEND_TOP` | `5` | how many largest purchases to list |
| `SPEND_MAX_REJECTED` | `1000` | rejected rows kept for display; the count is always exact |
| `LOG_LEVEL` | `INFO` | log level; logs go to standard error as `key=value` lines |

Exit status: `0` every row read, `1` the file cannot be read (missing, not UTF-8), `2` at least one row rejected, `64` wrong usage. The report goes to standard output, logs to standard error, so `java -jar target/spend.jar f.csv 2>/dev/null` is the report alone.

What `make run` prints (38 lines, one screen; this is also `sample/expected-report.txt`, which a test compares byte for byte):

```text
Spending report (USD): 15 rows read, 10 rejected

2026-01
  Travel        1,204.50
  Groceries       145.80
  Fun              24.00
  Books            18.99
  Health           12.40
  Eating Out        4.50
  total         1,410.19

2026-02
  Eating Out      124.50
  Groceries        61.25
  Health           35.00
  Fun              24.00
  Books            22.40
  Travel           19.30
  total           286.45

Five largest purchases
    1,204.50  2026-01-05  Rail Pass (Travel)
      120.00  2026-02-14  Restaurant (Eating Out)
       61.25  2026-02-03  Corner Grocer (Groceries)
       55.03  2026-01-12  Corner Grocer (Groceries)
       48.60  2026-01-20  Corner Grocer (Groceries)

Rejected rows (10)
  line 6: date must look like 2026-01-31, got '31/02/2026'
  line 8: amount must not be null or negative: -8.00
  line 10: category must not be blank
  line 11: currency EUR is not the report currency USD
  line 13: amount is not a number: 'sixty'
  line 16: expected 5 columns, found 4
  line 18: expected 5 columns, found 6
  line 20: date must look like 2026-01-31, got '2026-13-10'
  line 25: unknown currency code 'US$'
  line 26: amount is not a number: ''
```

## Done when: the lesson's checklist and the proof for each line

| # | Done when (from the lesson) | Command | What proves it |
| --- | --- | --- | --- |
| 1 | Step 1: constructing a `Transaction` with a negative amount throws `IllegalArgumentException` with a message that names the field. | `make test` (`TransactionTest`) | `aNegativeAmountIsRejectedWithAMessageNamingTheField` expects exactly `amount must not be null or negative: -8.00`; the null amount, blank category (three spaces too), null date and null currency each have a test, and a zero amount is accepted on purpose. |
| 2 | Step 2: the ten planted bad rows come back as ten `Rejected` values, each with a reason a person could act on, and no stack trace is printed. | `make run` | The "Rejected rows (10)" block above, lines 6, 8, 10, 11, 13, 16, 18, 20, 25, 26, each with its own reason; standard error holds only two `INFO` log lines. `CsvRowParserEdgeCasesTest` checks each reason by text, so a row rejected by the wrong rule fails. Line 4, `"1,204.50"` in quotes, is a good row (it is the January `Travel` 1,204.50). |
| 3 | Step 3: a file saved with a byte-order mark and a description containing `é` both read correctly. | `make bom` | `1 rows read, 0 rejected` and `Café Noir`. `CsvFileRowSourceTest` also covers Windows line endings, blank lines, line numbers and a file that is not UTF-8 (reported as one line, exit status 1). |
| 4 | Step 4: the totals for one month match a hand calculation to the cent, and `0.10 + 0.20` prints `0.30`. | `make cents`, `make run` | `make cents` prints `total 0.30`. January groceries are 42.17 + 55.03 + 48.60 = 145.80, and the report prints `Groceries 145.80` (`LessonScenarioTest`). Amounts are `BigDecimal` built from strings and rounded once, half-even, in `ReportPrinter`; `SpendCliTest` shows 10.005 printing as 10.00 and 10.015 as 10.02 while the total stays 20.02. |
| 5 | Step 5: the output fits on one screen for your sample, and `echo $?` prints 2. | `make run` | 38 lines of output, and the last line `exit status: 2`. `SpendCliTest` also asserts 0 for a clean file, 1 for an unreadable one and 64 for wrong usage. |
| 6 | Step 6: the tests pass, and deleting the quote handling makes exactly one of them fail. | `make test`, then `make quote-check` | The five lesson tests are in `CsvRowParserTest` (good row, quoted amount, bad date, missing column, wrong currency). `make quote-check` copies the project, replaces the quote toggle in `CsvFieldSplitter` with a no-op and runs only that class: `Tests run: 5, Failures: 1`, the failing test being `aQuotedAmountWithAThousandsCommaStaysOneField`. |

The lesson's review checklist, answered: the rows are `BigDecimal` from strings (yes); the file is opened in a try-with-resources block with `StandardCharsets.UTF_8` named (`CsvFileRowSource`); names are descriptive, enforced by Checkstyle (`LocalVariableName` and friends require two or more characters); the bug story is in [ARCHITECTURE.md](ARCHITECTURE.md#the-bug-story) (the unquoted thousands comma that split a good row into six columns).

## Stretch goals

| Stretch (from the lesson) | Built? |
| --- | --- |
| Accept a `--currency` flag and convert with a rate table. | The flag is built (it picks the report currency, and rows in other currencies are still rejected). The conversion with a rate table is not built: it needs a rate source and a rule for rounding each converted row, which the lesson leaves open, and silently converting would hide the rejects that this tool exists to show. |
| Stream a 2 GB file without holding it in memory, and measure the heap. | Built, because streaming is the design. `make stream-check` writes 3,000,000 rows (125 MB) and reads them with `-Xmx32m`; it finishes in about 7 seconds with `2997000 rows read, 3000 rejected`. Holding 3 million rows in a list would not fit in 32 MB. To watch the heap yourself run `java -Xmx64m -jar target/spend.jar target/big.csv` and `jcmd <pid> GC.heap_info` from another terminal. |
| Replace `String category` with an enum plus an "other" case. | Not built. A category in a card export is open data, and an enum would force a code change for each new merchant category; the lesson lists it as an exercise in pattern matching only. |

## What is simplified for teaching

Only what the lesson scopes out:

- The CSV reader is the hand-written splitter the lesson asks for, not a CSV library: it handles quoted fields but not an escaped quote (`""`) or a newline inside a field. A production export with those needs a library behind the same `RowSource` port ([ADR 0004](docs/adr/0004-hand-written-csv-splitter.md)).
- Dates are ISO `yyyy-MM-dd` only. Amounts are plain decimals with an optional thousands comma; an exponent such as `1E+9` is rejected on purpose.
- One report currency per run; no conversion (see the stretch table).
- The lesson runs as a single file with `java Spend.java`; this build is a Maven project and a jar, so the parser can be tested and layered. `make run` is the equivalent command.

## Project structure

```text
src/main/java/com/plainround/spend/
  domain/            Transaction, ParsedRow (Ok | Rejected), SpendingAccumulator, SpendingReport and its
                     value types. Depends on the JDK only: no logging, no files, no text format.
  application/       BuildSpendingReport (the use case) and the RowSource port, plus the named
                     error InputUnreadableException. Depends on domain only.
  infrastructure/    csv/ (CsvFileRowSource, CsvRowParser, CsvFieldSplitter) implements the port;
                     Settings reads the environment. Depends on application and domain.
  interfaces/cli/    SpendCli (arguments, exit codes) and ReportPrinter (the text report, the only
                     place money is rounded). Depends on everything below; nothing depends on it.
src/main/resources/  logback.xml: key=value log lines on standard error.
src/test/java/       mirrors the packages above: unit tests per layer with in-memory fakes, the
                     end-to-end LessonScenarioTest and ArchitectureTest (ArchUnit).
config/              checkstyle.xml.
sample/              the lesson's 25-row file, its expected report and two small inputs.
scripts/             generate-big-csv.sh (stream-check) and prove-quote-test.sh (quote-check).
docs/adr/            decision records.
.github/workflows/   ci.yml: format, lint, tests with coverage, dependency audit.
pom.xml, mvnw, .mvn/ Maven build and the checked-in wrapper. Dockerfile: multi-stage image, non-root.
Makefile             the only commands a reader needs (make help).
```

The dependency rule (domain depends on nothing; application on domain; infrastructure and interfaces on both; nothing on interfaces) is enforced on every `make test` by `ArchitectureTest`, which also forbids `domain` from importing logging and `application` from touching `java.io` or `java.nio.file`.

## Quality gates

- **Format and lint:** `make lint` runs Spotless with google-java-format 1.36.1, Checkstyle 14.3.0 (`config/checkstyle.xml`: naming, Javadoc on the public surface, no TODO comments, no stray imports) and `javac -Xlint:all -Werror`.
- **Coverage:** JaCoCo reports to `target/site/jacoco/index.html`; `make test` fails below 95% line and 90% branch coverage of the `domain` package and 85% of lines overall. Measured on the last run: `domain`, `application`, `infrastructure` and `infrastructure.csv` at 100% of lines and branches, `interfaces.cli` at 97.8% of lines and 93.1% of branches (the process entry point `main` is not run in-process), 99.2% of lines overall.
- **Dependency audit:** `make audit` and the CI job `audit` run OSV-Scanner over `pom.xml`.
- **Build reproducibility:** exact versions, plugin versions pinned, Maven wrapper checked in.

## Troubleshooting

- **`UnsupportedClassVersionError` or "release 25 not supported":** the JDK on your path is older than 25. Run `java -version`; with SDKMAN, `sdk install java 25.0.2-tem`.
- **Accents look wrong in the terminal:** the tool reads UTF-8 regardless of the platform; set the terminal to UTF-8.
- **`make audit` fails to pull the image:** it needs Docker and network access to `ghcr.io`.

## Author and license

Written by Sumit Srivastava. Released under the MIT License, see [LICENSE](LICENSE). Changes are in [CHANGELOG.md](CHANGELOG.md).
