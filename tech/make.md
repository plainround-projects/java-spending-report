# GNU Make (3.81, the version on macOS)

## 1. What it is

A project has many jobs: build it, test it, check its style, run it. Each needs a long command line with options that nobody remembers. **Make** is a command-line program that runs named jobs described in a file called `Makefile`. You type `make test` and it finds the job `test` and runs its commands. Each job is a **target**; it can depend on other targets (**prerequisites**), and it has a **recipe**, the shell commands to run. Make was designed to rebuild files only when their sources changed, but it is also a good "menu of commands" for a project.

## 2. Why this repository uses it

The repository promises that a reader needs only a few short commands (`make up`, `run`, `test`, `lint`, `down`). The Makefile hides the long Maven, Java and Docker command lines behind those names and is the single place where they are written.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `Makefile` | all targets |
| `.env` (optional, not committed) | read by `-include .env` |
| `.github/workflows/ci.yml` | CI calls the same targets (`make lint`, `make test`, and more) |

## 4. How it works underneath

1. `make target` reads `Makefile`, finds the rule for `target`, first makes each prerequisite (recursively), then runs the recipe. If a target is the name of an existing file that is newer than its prerequisites, Make skips it; `.PHONY` targets are never skipped.
2. Each recipe line is run by a **separate** `sh` process. A `cd` or a variable assignment on one line is gone on the next. Commands that must share state go on one line, joined by `;`.
3. Make expands `$(NAME)` itself before starting the shell. To give the shell a `$`, write `$$`.
4. A recipe stops at the first command that fails (non-zero exit status), and `make` exits with an error. A line starting with `-` ignores failure; a line starting with `@` is not echoed before it runs.
5. This repository uses macOS's GNU Make 3.81, so it avoids newer Make features.

## 5. Walkthrough of this repository's use

The whole Makefile follows, from the first line to the last.

```make
# make up | run | test | lint | down      (make help lists everything)
```

A **comment (#)** line (Make uses `#` too) that summarizes the commands. `make help` lists everything.

> New words:
>
> - **comment (#)**: a line (or the end of a line) starting with `#` is ignored by the shell; it is a note for the reader. The first line `#!...` is the only `#` line that has a meaning.

```make
MVN := ./mvnw
JAR := target/spend.jar
SAMPLE := sample/transactions.csv
BIG := target/big.csv
BIG_ROWS ?= 3000000
IMAGE := plainround/spend:0.1.0
```

Six **Make variable**s. `MVN` is the Maven command (always the wrapper, see `maven-wrapper.md`). `JAR` is the file the build produces. `SAMPLE` is the lesson's input file. `BIG` is where the generated big test file goes. `BIG_ROWS ?= 3000000` is overridable: `make stream-check BIG_ROWS=1000` uses a smaller file. `IMAGE` is the name and version tag of the Docker image.

> New words:
>
> - **Make variable**: `NAME := value` defines a variable whose value is computed once, right there. `NAME ?= value` sets it only if no value exists yet, so a person can override it with `make NAME=other` or an environment variable. `$(NAME)` inserts the value. This happens inside Make, *before* the shell sees the line.

```make
-include .env
export
```

**-include and export**: read `.env` if it exists, and pass all Make variables on as environment variables.

> New words:
>
> - **-include and export**: `-include .env` reads the file `.env` (lines of `NAME=value`) as part of the Makefile if it exists; the leading `-` means no error if it is missing. A bare `export` makes every Make variable available as an environment variable to the commands that Make runs, so settings in `.env` reach the programs.

```make
.PHONY: help up run cents bom test lint format audit check stream-check quote-check docker-build docker-run down clean
```

**.PHONY**: every **target** name here is a command, not a file.

> New words:
>
> - **.PHONY**: lists targets that are not files. Without it, a file named `test` in the folder would make Make think the target `test` is already done.
> - **target**: the name of a job in a Makefile, written at the start of a line followed by a colon: `test:`. `make test` runs it.

```make
help: ## list the targets
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | sed -E 's/:.*## /\t/' | sort
```

`help` is the menu. The recipe is a **pipe**: **grep** `-E '^[a-z-]+:.*## '` finds the lines of `$(MAKEFILE_LIST)` (**$(MAKE), $(CURDIR), $(MAKEFILE_LIST)**) that look like `name: ... ## description`; **sed** `-E 's/:.*## /\t/'` replaces everything between the colon and the description by a tab; **sort** puts them in order. The `@` at the start of the line keeps the command itself from being printed (**recipe**). The `##` comments on every rule are the documentation (**help convention**).

> New words:
>
> - **pipe**: `a | b` connects the output of `a` to the input of `b`, so the two run together. `a | b | c` chains more.
> - **grep**: prints the lines of its input that match a pattern. `-E` uses extended regular expressions (`a|b` for alternatives), `-q` prints nothing and only sets the exit status, `-c` prints the number of matching lines, `-h` omits file names, `-v` inverts. Exit status 0 means at least one line matched, 1 means none.
> - **$(MAKE), $(CURDIR), $(MAKEFILE_LIST)**: built-in variables: `$(MAKE)` is the make program that is running now (used to call make again), `$(CURDIR)` is the current folder, `$(MAKEFILE_LIST)` is the list of makefiles read.
> - **sed**: "stream editor": applies edits to each line of its input. `sed -n 's/PATTERN/REPLACEMENT/p'` means: do not print lines by default (`-n`), substitute the first match of the pattern by the replacement in each line (`s/.../.../`), and print the line if a substitution happened (`p`). Parentheses `\(...\)` capture text and `\1` pastes it in the replacement. `sed -n 2p` prints only line 2. `sed -E` uses extended patterns (plain parentheses).
> - **sort**: sorts lines. `-n` compares as numbers, `-r` reverses, no option sorts as text.
> - **recipe**: the command lines under a rule. Each recipe line must begin with a real TAB character (not spaces) and is run by its own separate shell. A line beginning with `@` is run without being printed first; a failing command stops the recipe.
> - **help convention**: each target carries a comment `## description` on its rule line; the `help` target lists them by searching the Makefile for such lines.

```make
# There is no server to start: "up" builds the runnable jar, which is all "run" needs.
```

A comment: there is no server to start; `up` builds the runnable jar, which is all `run` needs.

```make
up: ## build target/spend.jar (tests skipped; make test runs them)
	$(MVN) -q -B clean package -DskipTests -Djacoco.skip=true
	@ls -l $(JAR)
```

`up` builds the program: `$(MVN) -q -B clean package -DskipTests -Djacoco.skip=true` (every option is explained in `maven.md`) and then `@ls -l $(JAR)` shows the file that was made, so you can see it exists.

```make
run: $(JAR) ## print the report for the lesson's sample file (exit status 2: ten rows are bad on purpose)
	@java -jar $(JAR) $(SAMPLE); status=$$?; echo "exit status: $$status"; test $$status -eq 2
```

`run` has the **prerequisite** `$(JAR)`: the jar must exist first. The recipe is one line, because each recipe line runs in its own shell: `java -jar $(JAR) $(SAMPLE)` runs the program on the sample file; `status=$$?` stores its exit status (**$$**); `echo "exit status: $$status"` prints it; `test $$status -eq 2` succeeds only if the status is 2. The expected status is 2 because ten rows of the sample are bad on purpose, so the target passes exactly when the program behaves.

> New words:
>
> - **prerequisite**: a name listed after the colon of a rule (`run: $(JAR)`). Make first makes each prerequisite, and only then runs the rule's own commands. If the prerequisite is a file that already exists and is up to date, nothing is done for it.
> - **$$**: because Make treats `$` as the start of its own variable, a dollar sign meant for the shell is written twice: `$$status` reaches the shell as `$status`, `$$?` as `$?` (the exit status of the last command), `$$(cmd)` as `$(cmd)`.

```make
cents: $(JAR) ## 0.10 + 0.20 prints 0.30, not 0.30000000000000004
	@java -jar $(JAR) sample/cents.csv 2>/dev/null | grep -E 'total|Treats'
```

A small demonstration: `2>/dev/null` hides the log lines (**redirection**) and `grep -E 'total|Treats'` keeps only the lines that prove the point of the lesson's first example: that 0.10 + 0.20 prints 0.30.

> New words:
>
> - **redirection**: `>file` sends what a command prints to a file; `>>file` appends; `<file` feeds a file to a command; `2>&1` sends the error stream to the same place as the normal output; `>&2` sends a message to the error stream (standard error); `>/dev/null` throws output away; `2>/dev/null` throws away only errors.

```make
bom: $(JAR) ## a file with a byte-order mark and an accented description reads correctly
	@java -jar $(JAR) sample/bom-and-accent.csv 2>/dev/null | grep -E 'rows read|Caf'
```

Another demonstration: a file starting with a byte-order mark and containing an accented description reads correctly; the output is filtered to the lines `rows read` and `Caf`.

```make
$(JAR):
	$(MAKE) --no-print-directory up
```

A **rule that makes a file**: it says how to create the jar. Its recipe calls make again (`$(MAKE) --no-print-directory up`, $(MAKE), $(CURDIR), $(MAKEFILE_LIST)). So `make run` builds the jar only when it is missing.

> New words:
>
> - **rule that makes a file**: a rule whose target is a real file name (`target/spend.jar:`) says how to create that file. A rule that lists it as a prerequisite (`run: $(JAR)`) makes Make create the file first if it does not exist.

```make
test: ## unit, architecture and end-to-end tests with coverage (target/site/jacoco/index.html)
	$(MVN) -B clean verify
```

`test` is the whole build including the checks: `$(MVN) -B clean verify` runs every phase up to `verify` (tests, coverage report, coverage rules).

```make
lint: ## Spotless (google-java-format) check, Checkstyle, compiler -Xlint:all -Werror
	$(MVN) -B spotless:check checkstyle:check clean compile test-compile
```

`lint` runs the formatter check, Checkstyle, and a clean compile of main and test code with the strict compiler flags (see `spotless-google-java-format.md` and `checkstyle.md`).

```make
format: ## rewrite the sources with google-java-format
	$(MVN) -B spotless:apply
```

`format` fixes the formatting in place.

```make
audit: ## known-vulnerability scan of the dependencies with OSV-Scanner (needs Docker)
	docker run --rm -v "$(CURDIR)":/src -w /src ghcr.io/google/osv-scanner:v2.7.0 scan source --recursive /src
```

`audit` runs a vulnerability scanner in a container (see `osv-scanner.md`): `docker run --rm` removes the container afterwards, `-v "$(CURDIR)":/src` shares the project folder as `/src`, `-w /src` sets the working folder inside, `ghcr.io/google/osv-scanner:v2.7.0` is the image, `scan source --recursive /src` is the scanner's command.

```make
check: lint test ## everything CI runs except the audit
```

`check` has **no recipe**; it only lists two prerequisites, so `make check` runs `lint` and then `test`.

```make
stream-check: up ## a 3-million-row file read with a 32 MB heap, which only works if the file is streamed
	scripts/generate-big-csv.sh $(BIG_ROWS) $(BIG)
	java -Xmx32m -jar $(JAR) $(BIG) | sed -n '1p;/^Rejected rows/p'
```

`stream-check` first builds the jar (prerequisite `up`), generates a 3-million-row file with the script, then runs the program with a **32 MB heap** (`-Xmx32m`). It can only succeed if the file is streamed and not loaded whole. sed `-n '1p;/^Rejected rows/p'` prints line 1 (the report header) and the line that begins `Rejected rows`.

```make
quote-check: ## delete the quote handling in a copy and show that exactly one parser test fails
	scripts/prove-quote-test.sh
```

`quote-check` calls the script that proves the quote handling is tested (see `bash.md`).

```make
docker-build: ## build the multi-stage image
	docker build -t $(IMAGE) .
```

`docker-build` builds the multi-stage image from the `Dockerfile` in this folder (`.`), tagged with `$(IMAGE)`.

```make
docker-run: docker-build ## run the image on the sample file
	docker run --rm -v "$(CURDIR)/sample":/data:ro $(IMAGE) /data/transactions.csv; test $$? -eq 2
```

`docker-run` first builds the image, then runs it on the sample folder mounted read-only (`-v ...:/data:ro`); `--rm` removes the container afterwards; then `test $$? -eq 2` expects the exit status 2 again.

```make
down: ## remove the build output
	$(MVN) -q -B clean
	rm -f $(BIG)
```

`down` removes the build output (`clean` of Maven) and the big generated file; `rm -f` does not complain if the file is missing.

```make
clean: down
```

`clean` is an alias: its only prerequisite is `down`.

## 6. Try it yourself

- `make help`. Expected: every target with its description.
- `make -n test`. Expected: prints the command that `make test` would run, without running it (`-n` is a dry run).
- `make up` then `make -n run`. Expected: only the `java -jar ...` line, because the jar already exists.
- `make lint test` in one go. Expected: both run, in order, and the second only starts if the first passed.
- `make nonsense`. Expected: `make: *** No rule to make target 'nonsense'.  Stop.`

## 7. Mistakes people make

- Spaces instead of a TAB at the start of a recipe line: `*** missing separator`. The Makefile uses real tabs.
- Forgetting `.PHONY`: a stray file named `test` would stop `make test` from running.
- Using `$status` instead of `$$status` in a recipe.
- Putting `cd dir` on one line and the command on the next.
- Treating Make as a build system for Java. Here Maven does the building; Make only names the commands.

## 8. Check yourself

1. What is a phony target?
   **Answer:** A target that is not a file; `.PHONY` makes Make always run its recipe.
2. Why is `$$?` written with two dollar signs?
   **Answer:** Make would treat a single `$` as its own variable; `$$` passes one `$` to the shell.
3. Why must `status=$$?; test $$status -eq 2` be on one line?
   **Answer:** Each recipe line runs in a separate shell, so the variable would be lost on the next line.
4. What does `?=` mean?
   **Answer:** Set the variable only if it has no value yet, so the caller can override it.
5. What does `-include .env` do when the file is missing?
   **Answer:** Nothing; the leading `-` makes a missing file not an error.

## 9. Further reading

- GNU Make manual: https://www.gnu.org/software/make/manual/make.html
- Phony targets: https://www.gnu.org/software/make/manual/make.html#Phony-Targets
- Recipes and shells: https://www.gnu.org/software/make/manual/make.html#Recipes
