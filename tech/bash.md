# Bash (and POSIX shell)

## 1. What it is

A **shell** is a program that reads commands you type (or that a file contains) and runs other programs. **Bash** is the most common shell on macOS and Linux. A **shell script** is a text file of commands. Besides running programs, the shell offers variables, `if`/`for`, functions, **pipes** (`a | b` sends the output of `a` to the input of `b`) and **redirections** (`> file` writes output to a file).

## 2. Why this repository uses it

Some jobs are easier as a short script than as Java: generating a test file, making a temporary copy and breaking it on purpose, starting a JVM with special flags and asking it questions with `jcmd`. The scripts glue those tools together and the Makefile calls them.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `scripts/generate-big-csv.sh` | script run by a `make` target |
| `scripts/prove-quote-test.sh` | script run by a `make` target |
| `mvnw` | the Maven wrapper, a long POSIX `sh` script (not edited by hand) |
| `Makefile` recipes | every recipe line is a shell command |

## 4. How it works underneath

- The shell is itself a program (`bash`). When you run `./scripts/x.sh`, the system reads the first line (`#!/usr/bin/env bash`, the shebang) and starts bash with the file as its input. The file must be executable (`chmod +x`).
- A command is a program name followed by arguments. The shell splits the line into words, expands variables and `$(...)`, and starts the program as a **process**. The process ends with an **exit status**: 0 means success, anything else failure. `$?` holds the last status; `if`, `&&` and `||` use it.
- Every process has three streams: standard input (0), standard output (1) and standard error (2). `>` redirects output to a file, `2>` redirects errors, `2>&1` joins errors to output, `|` connects output to the next command's input.
- Quotes: `"double"` keep spaces together but still expand `$var`; `'single'` change nothing. Unquoted variables split into words, which is why scripts write `"$var"`.
- `set -euo pipefail` makes a script fail early instead of continuing after an error. `trap '...' EXIT` runs cleanup when the script ends.
- A command followed by `&` runs in the background; `$!` is its process id; `wait pid` waits for it; `kill pid` asks it to stop with `SIGTERM`.

## 5. Walkthrough of this repository's use

### scripts/generate-big-csv.sh: make a big test file

```bash
#!/usr/bin/env bash
# Usage: generate-big-csv.sh ROWS OUTPUT
# Writes ROWS transactions in the sample's format; every 1000th row is bad on purpose.
set -euo pipefail
rows="${1:?rows}"
output="${2:?output file}"
mkdir -p "$(dirname "$output")"
```

`#!/usr/bin/env bash` is the **shebang**: when the file is run as a program, the system starts `bash` (found through `env`, which searches the `PATH`) and gives it this file. Lines starting with `#` are comments. `set -euo pipefail` makes the script strict: `-e` stops at the first command that fails, `-u` treats an unset variable as an error, and `-o pipefail` makes a pipeline fail if any command in it fails (normally only the last one counts). `"${1:?rows}"` reads the first argument and, if it is missing, stops with the message `rows`; the `:?` form is how a script demands an argument. The quotes keep a value with spaces together. `mkdir -p "$(dirname "$output")"` makes the output folder (and its parents); `-p` also means "no error if it exists"; `$(...)` runs a command and uses its output; `dirname` prints the folder part of a path.

```bash
awk -v rows="$rows" 'BEGIN {
  print "date,description,amount,currency,category";
  split("Groceries,Travel,Books,Health,Fun,Eating Out,Home", categories, ",");
  for (row = 1; row <= rows; row++) {
    month = (row % 12) + 1; day = (row % 28) + 1; category = categories[(row % 7) + 1];
    if (row % 1000 == 0) { printf "2026-%02d-%02d,Bad %d,oops,USD,%s\n", month, day, row, category; continue }
    printf "2026-%02d-%02d,Shop %d,%d.%02d,USD,%s\n", month, day, row, (row % 500) + 1, row % 100, category
  }
}' > "$output"
```

`awk` is a small language for processing text. `-v rows="$rows"` passes the shell value in as an awk variable. The program is inside single quotes (so the shell does not touch `$` or `%` in it). `BEGIN { ... }` runs once before any input; there is no input here, so awk is used as a generator. It prints the header, splits a comma list into the array `categories`, and loops `row` from 1 to `rows`. `%` is the remainder, so months, days and categories cycle. `printf` formats lines (`%02d` is a number padded to two digits). Every 1000th row is made bad (`oops` as amount) and `continue` skips the normal line. `> "$output"` sends all of awk's output to the file.

```bash
echo "wrote $rows rows to $output ($(wc -c < "$output" | tr -d ' ') bytes)" >&2
```

The last line reports to **standard error** (`>&2` sends it to file descriptor 2): `wc -c < file` counts bytes, and `tr -d ' '` deletes the spaces that macOS `wc` adds. Diagnostics go to standard error so a caller can still pipe the real output.

### scripts/prove-quote-test.sh: prove that one test guards the quote handling

```bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
```

`root="$(cd "$(dirname "$0")/.." && pwd)"` finds the project folder whatever folder you run the script from: `$0` is the script's own path, `dirname` its folder, `..` the parent, and `pwd` prints the absolute path. `mktemp -d` makes a fresh temporary folder. `trap 'rm -rf "$work"' EXIT` registers a command to run when the script ends for any reason, so the temporary copy is always removed.

```bash
rsync -a --exclude target --exclude .git "$root"/ "$work"/
splitter="$work/src/main/java/com/plainround/spend/infrastructure/csv/CsvFieldSplitter.java"
# Replace the quote toggle with a no-op, i.e. every comma now splits.
sed -i.bak 's/insideQuotes = !insideQuotes;/insideQuotes = false;/' "$splitter"
cmp -s "$splitter" "$splitter.bak" && { echo "mutation did not apply" >&2; exit 1; }
```

`rsync -a --exclude target --exclude .git "$root"/ "$work"/` copies the project (archive mode keeps permissions) without the build output and git data. The script then **mutates the copy, never the original**: `sed -i.bak 's/insideQuotes = !insideQuotes;/insideQuotes = false;/'` edits the file in place (`-i.bak` keeps a `.bak` backup, which works on both macOS and Linux `sed`) and replaces the line that flips the quote state with one that never does. `cmp -s a b` compares two files quietly; if they are **equal**, the edit did not change anything, so the script prints an error and stops (`&&` runs the next command only if the previous one succeeded; `{ ...; }` groups commands).

```bash
cd "$work"
set +e
./mvnw -q -B test -Dtest=CsvRowParserTest -Djacoco.skip=true -Dspotless.check.skip=true >out.log 2>&1
set -e
failed="$(grep -E 'Tests run:.*Failures' out.log | tail -1)"
echo "$failed"
grep -E '^\[ERROR\]   CsvRowParserTest\.' out.log || true
echo "$failed" | grep -q 'Tests run: 5, Failures: 1' && echo "OK: exactly one of the five parser tests fails" \
  || { echo "UNEXPECTED: see $work/out.log" >&2; trap - EXIT; exit 1; }

```

`set +e` turns off "stop on error" for one command, because the tests are *expected* to fail; `set -e` turns it back on. The test run's output goes to `out.log` (`>out.log 2>&1` sends both output streams to the file). `grep -E ... | tail -1` takes the last summary line. The final `&&` / `||` pair is an if-else on one line: if the summary says `Tests run: 5, Failures: 1`, print OK; otherwise print where the log is, cancel the cleanup trap (`trap - EXIT`) so the log survives, and exit with status 1.

## 6. Try it yourself

- `bash -n scripts/generate-big-csv.sh`. Expected: no output (the syntax is valid; `-n` reads without running).
- `ls -l scripts/`. Expected: lines starting `-rwx`, so the scripts are executable.
- `bash -x scripts/generate-big-csv.sh 2>&1 | head -20` with the arguments the README gives. Expected: each command printed with a `+` before it as it runs (`-x` traces).
- `echo $?` right after a command. Expected: `0` after success, a non-zero number after failure.
- `man bash` or `help set`. Expected: the reference for everything above.

## 7. Mistakes people make

- Forgetting quotes around variables: a path with a space becomes two words.
- Parsing `ls` output, or using `for f in $(cat file)`. Both break on spaces.
- A pipeline hides failures unless `pipefail` is set; here it is.
- `set -e` stops the script on the *expected* failure of a command (such as `grep` finding nothing); the scripts here add `|| true` exactly where a non-zero status is normal.
- Leaving temporary files behind. `mktemp -d` plus `trap ... EXIT` removes them.
- Writing a script that only works from one folder. These scripts compute their own location from `$0` or `BASH_SOURCE`.

## 8. Check yourself

1. What does `set -euo pipefail` do?
   **Answer:** Stops on the first failing command (`-e`), treats unset variables as errors (`-u`), and makes a pipeline fail if any part fails (`pipefail`).
2. What is the difference between `>` and `>>`?
   **Answer:** `>` creates or overwrites the file; `>>` appends.
3. What does `cmd1 && cmd2 || cmd3` do?
   **Answer:** Runs `cmd2` if `cmd1` succeeded; runs `cmd3` if `cmd1` or `cmd2` failed.
4. What is `$!`?
   **Answer:** The process id of the most recent background command.
5. Why does a script use `trap ... EXIT`?
   **Answer:** To run cleanup (delete temporary files, stop processes) however the script ends.

## 9. Further reading

- GNU Bash manual (the shell on macOS is 3.2 and these scripts avoid newer features): https://www.gnu.org/software/bash/manual/bash.html
- POSIX shell command language: https://pubs.opengroup.org/onlinepubs/9699919799/utilities/V3_chap02.html
