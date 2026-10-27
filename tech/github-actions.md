# GitHub Actions (workflow syntax)

## 1. What it is

GitHub Actions is GitHub's built-in automation. A **workflow** is a YAML file under `.github/workflows/`. GitHub starts it when an **event** happens (a push, a pull request). It contains **jobs**; each job runs on a clean virtual machine (a **runner**) and has **steps**: shell commands or reusable **actions**.

## 2. Why this repository uses it

The repository promises that CI runs format, lint, tests and the audit on every push. The workflow just calls the same `make` targets a reader runs locally, so CI and local results agree.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `.github/workflows/ci.yml` | the workflow |
| `Makefile` | the targets the steps call |

## 4. How it works underneath

- The workflow file is committed. GitHub reads it on each push and queues the jobs.
- Each job gets a new Ubuntu machine with common tools (Git, Docker, Make, many JDKs). Nothing is kept between jobs; a job can share files with another only through artifacts or caches.
- `uses: owner/repo@version` downloads that action's code from GitHub and runs it. `@v4` pins the major version.
- A step fails if its command exits non-zero; a failed step fails the job; the commit shows a red cross.
- Jobs run in parallel unless one `needs:` another.

## 5. Walkthrough of this repository's use

```yaml
name: ci

on: [push, pull_request]

permissions:
  contents: read
```

`name` is the label in the GitHub Actions page. `on: [push, pull_request]` is the **trigger**: run on every push to any branch and on every pull request. `permissions: contents: read` gives the workflow's automatic token read-only access to the repository, the least it needs (least privilege).

```yaml
  check:
    name: Format, lint, tests with coverage
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "25"
          cache: maven
      - run: make lint
      - run: make test
```

A **job** is a group of steps that runs on one fresh virtual machine (`runs-on: ubuntu-latest`). `check` runs the formatter, linter and tests. A step with `uses:` runs a ready-made **action**: `actions/checkout@v4` copies the repository onto the machine; `actions/setup-java@v4` installs Java (`distribution: temurin`, `java-version: "25"`) and, with `cache: maven`, caches `~/.m2` between runs. A step with `run:` is a shell command: here `make lint` and `make test` are the same commands you run locally (the CI mirrors the README).

```yaml
  audit:
    name: Dependency audit (OSV-Scanner)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: make audit
```

`audit` is a separate job, so it runs in parallel with `check`. It needs only the checkout and Docker (already on the runner) to run the OSV scanner.

```yaml
  image:
    name: Container image builds
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: make docker-run
```

The last job builds the container image with `make docker-run` (the image builds and prints the report).

## 6. Try it yourself

- `cat .github/workflows/ci.yml`. Expected: the file above.
- Run the same commands locally: `make lint`, `make test`, `make audit`. Expected: the same results CI shows.
- On GitHub, the Actions tab of a pushed repository. Expected: one run per push with the jobs `check`, `audit` and the third job.
- Validate the YAML: `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/ci.yml'))"` (needs PyYAML). Expected: no output.

## 7. Mistakes people make

- Giving a workflow more permissions than needed. This one has read-only `contents`.
- Pinning actions only by a moving tag (`@v4`) when supply-chain risk matters: a commit hash is stronger; the tag is a convenience here.
- CI that differs from local commands. Here the steps are `make` targets.
- Treating the workflow as the only place that checks the build; the local `make` targets are the same.
- Secrets in the workflow file. None are used.

## 8. Check yourself

1. What triggers the workflow?
   **Answer:** Every push and every pull request (`on: [push, pull_request]`).
2. Why does CI call `make lint` and `make test`?
   **Answer:** So CI and local runs use the same commands and give the same results.
3. What does `cache: maven` do?
   **Answer:** Saves `~/.m2` between runs so libraries are not downloaded every time.
4. What does `permissions: contents: read` do?
   **Answer:** Limits the workflow's automatic token to reading the repository.
5. Do the jobs share files?
   **Answer:** No. Each job has its own fresh machine.

## 9. Further reading

- Workflow syntax: https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions
- actions/checkout v4: https://github.com/actions/checkout ; actions/setup-java v4: https://github.com/actions/setup-java
- Security hardening: https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions
