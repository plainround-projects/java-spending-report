# GNU Make (3.81, the version on macOS)

## 1. What it is

`make` is a command-line tool that runs named jobs described in a file called `Makefile`. Each job is a **target**; it can depend on other targets (**prerequisites**), and it has a **recipe**, the shell commands to run. Make was designed to rebuild files only when their sources changed, but it is also a good "menu of commands" for a project.

## 2. Why this repository uses it

The repository promises that a reader needs only a few short commands (`make up`, `run`, `test`, `lint`, `down`). The Makefile hides the long Maven, Java and Docker command lines behind those names and is the single place where they are written.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `Makefile` | all targets |
| `.env` (optional, not committed) | read by `-include .env` |
| `.github/workflows/ci.yml` | CI calls the same targets: `make lint`, `make test` |

## 4. How it works underneath

- `make target` reads `Makefile`, finds the rule for `target`, first makes each prerequisite (recursively), then runs the recipe. If a target is the name of an existing file and is newer than its prerequisites, Make skips it; `.PHONY` targets are never skipped.
- Each recipe line is run by a **separate** `sh` process. A `cd` or a variable assignment on one line is gone on the next. Commands that must share state go on one line.
- Make expands `$(NAME)` itself before starting the shell. To give the shell a `$`, write `$$`.
- A recipe stops at the first command that fails (non-zero exit status), and `make` exits with an error. A line starting with `-` ignores failure; a line starting with `@` is not echoed.
- This repository uses macOS's GNU Make 3.81, so it avoids newer Make features.

## 5. Walkthrough of this repository's use

### Header and `help`

```make
# make up | run | test | lint | down      (make help lists everything)
MVN := ./mvnw
JAR := target/spend.jar
SAMPLE := sample/transactions.csv
BIG := target/big.csv
BIG_ROWS ?= 3000000
IMAGE := plainround/spend:0.1.0

-include .env
export
```

`#` starts a comment. `NAME := value` defines a variable; `:=` computes the value once, immediately. `?=` sets it only if it is not already set (so `make BIG_ROWS=1000 ...` or an environment variable can override it). `$(NAME)` uses a variable. `-include .env` reads a file of settings if it exists (the leading `-` means: no error if it is missing). `export` with no names makes every Make variable available as an environment variable to the commands it runs, so `.env` settings reach the programs.

```make
.PHONY: help up run cents bom test lint format audit check stream-check quote-check docker-build docker-run down clean
```

`.PHONY` lists targets that are **not files**. Without it, a file named `test` in the folder would make `make test` think there is nothing to do.

```make
help: ## list the targets
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | sed -E 's/:.*## /\t/' | sort
```

`make help` lists the targets. The recipe is a shell pipeline: `grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST)` finds lines that look like `name: ... ## description`; `sed -E 's/:.*## /\t/'` turns each into `name<TAB>description`; `sort` orders them. `$(MAKEFILE_LIST)` is the list of makefiles read. The `##` comments on every target are the documentation. The `@` at the start of a recipe line hides the command itself.

### The targets

```make
# There is no server to start: "up" builds the runnable jar, which is all "run" needs.
up: ## build target/spend.jar (tests skipped; make test runs them)
	$(MVN) -q -B clean package -DskipTests -Djacoco.skip=true
	@ls -l $(JAR)
```

A **rule** is `target: prerequisites` followed by tab-indented **recipe** lines. `up` has no prerequisites. It runs Maven (`$(MVN)` is `./mvnw`) with `-q` quiet, `-B` batch mode, goals `clean package`, and two properties: `-DskipTests` (build without running tests) and `-Djacoco.skip=true` (no coverage agent). The comment explains the name: nothing to start, so "up" builds the jar.

```make
run: $(JAR) ## print the report for the lesson's sample file (exit status 2: ten rows are bad on purpose)
	@java -jar $(JAR) $(SAMPLE); status=$$?; echo "exit status: $$status"; test $$status -eq 2
```

```make
$(JAR):
	$(MAKE) --no-print-directory up
```

`run: $(JAR)` means: before `run`, the file `target/spend.jar` must exist. The rule `$(JAR):` with no prerequisites says how to make that file: call `make up` again (`$(MAKE)` is the running make; `--no-print-directory` keeps the output quiet). So `make run` builds the jar only when it is missing. In the recipe, **each line runs in its own shell**, so everything that must share a variable is on one line joined by `;`. `$$status` is written with two dollar signs because Make would otherwise treat `$status` as its own variable; `$$` passes a single `$` to the shell. `$$?` is the exit status of the Java program. The line prints it and then runs `test $$status -eq 2`, which succeeds only if the status was 2. Exit status 2 is the expected result, because ten rows are bad on purpose.

```make
cents: $(JAR) ## 0.10 + 0.20 prints 0.30, not 0.30000000000000004
	@java -jar $(JAR) sample/cents.csv 2>/dev/null | grep -E 'total|Treats'

bom: $(JAR) ## a file with a byte-order mark and an accented description reads correctly
	@java -jar $(JAR) sample/bom-and-accent.csv 2>/dev/null | grep -E 'rows read|Caf'
```

Two small demonstrations. `2>/dev/null` hides the log lines and `grep -E` keeps only the lines that prove the point (`total`, `Treats`; `rows read`, `Caf`).

```make
test: ## unit, architecture and end-to-end tests with coverage (target/site/jacoco/index.html)
	$(MVN) -B clean verify
```

```make
lint: ## Spotless (google-java-format) check, Checkstyle, compiler -Xlint:all -Werror
	$(MVN) -B spotless:check checkstyle:check clean compile test-compile
```

```make
format: ## rewrite the sources with google-java-format
	$(MVN) -B spotless:apply
```

`test` is the whole build including checks: `verify` runs every phase up to it (tests, coverage report, coverage rules). `lint` runs the formatter check, Checkstyle, and a clean compile of main and test code with the strict compiler flags. `format` fixes the formatting.

```make
audit: ## known-vulnerability scan of the dependencies with OSV-Scanner (needs Docker)
	docker run --rm -v "$(CURDIR)":/src -w /src ghcr.io/google/osv-scanner:v2.7.0 scan source --recursive /src
```

```make
check: lint test ## everything CI runs except the audit
```

`audit` runs the vulnerability scanner in a container (see `osv-scanner.md` and `docker.md`): `--rm` removes the container afterwards, `-v "$(CURDIR)":/src` shares the project folder as `/src`, `-w /src` sets the working folder. `$(CURDIR)` is Make's current folder. `check: lint test` has **no recipe**; it just names two prerequisites, so `make check` runs both.

```make
stream-check: up ## a 3-million-row file read with a 32 MB heap, which only works if the file is streamed
	scripts/generate-big-csv.sh $(BIG_ROWS) $(BIG)
	java -Xmx32m -jar $(JAR) $(BIG) | sed -n '1p;/^Rejected rows/p'
```

```make
quote-check: ## delete the quote handling in a copy and show that exactly one parser test fails
	scripts/prove-quote-test.sh
```

`stream-check: up` builds the jar first, generates a 3-million-row file with the script, then runs the program with a **32 MB heap** (`-Xmx32m`). It only works because the file is streamed. `sed -n '1p;/^Rejected rows/p'` prints line 1 and the line starting `Rejected rows`. `quote-check` just calls the script.

```make
docker-build: ## build the multi-stage image
	docker build -t $(IMAGE) .

docker-run: docker-build ## run the image on the sample file
	docker run --rm -v "$(CURDIR)/sample":/data:ro $(IMAGE) /data/transactions.csv; test $$? -eq 2
```

```make
down: ## remove the build output
	$(MVN) -q -B clean
	rm -f $(BIG)

clean: down
```

`docker-run: docker-build` builds the image first, then runs it on the sample folder mounted read-only (`:ro`). `down` removes the build output (`clean` is an alias with `down` as its prerequisite).

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
