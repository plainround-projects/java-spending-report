# GitHub Actions

## 1. What it is

**Continuous integration (CI)** means running the checks automatically on every change, on a clean machine, so that "it works on my computer" is never the only evidence. **GitHub Actions** is GitHub's service for this. You write a **workflow** file in `.github/workflows/`; GitHub starts a fresh virtual machine, checks out the code and runs your jobs whenever the chosen events happen.

## 2. Why this repository uses it

The repository promises a CI workflow that runs the same Makefile targets a person runs locally. The workflow therefore contains almost no logic of its own: each step is `make <target>`.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `.github/workflows/ci.yml` | the workflow, walked through below |
| `Makefile` | every `run:` step calls a target |

## 4. How it works underneath

1. GitHub reads every `.yml` file under `.github/workflows/`. The `on:` key decides when a workflow starts.
2. Each **job** gets its own fresh virtual machine; jobs run in parallel unless `needs:` orders them. Nothing is shared between jobs.
3. A **step** is either `uses:` (run a packaged action, identified by `owner/name@version`) or `run:` (run a shell command).
4. A step fails if its command exits with a non-zero status; the job stops (unless `if: always()` marks a later step) and the whole run is marked red.
5. The workflow could not be run in the authoring environment; its commands are the same ones verified locally.

## 5. Walkthrough of this repository's use

The whole workflow follows, from the first line to the last.

```yaml
name: ci

on: [push, pull_request]
```

`name: ci` is the workflow's name shown in the **GitHub Actions** page. **YAML**: `key: value` pairs and indentation. `on: [push, pull_request]` lists the events that start it: someone pushes commits, or opens or updates a pull request (a proposal to merge changes).

> New words:
>
> - **GitHub Actions**: GitHub's built-in automation. A **workflow** is a YAML file in `.github/workflows/` that says: on which events (a push, a pull request) start which **jobs**; each job runs on a fresh virtual machine (`runs-on`) and consists of **steps**: `uses:` runs a ready-made action, `run:` runs a shell command.
> - **YAML**: a text format for nested data that uses indentation instead of brackets. `key: value` is a pair; a line starting with `- ` is an item of a list; a block indented under `key:` belongs to that key. Text in quotes stays text; `#` starts a comment. See `yaml.md`.

```yaml
permissions:
  contents: read
```

`permissions: contents: read` gives the workflow's automatic token the least power: it can read the repository and do nothing else.

```yaml
jobs:
  check:
    name: Format, lint, tests with coverage
    runs-on: ubuntu-latest
    steps:
```

`jobs:` holds the jobs; jobs run in parallel unless one says it needs another. The first job has the id `check` and the display name `Format, lint, tests with coverage`; `runs-on: ubuntu-latest` picks a fresh virtual machine with Ubuntu.

```yaml
      - uses: actions/checkout@v4
```

Each step starts with `-`. `uses: actions/checkout@v4` runs a ready-made action that downloads the repository's files onto the machine; `@v4` is the version.

```yaml
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "25"
          cache: maven
```

`actions/setup-java@v4` installs Java; its `with:` block gives the inputs: the Temurin distribution, version 25, and `cache: maven`, which saves the downloaded libraries between runs.

```yaml
      - run: make lint
      - run: make test
```

`run:` executes a shell command on the machine: `make lint` (formatter check, Checkstyle, strict compile), then `make test` (all tests with coverage).

```yaml
      - name: Package and run the lesson's scenario
        run: make run
```

A step with a `name:`: `make run` builds the jar and runs the lesson's sample, expecting exit status 2.

```yaml
  audit:
    name: Dependency audit (OSV-Scanner)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: make audit
```

The second job, `audit`: checkout and then `make audit`, which scans the dependencies with OSV-Scanner in a container (see `osv-scanner.md`). It needs no Java.

```yaml
  image:
    name: Container image builds
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: make docker-run
```

The third job, `image`: `make docker-run` builds the container image and runs it on the sample file.

## 6. Try it yourself

- Read it locally with `cat .github/workflows/ci.yml` and compare each `run:` line with a target in `make help`.
- Run the same steps yourself: `make lint`, `make test`, `make audit`. Expected: the same results CI shows.
- After pushing to GitHub, open the repository's Actions tab. Expected: a run named `ci` with one entry per job.

## 7. Mistakes people make

- Putting logic in the workflow instead of the Makefile: it then cannot be run or debugged locally.
- Unpinned action versions (`@main`): the workflow changes without a commit. Here `@v4` is used.
- Giving the token more permissions than needed: here `contents: read` only.
- Forgetting `if: always()` on a cleanup step; without it the step is skipped after a failure.

## 8. Check yourself

1. What starts the workflow?
   **Answer:** A push or a pull request (`on: [push, pull_request]`).
2. Do two jobs share files?
   **Answer:** No. Each runs on its own fresh virtual machine.
3. What does `uses: actions/checkout@v4` do?
   **Answer:** Downloads the repository's files onto the machine.
4. What does `if: always()` do?
   **Answer:** Runs the step even if an earlier step failed.
5. Why are the steps just `make ...`?
   **Answer:** So a person can run exactly the same commands locally.

## 9. Further reading

- Workflow syntax: https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions
- actions/checkout: https://github.com/actions/checkout
- actions/setup-java: https://github.com/actions/setup-java
