# Git (repository, .gitignore)

## 1. What it is

**Git** is a program that keeps the history of a folder of files. Every time you save a snapshot (a **commit**), git records exactly what changed, who changed it, when, and a message saying why. You can look at any earlier snapshot, compare two, or go back. The history lives in a hidden folder called `.git`; everything else in the folder is the **working tree**, the files you see. A folder tracked this way is a **repository**.

## 2. Why this repository uses it

The repository promises meaningful commits and a clean tree (no uncommitted changes). Git also keeps the executable bit of `mvnw` and the scripts, so they run after a fresh clone.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `.git/` | the history (not part of the project files) |
| `.gitignore` | files that git must never track, walked through below |
| `CHANGELOG.md` | the human-readable summary of what the commits add |
| `mvnw`, `scripts/*.sh` | stored with the executable bit |

## 4. How it works underneath

1. **A commit** is a snapshot of every tracked file plus a message and a pointer to the previous commit. Commits form a chain: the *history*.
2. **Staging.** `git add file` marks a change to go into the next commit; `git commit -m "message"` saves the snapshot.
3. **Ignoring.** Git tracks only files you add, but it also *warns* about every untracked file in `git status`. `.gitignore` removes build output and local files from that list and prevents `git add .` from picking them up.
4. **Cloning.** `git clone <address> dir` copies the whole history to a new folder. A fresh clone contains only tracked files, which is the test of whether the repository is complete: `make test` must work in a fresh clone.
5. **Not tracked here:** `target/`, `.env`, editor folders, logs. Tracked: sources, the wrapper, configuration, documents, evidence the labs wrote.

## 5. Walkthrough of this repository's use

### `.gitignore`, line by line

```gitignore
# Build output and local state
```

A **comment (#)**-style comment line (`#`): the first group is build output and local state.

> New words:
>
> - **comment (#)**: a line (or the end of a line) starting with `#` is ignored by the shell; it is a note for the reader. The first line `#!...` is the only `#` line that has a meaning.

```gitignore
target/
.idea/
*.iml
.vscode/
.DS_Store
*.log
logs/
dependency-reduced-pom.xml
```

`target/` is Maven's output folder (compiled classes, test reports, the jar). The trailing slash means a folder. `.idea/` and `*.iml` are IntelliJ's project folder and files; `.vscode/` is Visual Studio Code's; `.DS_Store` is a hidden file that the macOS Finder creates in folders; `*.log` is any log file; `logs/` a folder of logs; `dependency-reduced-pom.xml` is a file the Shade plugin could write (see `maven-shade.md`).

```gitignore
# Environment files with real values
.env
```

The next group: `.env` is the personal copy of settings (see **.env file**); it may hold local values and is never committed.

> New words:
>
> - **.env file**: a plain text file of `NAME=value` lines that sets environment variables. `.env.example` is the committed template that lists every variable with a harmless default; you copy it to `.env` (which git ignores) to change values on your computer. No real secrets belong in the example file.

```gitignore
# Generated big test input
*.big.csv
```

`*.big.csv` is the large generated input of `make stream-check`, never committed.

```gitignore
.omc/
```

`.omc/` is a folder created by a tool used while the repository was written.

## 6. Try it yourself

- `git log --oneline`. Expected: the commit list, newest first.
- `git status --short`. Expected: no output when the working tree is clean.
- `git check-ignore -v target/classes`. Expected: a line naming the `.gitignore` rule that ignores it (`.gitignore:2:target/`).
- `git ls-files -s mvnw`. Expected: a line starting with `100755`, meaning the executable bit is stored.
- `git clone . /tmp/fresh-clone && cd /tmp/fresh-clone && make test` (a clean-copy check). Expected: the tests pass; nothing needed from the original folder.

## 7. Mistakes people make

- Committing generated files: they clutter the history and conflict.
- Committing secrets: a file in history stays there even after being deleted. `.env` is ignored for this reason.
- Adding a pattern to `.gitignore` for a file that is already tracked: git keeps tracking it until `git rm --cached` is run.
- Giant binary files in git; the lab's GC logs are ignored for that reason.
- Forgetting the executable bit on a new script: `Permission denied` on a fresh clone.

## 8. Check yourself

1. What is a commit?
   **Answer:** A saved snapshot of the tracked files with a message, linked to the previous snapshot.
2. What does `.gitignore` do?
   **Answer:** Lists files git should not track or offer to add.
3. Why is `.env` ignored but `.env.example` committed?
   **Answer:** `.env` may hold personal or secret values; the example documents the variables with harmless defaults.
4. How can you prove a repository is complete?
   **Answer:** Clone it fresh and run the build and tests there.
5. What does `100755` mean in `git ls-files -s`?
   **Answer:** A regular file with the executable bit set.

## 9. Further reading

- Pro Git book: https://git-scm.com/book/en/v2
- gitignore patterns: https://git-scm.com/docs/gitignore
