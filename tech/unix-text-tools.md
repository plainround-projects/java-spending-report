# Command-line text tools (grep, sed, awk, sort, curl and friends)

## 1. What it is

Unix systems come with many small programs that each do one thing to text: find lines, change them, count them, sort them. They read **standard input** (what is fed to them), write **standard output** (what they print), and are chained with pipes (`|`). Together they act as a toolbox for turning one program's output into another's input.

## 2. Why this repository uses it

The lab and the build targets need to pull one number out of a log or a report and compare it. A pipeline of two or three of these tools does that in one line, without writing a program. They are also what `make help` uses to list the targets.

## 3. Where it appears

| Tool | What it does | Where it is used |
| --- | --- | --- |
| `grep` | finds lines that match a pattern | `scripts/prove-quote-test.sh`, `Makefile` |
| `sed` | edits text streams | `scripts/prove-quote-test.sh`, `Makefile` |
| `awk` | a small programming language for lines and columns | `scripts/generate-big-csv.sh` |
| `tail` | prints the last lines | `scripts/prove-quote-test.sh` |
| `tr` | translates or deletes characters | `scripts/generate-big-csv.sh` |
| `wc` | counts bytes or lines | `scripts/generate-big-csv.sh` |
| `date` | prints the time (here as seconds since 1970) | `scripts/generate-big-csv.sh` |
| `mktemp` | creates a uniquely named temporary file or folder | `scripts/prove-quote-test.sh` |
| `rsync` | copies a tree of files, with exclusions | `scripts/prove-quote-test.sh` |
| `cmp` | compares two files byte by byte | `scripts/prove-quote-test.sh` |

## 4. How it works underneath

1. Every one of these is a separate program started by the shell as a process; none is part of Java.
2. They are line-oriented: input is split at line breaks, and most tools handle one line at a time, so they work on huge inputs with little memory.
3. Patterns are **regular expressions**: a mini-language for text shapes. `.` is any character, `*` zero or more of the previous, `[0-9]` one digit, `^` the start of a line, `$` the end, `\(...\)` a captured group (basic form) or `(...)` (extended form, with `-E`).
4. macOS ships the BSD versions of these tools; Linux has the GNU versions, which differ in some options. The scripts here use only options that work on both.

## 5. Walkthrough of this repository's use

Every tool that the Makefile and the scripts use is described below, with the options this repository uses. How each one is used on each line is explained in `bash.md` and `make.md`.

### `grep`

Used in: `scripts/prove-quote-test.sh`, `Makefile`.

**grep**

> New words:
>
> - **grep**: prints the lines of its input that match a pattern. `-E` uses extended regular expressions (`a|b` for alternatives), `-q` prints nothing and only sets the exit status, `-c` prints the number of matching lines, `-h` omits file names, `-v` inverts. Exit status 0 means at least one line matched, 1 means none.

### `sed`

Used in: `scripts/prove-quote-test.sh`, `Makefile`.

**sed**

> New words:
>
> - **sed**: "stream editor": applies edits to each line of its input. `sed -n 's/PATTERN/REPLACEMENT/p'` means: do not print lines by default (`-n`), substitute the first match of the pattern by the replacement in each line (`s/.../.../`), and print the line if a substitution happened (`p`). Parentheses `\(...\)` capture text and `\1` pastes it in the replacement. `sed -n 2p` prints only line 2. `sed -E` uses extended patterns (plain parentheses).

### `awk`

Used in: `scripts/generate-big-csv.sh`.

**awk**

> New words:
>
> - **awk**: a small programming language for text, one line at a time. A program is a list of `pattern { action }` pairs. Each input line is split into fields `$1`, `$2`, ... at white space; `$0` is the whole line, `NF` the number of fields. `-v name=value` sets a variable before it runs. A `BEGIN { ... }` block runs once, before any input. `print` writes its arguments; `printf format, args` writes with a format; `getline` reads the next line into `$0`.

### `tail`

Used in: `scripts/prove-quote-test.sh`.

**head and tail**

> New words:
>
> - **head and tail**: `head -5` prints the first 5 lines; `tail -6` the last 6; `head -1` the first line.

### `tr`

Used in: `scripts/generate-big-csv.sh`.

**tr**

> New words:
>
> - **tr**: translates or deletes characters: `tr -d ' '` deletes every space; `tr -d '[:space:]'` deletes all white space.

### `wc`

Used in: `scripts/generate-big-csv.sh`.

**wc**

> New words:
>
> - **wc**: counts: `wc -c` the bytes, `wc -l` the lines. `wc -c < file` reads from the file so no file name is printed.

### `date`

Used in: `scripts/generate-big-csv.sh`.

**date**

> New words:
>
> - **date**: prints the date and time; `date +%s` prints the number of seconds since 1 January 1970, handy for measuring how long something took.

### `mktemp`

Used in: `scripts/prove-quote-test.sh`.

**mktemp**

> New words:
>
> - **mktemp**: creates a new empty file (or, with `-d`, folder) with a unique name in the temporary area and prints its name.

### `rsync`

Used in: `scripts/prove-quote-test.sh`.

**rsync**

> New words:
>
> - **rsync**: copies files; `rsync -a --exclude target --exclude .git SRC/ DEST/` copies a whole tree, preserving permissions, leaving out the named folders.

### `cmp`

Used in: `scripts/prove-quote-test.sh`.

**cmp**

> New words:
>
> - **cmp**: compares two files byte by byte; `cmp -s` prints nothing and only sets the exit status (0 if identical).

## 6. Try it yourself

- `printf 'b\na\nb\n' | sort | uniq -c`. Expected: `   1 a` and `   2 b`.
- `echo 'cache=1476' | sed -n 's/.*cache=\([0-9]*\).*/\1/p'`. Expected: `1476`.
- `printf 'a 1\nb 2\n' | awk '{print $2, $1}'`. Expected: `1 a` and `2 b`.
- `make help`. Expected: the target list, produced by `grep`, `sed` and `sort`.
- `printf 'one\ntwo\n' | tee /tmp/two.txt | wc -l`. Expected: `2`, and the file `/tmp/two.txt` holds both lines.

## 7. Mistakes people make

- Using `grep` where `awk` is needed (columns), or the reverse.
- Forgetting `sort` before `uniq -c` or `join`: both need sorted input.
- Writing a regular expression that matches more than intended; the scripts anchor with `^` and use specific words.
- Relying on GNU-only options on macOS (`sed -i` without a suffix fails on macOS; `sed -i.bak` works on both).
- Parsing numbers out of text that may change format; the scripts check their results.

## 8. Check yourself

1. What does `|` do?
   **Answer:** Connects the output of the command on the left to the input of the command on the right.
2. Why `sort` before `uniq -c`?
   **Answer:** `uniq` only collapses neighboring duplicates.
3. What does `sed -n 's/x/y/p'` print?
   **Answer:** Only the lines where the substitution happened, after substituting.
4. What does `awk '{print $3}'` print?
   **Answer:** The third whitespace-separated column of each line.
5. What does `grep -q` change?
   **Answer:** It prints nothing and only sets the exit status: 0 if a line matched, 1 if not.

## 9. Further reading

- POSIX utilities: https://pubs.opengroup.org/onlinepubs/9699919799/utilities/contents.html
- GNU grep: https://www.gnu.org/software/grep/manual/ ; GNU sed: https://www.gnu.org/software/sed/manual/ ; GNU awk: https://www.gnu.org/software/gawk/manual/ (macOS uses the BSD versions with the same core behavior)
- curl: https://curl.se/docs/manpage.html
