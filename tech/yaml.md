# YAML

## 1. What it is

**YAML** is a text format for structured data that uses **indentation** instead of brackets and tags. It is popular for configuration files because it is easy to read: a name, a colon and a value make a pair; a hyphen makes a list item; deeper indentation means "belongs to the line above". Programs read it into the same kind of tree as XML or JSON.

## 2. Why this repository uses it

GitHub Actions requires its workflows in YAML. The repository does not choose the format.

## 3. Where it appears

| File | What it is | Lines |
| --- | --- | --- |
| `.github/workflows/ci.yml` | the CI workflow | 37 |

## 4. How it works underneath

1. A parser reads the lines and works out the tree from the indentation. Two spaces more than the previous key means "child of it".
2. A line `key: value` is a pair. A line `- item` is a list item. A line `key:` with nothing after the colon means the value is on the following, more indented lines.
3. Scalars (single values) are text unless they look like a number, a boolean (`true`, `false`) or `null`; quotes force text.
4. Mistakes in indentation change the meaning without any bracket to warn you; many YAML errors are indentation errors.
5. YAML is a superset of JSON: a JSON document is also valid YAML.

## 5. Walkthrough of this repository's use

Every YAML feature that appears in this repository, with a real example:

### Mappings (key: value) and nesting by indentation

```yaml
permissions:
  contents: read
```

A **mapping** is a list of `key: value` pairs. A value that is itself a mapping is written on the next lines, **indented** more than its key (two spaces here). Indentation is the structure; tabs are not allowed.

### Sequences (lists)

```yaml
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
```

A **sequence** is a list; each item starts with `- ` at the same indentation. An item can itself be a mapping: `- uses: ...` then more keys indented to line up under `uses`.

### Flow sequences

```yaml
on: [push, pull_request]
```

Square brackets write a list on one line: `[push, pull_request]` is the same as two `- ` items.

### Quoted and unquoted text

```yaml
          java-version: "25"
```

Plain text needs no quotes. Quotes keep a value as text when it could be read as something else: `"25"` stays the text 25 and not the number.

### Comments

```yaml
name: ci
```

This file has no comment lines; a `#` would start one that runs to the end of the line.

The files themselves are explained line by line in `github-actions.md`.

## 6. Try it yourself

- `python3 -c 'import sys, yaml; print(yaml.safe_load(open(".github/workflows/ci.yml")))'` (needs `pip install pyyaml`). Expected: a Python dictionary: the YAML turned into a tree.
- `grep -n 'run:' .github/workflows/ci.yml`. Expected: the `make ...` steps.
- In a scratch copy, remove two spaces of indentation from one line and re-read the file. Expected: a parse error or a changed structure.

## 7. Mistakes people make

- Using tabs for indentation: not allowed.
- Misaligned list items.
- Unquoted `no`, `on`, `yes`: read as booleans.
- Putting a colon followed by a space inside plain text: it starts a pair; quote the text.
- Treating `${VAR}` as YAML: it belongs to the tool that reads the file.

## 8. Check yourself

1. What does indentation mean in YAML?
   **Answer:** A line indented more than the previous key belongs to that key.
2. How is a list item written?
   **Answer:** A line starting with `- `.
3. Why is `restart: "no"` quoted?
   **Answer:** Bare `no` would be read as the boolean false.
4. What is `[a, b]`?
   **Answer:** A list written on one line.
5. Is `${VAR:-x}` YAML?
   **Answer:** No. The tool that reads the file replaces it.

## 9. Further reading

- YAML specification: https://yaml.org/spec/1.2.2/
- Learn YAML in Y minutes: https://learnxinyminutes.com/yaml/
