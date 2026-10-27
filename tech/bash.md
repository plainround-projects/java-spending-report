# Bash and shell scripts (bash 3.2 compatible)

## 1. What it is

When you type a command in a **terminal** (the window where you type text commands), a program called the **shell** reads it and starts the program you named. `ls -l` is the program `ls` with the argument `-l`. A **shell script** is a text file of such commands that the shell runs from top to bottom, the way you would type them. Scripts are how people automate: "start the server, wait until it answers, generate load for five minutes, record the numbers". **Bash** is the most widely used shell; it adds variables, loops, functions and arrays to the original `sh`. A **process** is a running program; a script starts many of them, one after another or at the same time.

## 2. Why this repository uses it

The lesson's steps need more than one command: generating a big file, proving a test fails when it should, timing a crawl three times, running a 30-minute soak. Those jobs are written as scripts in the repository, so they run the same way for everyone and the evidence they write can be re-created.

## 3. Where it appears

| Script | Job |
| --- | --- |
| `scripts/generate-big-csv.sh` | makes a big input file for `make stream-check` |
| `scripts/prove-quote-test.sh` | proves that the quote handling is tested (`make quote-check`) |

## 4. How it works underneath

1. **Starting.** `./scripts/x.sh` asks the operating system to run the file. The first line `#!/usr/bin/env bash` tells it to start `bash` (found through `env`) and give it the file. The file must be executable (`chmod +x`); git remembers that.
2. **Streams.** Every process has three: standard input (0), standard output (1) and standard error (2). A pipe `a | b` connects output of `a` to input of `b`. Redirections (`>file`, `2>&1`) send them to files. Messages for people go to standard error so that standard output carries only data.
3. **Exit status.** Every process ends with a number: 0 is success, anything else is failure. `&&`, `||`, `if` and `set -e` act on it; `$?` is the last one.
4. **Variables and expansion.** Before running a command the shell replaces `$name`, `${name}`, `$(command)` and `$(( expression ))` by their values, then splits the result into words (unless it is in double quotes) and expands patterns like `*.zip`. That is why scripts always write `"$name"`.
5. **Strict mode.** Each script begins with `set -euo pipefail`: stop at the first failing command, treat unset variables as errors, and make a pipe fail if any stage fails. Places where failure is expected say so with `|| true`.
6. **Cleanup.** Scripts that start servers register a `trap ... EXIT` that kills them and deletes temporary folders, so Ctrl+C or an error leaves nothing running.
7. **macOS compatibility.** The default bash on macOS is 3.2 (2007). The scripts avoid newer features and use only `sed`, `awk` and `date` options that exist in both the BSD tools of macOS and the GNU tools of Linux.

## 5. Walkthrough of this repository's use

### `scripts/generate-big-csv.sh`

```bash
#!/usr/bin/env bash
```

The **shell script**'s first line starts it with **bash**.

> New words:
>
> - **shell script**: a text file of shell commands, run from top to bottom as if you had typed them. The first line `#!/bin/sh` (the "shebang") says which program should run the file. To run it you make it executable (`chmod +x file`) and type `./file`.
> - **bash**: a shell, an extended version of `sh`. A script that starts with `#!/usr/bin/env bash` asks the system to find `bash` wherever it is installed (`env` looks it up in the PATH) and run the file with it. The macOS built-in bash is version 3.2, old, so these scripts avoid newer bash features.

```bash
# Usage: generate-big-csv.sh ROWS OUTPUT
# Writes ROWS transactions in the sample's format; every 1000th row is bad on purpose.
```

Two **comment (#)** lines: how to call the script (`generate-big-csv.sh ROWS OUTPUT`) and what it does: it writes ROWS fake transactions in the format of the sample file, and every 1000th row is bad on purpose, so the report has some rejected rows to count.

> New words:
>
> - **comment (#)**: a line (or the end of a line) starting with `#` is ignored by the shell; it is a note for the reader. The first line `#!...` is the only `#` line that has a meaning.

```bash
set -euo pipefail
```

Strict mode (**set -e -u -f -o pipefail**): stop on the first failing command, treat unset variables as errors, and fail a pipe if any command in it fails.

> New words:
>
> - **set -e -u -f -o pipefail**: options that make a script strict. `-e` stop at the first failing command; `-u` treat an unset variable as an error; `-f` switch off filename patterns like `*` (so a stray `*` is not expanded); `-o pipefail` makes a pipe fail if any command in it fails, not only the last. `set +e` switches one off again; `set -x` prints every command before it runs (for debugging).

```bash
rows="${1:?rows}"
output="${2:?output file}"
```

The two arguments, each read with **${1:?text}**: `rows` is the first argument (the number of rows), `output` the second (the file to write). If one is missing the script stops with the message `rows` or `output file`.

> New words:
>
> - **${1:?text}**: reads the first argument and, if it is missing or empty, stops the script with the error message `text`. It is a one-line way to say "this argument is required".

```bash
mkdir -p "$(dirname "$output")"
```

Makes sure the folder of the output file exists. `dirname "$output"` gives the folder part of the path (**command substitution**); `-p` creates missing parent folders and does not complain if it exists.

> New words:
>
> - **command substitution**: `$(command)` runs the command and is replaced by what it printed. `"$(dirname "$0")"` is the folder of the script.

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

The whole file is produced by one **awk** program. `-v rows="$rows"` passes the shell variable into awk as its own variable `rows`. The program is a single `BEGIN { ... }` block (it needs no input file). `print "date,..."` writes the header line. `split("Groceries,Travel,...", categories, ",")` cuts the text at each comma and fills the array `categories` (awk arrays start at position 1, so `categories[1]` is `Groceries`). The `for (row = 1; row <= rows; row++)` loop runs once per row. Inside: `month = (row % 12) + 1` and `day = (row % 28) + 1` use the remainder (`%`) to cycle through valid months 1 to 12 and days 1 to 28 (28 is safe for every month); `category = categories[(row % 7) + 1]` cycles through the seven category names. `if (row % 1000 == 0) { printf ...; continue }` handles every thousandth row: it prints a bad row with the amount `oops` and skips to the next round (`continue`). Otherwise `printf "2026-%02d-%02d,Shop %d,%d.%02d,USD,%s\n", ...` prints a good row: `%02d` is a whole number padded with zeros to two digits, `%d` a plain whole number, `%s` text, `\n` a line break; the amount is `(row % 500) + 1` whole dollars and `row % 100` cents. The closing `}'` ends the awk program and the single quote that held it. `> "$output"` (**redirection**) writes everything awk printed into the output file.

> New words:
>
> - **awk**: a small programming language for text, one line at a time. A program is a list of `pattern { action }` pairs. Each input line is split into fields `$1`, `$2`, ... at white space; `$0` is the whole line, `NF` the number of fields. `-v name=value` sets a variable before it runs. A `BEGIN { ... }` block runs once, before any input. `print` writes its arguments; `printf format, args` writes with a format; `getline` reads the next line into `$0`.
> - **redirection**: `>file` sends what a command prints to a file; `>>file` appends; `<file` feeds a file to a command; `2>&1` sends the error stream to the same place as the normal output; `>&2` sends a message to the error stream (standard error); `>/dev/null` throws output away; `2>/dev/null` throws away only errors.

```bash
echo "wrote $rows rows to $output ($(wc -c < "$output" | tr -d ' ') bytes)" >&2
```

A summary for the person running the script, sent to standard error (`>&2`) so it does not mix with data. `wc -c < "$output"` (**wc**) counts the bytes of the file; `tr -d ' '` (**tr**) removes the spaces that macOS's `wc` puts in front of the number.

> New words:
>
> - **wc**: counts: `wc -c` the bytes, `wc -l` the lines. `wc -c < file` reads from the file so no file name is printed.
> - **tr**: translates or deletes characters: `tr -d ' '` deletes every space; `tr -d '[:space:]'` deletes all white space.

### `scripts/prove-quote-test.sh`

```bash
#!/usr/bin/env bash
```

The shell script's first line starts it with bash.

```bash
# The lesson's step 6 check: with the quote handling deleted, exactly one of the five parser tests
# must fail. Works on a temporary copy, so the working tree is never touched.
```

The comment says what is proved: when the quote handling is deleted from the CSV splitter, exactly one of the five parser tests must fail (the test about the quoted "1,204.50"). The check works on a temporary copy, so the real code is never touched.

```bash
set -euo pipefail
```

Strict mode (set -e -u -f -o pipefail).

```bash
root="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d)"
```

`root` is the repository's folder: the script's own folder (`dirname "$0"`) plus `/..` (one level up), made absolute by `cd` followed by `pwd` (command substitution). `work` is a new temporary folder from **mktemp**.

> New words:
>
> - **mktemp**: creates a new empty file (or, with `-d`, folder) with a unique name in the temporary area and prints its name.

```bash
trap 'rm -rf "$work"' EXIT
```

**trap**: whenever the script ends (`EXIT`), delete the temporary folder (`rm -rf`). The single quotes keep `$work` from being replaced now; it is replaced when the trap runs.

> New words:
>
> - **trap**: `trap command SIGNALS` registers a command to run when the script gets one of those signals or ends. `EXIT` means "whenever the script ends"; `INT` is Ctrl+C; `TERM` is the polite "please stop" signal; `HUP` is "terminal closed". It is how a script cleans up its temporary files even when it is interrupted.

```bash
rsync -a --exclude target --exclude .git "$root"/ "$work"/
```

Copies the whole repository into the temporary folder with **rsync**, leaving out `target` (build output) and `.git`. The trailing slashes mean "the contents of".

> New words:
>
> - **rsync**: copies files; `rsync -a --exclude target --exclude .git SRC/ DEST/` copies a whole tree, preserving permissions, leaving out the named folders.

```bash
splitter="$work/src/main/java/com/plainround/spend/infrastructure/csv/CsvFieldSplitter.java"
```

Names the file to damage: the CSV splitter inside the copy.

```bash
# Replace the quote toggle with a no-op, i.e. every comma now splits.
```

The comment explains the mutation: replace the line that flips the "inside quotes" flag by one that keeps it false, so every comma splits a field.

```bash
sed -i.bak 's/insideQuotes = !insideQuotes;/insideQuotes = false;/' "$splitter"
```

**sed** edits the file in place (`-i.bak` keeps the original beside it as `CsvFieldSplitter.java.bak`; giving a suffix makes the command work on both macOS and Linux). The substitution `s/insideQuotes = !insideQuotes;/insideQuotes = false;/` replaces the toggle statement.

> New words:
>
> - **sed**: "stream editor": applies edits to each line of its input. `sed -n 's/PATTERN/REPLACEMENT/p'` means: do not print lines by default (`-n`), substitute the first match of the pattern by the replacement in each line (`s/.../.../`), and print the line if a substitution happened (`p`). Parentheses `\(...\)` capture text and `\1` pastes it in the replacement. `sed -n 2p` prints only line 2. `sed -E` uses extended patterns (plain parentheses).

```bash
cmp -s "$splitter" "$splitter.bak" && { echo "mutation did not apply" >&2; exit 1; }
```

A safety check: if the edited file is byte-for-byte the same as the backup (**cmp** succeeds), the substitution did not match anything, and the script reports `mutation did not apply` on standard error and stops. `&&` (**&& and ||**) runs the `{ ...; }` group (**command group { ...; }**) only in that case.

> New words:
>
> - **cmp**: compares two files byte by byte; `cmp -s` prints nothing and only sets the exit status (0 if identical).
> - **&& and ||**: `a && b` runs `b` only if `a` succeeded. `a || b` runs `b` only if `a` failed. A command *succeeds* when it exits with status 0 and fails with any other status. `a || :` means "run a, and if it fails do nothing" (`:` is a command that does nothing and succeeds).
> - **command group { ...; }**: braces group several commands so that their combined output can be piped or redirected as one: `{ echo a; echo b; } | tee file`. The commands run in the current shell; the closing `}` must follow a `;` or a newline.

```bash
cd "$work"
```

Moves into the damaged copy.

```bash
set +e
```

Temporarily switches off "stop at the first failure" (set -e -u -f -o pipefail), because the test run is *expected* to fail.

```bash
./mvnw -q -B test -Dtest=CsvRowParserTest -Djacoco.skip=true -Dspotless.check.skip=true >out.log 2>&1
```

Runs only the parser test class in the copy (see `maven.md` for each option) and sends both output streams into `out.log` (`>out.log 2>&1`, redirection).

```bash
set -e
```

Switches strict mode back on.

```bash
failed="$(grep -E 'Tests run:.*Failures' out.log | tail -1)"
echo "$failed"
```

Takes the summary line from the log: **grep** keeps lines matching `Tests run:.*Failures`, and `tail -1` (**head and tail**) keeps the last one (the total). It is printed with `echo`.

> New words:
>
> - **grep**: prints the lines of its input that match a pattern. `-E` uses extended regular expressions (`a|b` for alternatives), `-q` prints nothing and only sets the exit status, `-c` prints the number of matching lines, `-h` omits file names, `-v` inverts. Exit status 0 means at least one line matched, 1 means none.
> - **head and tail**: `head -5` prints the first 5 lines; `tail -6` the last 6; `head -1` the first line.

```bash
grep -E '^\[ERROR\]   CsvRowParserTest\.' out.log || true
```

Prints the lines that name the failing test (they begin `[ERROR]   CsvRowParserTest.`). `|| true` makes a missing match harmless under strict mode.

```bash
echo "$failed" | grep -q 'Tests run: 5, Failures: 1' && echo "OK: exactly one of the five parser tests fails" \
  || { echo "UNEXPECTED: see $work/out.log" >&2; trap - EXIT; exit 1; }
```

The verdict: if the summary contains `Tests run: 5, Failures: 1`, print `OK: exactly one of the five parser tests fails`. Otherwise (`||` after the line continuation `\`) print `UNEXPECTED`, `trap - EXIT` cancels the cleanup so the temporary folder (with `out.log`) is kept for inspection, and exit with status 1.

## 6. Try it yourself

- `bash -n scripts/*.sh`. Expected: no output. `-n` reads the scripts and checks their syntax without running anything.
- `head -3 scripts/generate-big-csv.sh`. Expected: the shebang line and the usage comment.
- `ls -l scripts/`. Expected: `-rwx` at the start of each line (executable).
- `bash -x scripts/generate-big-csv.sh 5 /tmp/five.csv 2>&1 | head` (a safe one to try; for the lab scripts read the README first). Expected: each command printed with a leading `+` before it runs.

## 7. Mistakes people make

- Forgetting to quote a variable: `rm -rf $dir/*` with an empty `dir` would delete from the root. Scripts here quote everything and `set -u` stops on empty variables.
- Relying on a command's exit status inside a pipe without `pipefail`.
- Using `[[ ]]` or associative arrays, which bash 3.2 lacks or treats differently.
- `sed -i` without a suffix: fails on macOS. Here `-i.bak` is used.
- Sleeping a fixed time and hoping: the scripts wait for the health endpoint instead, with a limit.

## 8. Check yourself

1. What does `set -euo pipefail` do?
   **Answer:** Stops at the first failing command, treats unset variables as errors, and fails a pipe if any command in it fails.
2. Why write `"$name"` with quotes?
   **Answer:** So a value containing spaces stays one word.
3. What does `cmd || true` mean under `set -e`?
   **Answer:** Run `cmd`; if it fails, carry on instead of stopping.
4. What does `trap '...' EXIT` do?
   **Answer:** Runs the given command whenever the script ends, however it ends, so cleanup always happens.
5. What are standard output and standard error?
   **Answer:** Two separate output streams of a process: results go to the first, messages for people to the second.

## 9. Further reading

- Bash reference manual: https://www.gnu.org/software/bash/manual/bash.html
- POSIX shell: https://pubs.opengroup.org/onlinepubs/9699919799/utilities/V3_chap02.html
- ShellCheck (a linter for scripts): https://www.shellcheck.net/
