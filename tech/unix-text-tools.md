# Command-line text tools (grep, sed, awk, sort, curl and friends)

## 1. What it is

Unix systems come with many small programs that each do one thing to text: find lines, change them, count them, sort them. They read **standard input**, write **standard output**, and are chained with pipes (`|`). Together they act as a flexible toolbox for turning one program's output into another's input.

## 2. Why this repository uses it

The lab and the build targets need to pull one number out of a log or a report and compare it. A pipeline of two or three of these tools does that in one line, without writing a program. They are also what `make help` uses to list the targets.

## 3. Where it appears

| Tool | Where it first appears |
| --- | --- |
| `grep` | `Makefile` |
| `sed` | `Makefile` |
| `awk` | `scripts/generate-big-csv.sh` |
| `sort` | `Makefile` |
| `tail` | `scripts/prove-quote-test.sh` |
| `wc` | `scripts/generate-big-csv.sh` |
| `tr` | `scripts/generate-big-csv.sh` |
| `rsync` | `scripts/prove-quote-test.sh` |
| `cmp` | `scripts/prove-quote-test.sh` |
| `mktemp` | `scripts/prove-quote-test.sh` |

## 4. How it works underneath

- Every one of these is a separate program started by the shell as a process; none is part of Java.
- They are line-oriented: input is split at newlines, and most tools handle one line at a time, so they work on huge inputs with little memory.
- Patterns are **regular expressions**: a mini-language for text shapes. `.` is any character, `*` zero or more of the previous, `[0-9]` a digit, `^` the start of a line, `$` the end, `\(...\)` a group (basic form) or `(...)` (extended form, with `-E`).
- macOS ships the BSD versions of these tools; GNU versions on Linux differ in some options. The scripts here use only options that work on both.

## 5. Walkthrough of this repository's use

### `grep`

First use in `Makefile`:

```bash
@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | sed -E 's/:.*## /\t/' | sort
```

Prints the lines of its input that match a pattern. `-E` uses extended regular expressions (`a|b`, `[0-9]+`), `-c` counts matching lines, `-q` prints nothing and only sets the exit status, `-h` hides file names, `-v` inverts. Exit status 1 means "no match".

### `sed`

First use in `Makefile`:

```bash
@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | sed -E 's/:.*## /\t/' | sort
```

A stream editor: reads lines, applies edits, prints the result. `s/old/new/` substitutes; `-n` with the `p` command prints only chosen lines; `-E` extended regular expressions; `-i.bak` edits a file in place and keeps a backup (the form that works on both macOS and Linux).

### `awk`

First use in `scripts/generate-big-csv.sh`:

```bash
awk -v rows="$rows" 'BEGIN {
```

A tiny language for columns of text. A program is `pattern { action }`; `$1`, `$2` are the first and second columns, `$0` the whole line, `-v name=value` sets a variable, `BEGIN { }` runs before input. `printf` formats.

### `sort`

First use in `Makefile`:

```bash
@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | sed -E 's/:.*## /\t/' | sort
```

Sorts lines. `-n` sorts as numbers, `-rn` numbers in reverse.

### `tail`

First use in `scripts/prove-quote-test.sh`:

```bash
failed="$(grep -E 'Tests run:.*Failures' out.log | tail -1)"
```

Prints the last lines (`-6`, `-1`).

### `wc`

First use in `scripts/generate-big-csv.sh`:

```bash
echo "wrote $rows rows to $output ($(wc -c < "$output" | tr -d ' ') bytes)" >&2
```

Counts lines, words or bytes (`-c` bytes, `-l` lines).

### `tr`

First use in `scripts/generate-big-csv.sh`:

```bash
echo "wrote $rows rows to $output ($(wc -c < "$output" | tr -d ' ') bytes)" >&2
```

Translates or deletes characters (`tr -d ' '` deletes spaces).

### `rsync`

First use in `scripts/prove-quote-test.sh`:

```bash
rsync -a --exclude target --exclude .git "$root"/ "$work"/
```

Copies folders efficiently. `-a` archive mode (keeps permissions and times), `--exclude` skips paths.

### `cmp`

First use in `scripts/prove-quote-test.sh`:

```bash
cmp -s "$splitter" "$splitter.bak" && { echo "mutation did not apply" >&2; exit 1; }
```

Compares two files byte by byte; `-s` is silent and only sets the exit status (0 if equal).

### `mktemp`

First use in `scripts/prove-quote-test.sh`:

```bash
work="$(mktemp -d)"
```

Creates a new unique temporary file or (`-d`) folder and prints its path.

## 6. Try it yourself

- `printf 'b\na\nb\n' | sort | uniq -c`. Expected: `   1 a` and `   2 b`.
- `echo 'cache=1476' | sed -n 's/.*cache=\([0-9]*\).*/\1/p'`. Expected: `1476`.
- `printf 'a 1\nb 2\n' | awk '{print $2, $1}'`. Expected: `1 a` and `2 b`.
- `make help`. Expected: the target list, produced by `grep`, `sed` and `sort`.
- `grep -c 'Tests run' target/surefire-reports/*.txt | head -3`. Expected: file names with counts.

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
