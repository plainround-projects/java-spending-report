# Markdown and Mermaid

## 1. What it is

**Markdown** is a text format where simple marks give structure: `#` for headings, `-` for bullets, backticks for `code`, `|` for tables, triple backticks for code blocks. GitHub shows it formatted. **Mermaid** is a text language for diagrams; inside a Markdown code block marked `mermaid`, GitHub draws it as a picture.

## 2. Why this repository uses it

The deliverable is documentation as much as code: a README that tells a reader how to run and verify, an ARCHITECTURE file with a data-flow diagram, and short decision records. Plain text in Git means they are reviewed and versioned like code.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `README.md` | purpose, commands, Done-when proof, project tree |
| `ARCHITECTURE.md` | components, the Mermaid data-flow diagram, decisions, failure modes |
| `docs/adr/*.md` | decision records: context, decision, alternatives, consequences |
| `CHANGELOG.md` | release notes |
| `tech/*.md` | these notes |

## 4. How it works underneath

- Markdown is converted to HTML by the viewer (GitHub, an editor preview); the file itself stays readable as plain text.
- A blank line separates paragraphs. A line starting `|` begins a table; the second line (`| --- | --- |`) separates the header from the rows.
- A fenced code block starts and ends with three backticks; the word after the opening ones names the language for colors, or `mermaid` for a diagram.
- Mermaid reads the block and draws it in the browser. `flowchart LR` means a left-to-right flow diagram; `A["text"] -->|"label"| B` draws an arrow with a label.

## 5. Walkthrough of this repository's use

```mermaid
flowchart LR
  file["CSV file<br/>CsvFileRowSource"] -->|"line + line number"| parse["Parse a row<br/>CsvRowParser"]
  parse -->|"Ok(Transaction)"| agg["Aggregate<br/>SpendingAccumulator"]
  parse -->|"Rejected(line, reason)"| agg
  agg -->|"SpendingReport"| report["Report<br/>ReportPrinter"]
  report -->|"text"| out(["stdout"])
  report -->|"exit status 0 / 2"| shell(["shell"])
  file -. "unreadable file" .-> cli["SpendCli"]
  cli -->|"exit status 1"| shell
```

`flowchart LR` starts a left-to-right diagram. Each `id["text"]` defines a box (a `<br/>` inside text makes a line break); `a --> b` draws an arrow; `a -->|"label"| b` labels it; `a -.-> b` draws a dotted arrow; `(["text"])` draws a rounded shape. The box names are the lesson diagram's component names, so the picture and the Components table in the same file agree.

## 6. Try it yourself

- Open `README.md` in a Markdown preview or on GitHub. Expected: formatted headings and tables.
- Paste the Mermaid block into https://mermaid.live. Expected: the same diagram.
- `grep -c '^## ' README.md`. Expected: the number of top-level sections.
- `ls docs/adr`. Expected: numbered decision records.

## 7. Mistakes people make

- Forgetting the blank line before a table or list: it renders as plain text.
- A pipe `|` inside a table cell breaks the table; escape it or reword.
- A Mermaid label with special characters unquoted: quote it with `"..."`.
- Writing docs that drift from the code. The README's commands were run from a fresh clone; the Components table uses the same names as the diagram.

## 8. Check yourself

1. What does a line `## Title` make?
   **Answer:** A second-level heading.
2. How do you mark a code block as Mermaid?
   **Answer:** Start the fence with three backticks followed by `mermaid`.
3. What separates a table's header from its rows?
   **Answer:** A line like `| --- | --- |`.
4. What does `-->|"x"|` draw?
   **Answer:** An arrow with the label x.
5. What is an ADR?
   **Answer:** A short architecture decision record: context, decision, alternatives, consequences.

## 9. Further reading

- CommonMark specification: https://spec.commonmark.org/
- GitHub Flavored Markdown (tables, fences): https://github.github.com/gfm/
- Mermaid flowcharts: https://mermaid.js.org/syntax/flowchart.html
