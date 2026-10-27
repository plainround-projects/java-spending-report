# EditorConfig

## 1. What it is

People edit the same files in different editors (IntelliJ, VS Code, vim). Each has its own idea of how big an indent is and how a line ends, and small differences create noisy changes in the history. **EditorConfig** is a tiny standard: you put a file named `.editorconfig` in the project, and any editor that supports it (most do, some through a plugin) reads it and follows the rules.

## 2. Why this repository uses it

The repository wants Unix line endings, UTF-8, a final newline and two-space indents everywhere, and real tabs in the Makefile. The formatter and the Checkstyle rules enforce the Java part; EditorConfig makes the editor produce the right bytes for every other file type before any tool complains.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `.editorconfig` | the rules, walked through below |
| `config/checkstyle.xml` | rejects tabs and trailing spaces in Java files |
| `Makefile` | needs the tab setting |

## 4. How it works underneath

1. The editor looks for `.editorconfig` in the folder of the file being edited and in each parent folder, up to one that says `root = true`.
2. Sections are matched against the file name with glob patterns (`*` any text, `{a,b}` alternatives); later sections override earlier ones.
3. Settings are applied when the file is saved or typed (indentation); EditorConfig never rewrites other files by itself.

## 5. Walkthrough of this repository's use

```ini
root = true
```

`root = true` says this is the top-level EditorConfig file: editors should stop looking in parent folders. A line `[pattern]` opens a **section** that applies to the files matching it; the lines below it are settings.

```ini
[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true
indent_style = space
indent_size = 2
```

The section `[*]` applies to every file. `charset = utf-8` files are UTF-8 text. `end_of_line = lf` lines end with the Unix line break (not the Windows `crlf`). `insert_final_newline = true` every file ends with a line break. `trim_trailing_whitespace = true` spaces at the end of a line are removed on saving. `indent_style = space` indent with spaces, not tabs. `indent_size = 2` an indent is two spaces. These match what google-java-format produces, so an editor does not fight the formatter.

```ini
[*.{xml,yml,yaml,json,md}]
indent_size = 2
```

The braces list several extensions: the same two-space indent for XML, YAML, JSON and Markdown files (a repeat of the default, kept explicit so the rule survives if the default changes).

```ini
[Makefile]
indent_style = tab
```

A Makefile needs real TAB characters at the start of recipe lines, so this section overrides the default: `indent_style = tab`.

```ini
[*.md]
trim_trailing_whitespace = false
```

In Markdown two trailing spaces mean a forced line break, so trimming trailing whitespace is switched off for `.md` files.

## 6. Try it yourself

- `cat .editorconfig`. Expected: the file shown above.
- Open a Java file in an editor with EditorConfig support, press Tab. Expected: two spaces.
- `grep -rl "$(printf '\t')" src/main/java`. Expected: no output, meaning no Java file contains a tab character (the Checkstyle rule `FileTabCharacter` enforces it).

## 7. Mistakes people make

- Relying on EditorConfig alone: it only guides editors; the CI formatter is the real gate.
- Editors without support ignore the file.
- Forgetting the Makefile section: an editor that converts tabs to spaces breaks the Makefile (`*** missing separator`).
- Trimming trailing spaces in Markdown: two trailing spaces mean a line break there, which is why `*.md` is excepted.

## 8. Check yourself

1. What does `root = true` do?
   **Answer:** Stops the editor from looking for more `.editorconfig` files in parent folders.
2. Why is `indent_style = tab` set for the Makefile?
   **Answer:** Make recipe lines must start with a tab character.
3. What does `end_of_line = lf` mean?
   **Answer:** Lines end with the Unix line break, not the Windows pair.
4. Why is trailing-whitespace trimming off for `*.md`?
   **Answer:** Two trailing spaces are a hard line break in Markdown.
5. Does EditorConfig format code?
   **Answer:** No. It only sets basic editor behavior; the formatter does the rest.

## 9. Further reading

- EditorConfig: https://editorconfig.org/
- Supported properties: https://spec.editorconfig.org/
