# YAML

## 1. What it is

YAML is a text format for nested data that uses **indentation** instead of brackets. It has *mappings* (`key: value`), *sequences* (lists, each item starting with `- `) and *scalars* (text, numbers, `true`/`false`). `#` starts a comment.

## 2. Why this repository uses it

GitHub Actions and Docker Compose both use YAML for their configuration.

## 3. Where it appears

| File | What it holds |
| --- | --- |
| `.github/workflows/ci.yml` | the CI workflow |

## 4. How it works underneath

- Indentation is structure: children are indented more than their parent, with **spaces** (tabs are not allowed). Two spaces per level is the habit here.
- `key: value` is a mapping entry. `key:` followed by indented lines nests another mapping or a list.
- `- item` starts a list entry. `- run: make lint` is a list item that is itself a mapping with the key `run`.
- Strings usually need no quotes; quote them when they contain `:`, start with `*`, `&` or `{`, or look like numbers/booleans you want as text (`"25"`, `"8080:8080"`).
- `on` could be read as the boolean true in old YAML versions; GitHub Actions treats the key `on` as a workflow trigger.

## 5. Walkthrough of this repository's use

```yaml
name: ci

on: [push, pull_request]

permissions:
  contents: read
```

`name: ci` is a mapping entry (string value). `on: [push, pull_request]` is a **flow-style** list, written in brackets on one line; the same list in block style would be two `- ` lines. `permissions:` holds a nested mapping: one key, `contents`, with value `read`.

## 6. Try it yourself

- `python3 -c "import yaml; print(yaml.safe_load(open('.github/workflows/ci.yml')))"` (needs PyYAML). Expected: a Python dictionary with the keys `name`, `on` (or `True`), `permissions`, `jobs`.
- `docker compose config` (when a compose file exists). Expected: the file normalized; an error message with the line if it is invalid.
- `grep -c $'\t' .github/workflows/ci.yml`. Expected: `0` (no tabs).

## 7. Mistakes people make

- Tabs for indentation: parse error.
- Wrong indentation: a key silently becomes a child of the wrong parent.
- Unquoted values that YAML reinterprets (`25` becomes a number; the workflow quotes `"25"`).
- Long lines with `: ` inside unquoted text.

## 8. Check yourself

1. How does YAML show that one key belongs inside another?
   **Answer:** By indenting it more with spaces.
2. How do you write a list?
   **Answer:** Lines starting with `- `, or `[a, b]` on one line.
3. Why is `"25"` quoted in the workflow?
   **Answer:** So it stays text for the Java version input and is not read as a number.
4. Are tabs allowed for indentation?
   **Answer:** No.
5. What does `- run: make lint` mean?
   **Answer:** A list item that is a mapping with the key `run` and the value `make lint`.

## 9. Further reading

- YAML 1.2.2 specification: https://yaml.org/spec/1.2.2/
- GitHub Actions workflow syntax: https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions
- Compose file reference: https://docs.docker.com/reference/compose-file/
