# Git (ignore files, commits, fresh clone)

## 1. What it is

Git records the history of a folder as a series of **commits**, each a snapshot with a message. A **repository** is the folder plus that history (in the hidden `.git` folder). You choose what goes in each commit with `git add`, and what is never tracked with a `.gitignore` file.

## 2. Why this repository uses it

Each project folder here is its own Git repository, ready to be pushed to GitHub later. The history is part of the deliverable: small commits with clear messages that follow the order of the layers.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `.gitignore` | patterns of files Git ignores |
| `.dockerignore` | the same idea for the Docker build context |
| `.git/` | the history (not a file you edit) |
| `CHANGELOG.md` | the release notes in Keep a Changelog format |

## 4. How it works underneath

- A commit stores the content of the tracked files at that moment, a pointer to its parent commit, an author, a time and the message. The history is a chain of such commits.
- `git add` puts changes in the **staging area**; `git commit` turns the staged changes into a commit.
- Files matching `.gitignore` are not listed by `git status` and are skipped by `git add .`; a file that is already tracked stays tracked even if it is later ignored.
- `git status --short` prints one line per changed or untracked file; empty output means a clean working tree.

## 5. Walkthrough of this repository's use

```
# Build output and local state
target/
.idea/
*.iml
.vscode/
.DS_Store
*.log
logs/
dependency-reduced-pom.xml
# Environment files with real values
.env
# Generated big test input
*.big.csv
.omc/
```

`.gitignore` lists patterns of files Git must not track. `target/` is the build output folder; `.idea/`, `*.iml`, `.vscode/` and `.DS_Store` are editor and macOS leftovers; `*.log` and `logs/` are logs; `dependency-reduced-pom.xml` is a file the Shade plugin can write; `.env` is the file for real settings (the committed `.env.example` has no secrets); `*.big.csv` is a generated test input;  `.omc/` is tool state. A trailing `/` matches folders. The rule from the repository template is: build artifacts and local state are never committed, so `git status` after the last commit shows nothing untracked.

```
.git
.github
target
.idea
.omc
*.log
```

`.dockerignore` works the same way for `docker build`: these paths are not sent to Docker as build context, which keeps builds fast and keeps `.git` and `target` out of the image build.

**Commits.** The history uses *conventional commits*: a type, a scope in parentheses, a colon and a short summary, for example `feat(domain): add Transaction...`, `build: ...`, `test: ...`, `docs: ...`, `fix(...)`. The body ends with a `Co-Authored-By:` line. Each commit is one logical change (domain, application, infrastructure, interfaces, tests, build files, docs) so the history reads as the order the program was built.

**Fresh clone.** `git clone <folder> /tmp/fresh && cd /tmp/fresh` copies the committed files only. Following the README from there is the proof that nothing depends on files that happen to exist on the author's machine.

## 6. Try it yourself

- `git log --oneline`. Expected: the commits, newest first, each starting with a type such as `feat(...)`, `build:`, `test:` or `docs:`.
- `git status --short`. Expected: no output (clean tree).
- `git ls-files | wc -l`. Expected: the number of tracked files.
- `git check-ignore -v target/` . Expected: the `.gitignore` line that ignores the folder.
- `git clone . /tmp/fresh-copy && ls /tmp/fresh-copy`. Expected: only committed files; no `target/`.

## 7. Mistakes people make

- Committing build output or editor files; `.gitignore` prevents it.
- Committing secrets. `.env` is ignored and `.env.example` holds names only.
- Huge mixed commits; this history splits by layer.
- Expecting `.gitignore` to untrack a file already committed (use `git rm --cached`).

## 8. Check yourself

1. What is the staging area?
   **Answer:** The set of changes chosen for the next commit.
2. What does an empty `git status --short` mean?
   **Answer:** No changed and no untracked files.
3. Why is `.env` ignored but `.env.example` committed?
   **Answer:** `.env` may hold real settings; `.env.example` only lists names and safe defaults.
4. What is a fresh-clone test?
   **Answer:** Cloning the repository into a new folder and following the README, to prove nothing depends on files that are not committed.
5. What does `feat(domain): ...` tell you?
   **Answer:** The commit type (a feature) and the part of the code it touches.

## 9. Further reading

- Pro Git book: https://git-scm.com/book/en/v2
- gitignore: https://git-scm.com/docs/gitignore
- Conventional Commits 1.0.0: https://www.conventionalcommits.org/en/v1.0.0/
- Keep a Changelog 1.1.0: https://keepachangelog.com/en/1.1.0/
