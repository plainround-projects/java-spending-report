# CSV (comma-separated values)

## 1. What it is

CSV is a plain-text table format. Each line is a **row**; the values (**fields**) in a row are separated by commas. The first line is usually a **header** that names the columns. There is no official single standard in practice; RFC 4180 describes the common form: a field that contains a comma, a double quote or a line break is wrapped in double quotes.

## 2. Why this repository uses it

The lesson's input is a card-transaction export. Its messiness is the lesson: a quoted amount `"1,204.50"` that contains a comma, a row with too few columns, a missing field. Reading CSV correctly, and reporting the bad rows, is the project.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `sample/transactions.csv` | the lesson's 25 rows, 10 of them bad on purpose |
| `sample/cents.csv`, `sample/bom-and-accent.csv` | two small inputs for `make cents` and `make bom` |
| `sample/expected-report.txt` | the exact report the sample must produce |
| `scripts/generate-big-csv.sh` | writes a 3-million-row CSV for the streaming check |
| `src/main/java/com/plainround/spend/infrastructure/csv/*.java` | the reader, the splitter and the row parser |

## 4. How it works underneath

- A CSV file is bytes. They must be decoded with a **character encoding**; this tool reads UTF-8. Some programs add a **byte-order mark** (BOM, the invisible character U+FEFF) at the start of a UTF-8 file; the reader removes it.
- A line ends with `\n` or `\r\n`; Java's `Files.lines` handles both.
- Splitting on every comma is wrong whenever a field is quoted: `2026-01-05,Rail Pass,"1,204.50",USD,Travel` has five fields, not six. The splitter keeps a flag "inside quotes" and only splits on commas when the flag is off.
- Everything in a CSV is text. `42.17` is the characters `4`, `2`, `.`, `1`, `7`; the program decides it is an amount, builds a `BigDecimal` from it, and validates it.

## 5. Walkthrough of this repository's use

```csv
date,description,amount,currency,category
2026-01-03,Corner Grocer,42.17,USD,Groceries
2026-01-04,Coffee Cart,4.50,USD,Eating Out
2026-01-05,Rail Pass,"1,204.50",USD,Travel
2026-01-07,Book Shop,18.99,USD,Books
```

Line 1 is the header. Line 4 is a **good** row even though it looks odd: its amount is quoted because of the thousands comma.

```csv
2026-02-05,Rail Pass,1,204.50,USD,Travel
```

The same amount **unquoted** splits into six fields, so this row is rejected with `expected 5 columns, found 6`.

```csv
2026-02-02,Utility Bill,89.10,USD
```

Only four fields: the category is missing.

```java
  /**
   * Splits a line into trimmed fields; quote characters are removed from the result.
   *
   * @param line one line of CSV text
   * @return the fields, never empty (an empty line is one empty field)
   */
  static List<String> split(String line) {
    List<String> fields = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean insideQuotes = false;
    for (char symbol : line.toCharArray()) {
      if (symbol == '"') {
        insideQuotes = !insideQuotes;
      } else if (symbol == ',' && !insideQuotes) {
        fields.add(current.toString().trim());
        current.setLength(0);
      } else {
        current.append(symbol);
      }
    }
    fields.add(current.toString().trim());
    return fields;
  }
```

The splitter walks the line one character at a time. A `"` flips `insideQuotes` and is not copied. A `,` outside quotes ends the field (`trim()` removes surrounding spaces). Anything else is appended to the current field. After the loop the last field is added. The quote characters are removed from the result, so `"1,204.50"` becomes the field `1,204.50`.

The file reader skips line 1 and counts lines from 1, so a rejected row can say `line 18`:

```java
  private void emit(Iterator<String> lines, Consumer<ParsedRow> consumer) {
    int lineNumber = 0;
    while (lines.hasNext()) {
      String line = lines.next().replace(BYTE_ORDER_MARK, "");
      lineNumber++;
      if (lineNumber > 1 && !line.isBlank()) {
        consumer.accept(parser.parse(lineNumber, line));
      }
    }
  }
```

## 6. Try it yourself

- `head -5 sample/transactions.csv`. Expected: the header and four rows; the third data row has `"1,204.50"`.
- `awk -F, '{print NF}' sample/transactions.csv | sort | uniq -c`. Expected: counts of lines with 5, 4, 6 fields (a naive split by comma shows why the quoted row and the unquoted row differ).
- `make bom`. Expected: `1 rows read, 0 rejected` and `Café Noir`.
- `xxd sample/bom-and-accent.csv | head -2`. Expected: the first bytes `ef bb bf` (the BOM) before `date,description`.
- `make run`. Expected: the lesson report with the ten rejected lines.

## 7. Mistakes people make

- `line.split(",")`: it breaks on quoted commas. This is the first bug of the lesson.
- Forgetting the encoding: the same bytes read with a different charset show `Ã©` instead of `é`. The reader names UTF-8.
- Leaving the BOM in: it sticks to the first header name and breaks a header comparison. The reader removes it.
- Treating blank lines as rows. They are skipped (but still counted in line numbers).
- Assuming CSV supports every case: this splitter does not handle an escaped quote (`""`) or a line break inside a quoted field; the README lists that limit.

## 8. Check yourself

1. How many fields does `a,"b,c",d` have?
   **Answer:** Three: `a`, `b,c` and `d`.
2. What is a byte-order mark?
   **Answer:** An invisible character at the start of some UTF-8 files; the reader removes it.
3. Why does the unquoted `1,204.50` give `expected 5 columns, found 6`?
   **Answer:** The comma inside the amount splits it into two fields.
4. What is the first line of the file?
   **Answer:** The header naming the columns; the reader skips it but counts it as line 1.
5. What limits of this CSV reader does the README name?
   **Answer:** No escaped quotes (`""`) and no line breaks inside a field.

## 9. Further reading

- RFC 4180 (Common Format for CSV): https://www.rfc-editor.org/rfc/rfc4180
- `java.nio.file.Files.lines` (Java 25): https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html#lines(java.nio.file.Path,java.nio.charset.Charset)
