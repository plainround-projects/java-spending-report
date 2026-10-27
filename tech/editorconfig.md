# EditorConfig

## 1. What it is

EditorConfig is a tiny file format, `.editorconfig`, that tells code editors how to treat the files of a folder: character encoding, line endings, indentation, trailing whitespace. Most editors read it (some through a plugin).

## 2. Why this repository uses it

Different people use different editors. One shared file keeps indentation (2 spaces), line endings (`\n`) and the final newline the same for everyone, so diffs stay small. The formatter (Spotless) enforces the same layout for Java files.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `.editorconfig` | the settings |

## 4. How it works underneath

- The editor looks for `.editorconfig` in the file's folder and in each parent folder, up to one that says `root = true`.
- Sections in `[...]` choose files by glob pattern; for each key the last matching section wins.
- The file affects what the editor writes from now on; it does not rewrite existing files.

## 5. Walkthrough of this repository's use

```
root = true
```

```
[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true
indent_style = space
indent_size = 2
```

`root = true` stops the search here. `[*]` matches every file: `charset = utf-8`; `end_of_line = lf` (the Unix line ending, a single newline character); `insert_final_newline = true` (a file ends with a newline); `trim_trailing_whitespace = true` (remove spaces at the end of lines); `indent_style = space` and `indent_size = 2`.

```
[Makefile]
indent_style = tab
```

Make **requires** a tab at the start of every recipe line, so the Makefile overrides the space rule.

```
[*.md]
trim_trailing_whitespace = false
```

In Markdown, two spaces at the end of a line can mean a line break, so trimming is switched off there.

## 6. Try it yourself

- `cat .editorconfig`. Expected: the settings above.
- Open a Java file in an editor with EditorConfig support and press Tab. Expected: two spaces are inserted.
- `grep -rln $'\t' src/main/java | head -3`. Expected: no output (no tab characters in Java files).

## 7. Mistakes people make

- Expecting the file to fix existing files. It guides new edits; `make format` fixes Java layout.
- Replacing the tab in a Makefile recipe with spaces: `*** missing separator`.
- Forgetting `root = true` and inheriting settings from a parent folder.

## 8. Check yourself

1. What does `root = true` do?
   **Answer:** It stops the editor from reading `.editorconfig` files in parent folders.
2. Why does `[Makefile]` use tabs?
   **Answer:** Make requires a tab at the start of recipe lines.
3. Does EditorConfig reformat existing code?
   **Answer:** No. It only guides what the editor writes.
4. Which line ending is set?
   **Answer:** `lf`, a single newline.
5. Why is trailing-space trimming off for Markdown?
   **Answer:** Two trailing spaces can mean a line break there.

## 9. Further reading

- EditorConfig specification: https://spec.editorconfig.org/
- Supported editors and plugins: https://editorconfig.org/
