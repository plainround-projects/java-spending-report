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

### `sample/transactions.csv`, line by line

```csv
date,description,amount,currency,category
```

Line 1 is the **header**: the five column names. The reader skips it but counts it as line 1, which is why the first data row is "line 2" in the report.

```csv
2026-01-03,Corner Grocer,42.17,USD,Groceries
```

Line 2, a good row: a date in the form `2026-01-03`, a description, the amount `42.17`, the currency `USD` and the category `Groceries`.

```csv
2026-01-04,Coffee Cart,4.50,USD,Eating Out
```

Line 3, good: amount `4.50`, category `Eating Out` (a category may contain spaces).

```csv
2026-01-05,Rail Pass,"1,204.50",USD,Travel
```

Line 4, good, and the first trap: the amount is written `"1,204.50"` in double quotes because it contains a comma. A splitter that cuts at every comma would see six fields; the quote-aware splitter sees five.

```csv
2026-01-07,Book Shop,18.99,USD,Books
```

Line 5, good: `18.99`, `Books`.

```csv
31/02/2026,Gym Fees,35.00,USD,Health
```

Line 6, **rejected**: the date is written day/month/year and 31 February does not exist. Reason: `date must look like 2026-01-31, got '31/02/2026'`.

```csv
2026-01-09,Pharmacy,12.40,USD,Health
```

Line 7, good: `12.40`, `Health`.

```csv
2026-01-11,Ride Share,-8.00,USD,Travel
```

Line 8, **rejected**: a negative amount (`-8.00`). Reason: `amount must not be null or negative: -8.00`.

```csv
2026-01-12,Corner Grocer,55.03,USD,Groceries
```

Line 9, good: `55.03`, `Groceries`.

```csv
2026-01-14,Streaming Plan,15.99,USD,
```

Line 10, **rejected**: five fields, but the last (the category) is empty, so the row ends with a comma. Reason: `category must not be blank`.

```csv
2026-01-15,Hotel Stay,310.00,EUR,Travel
```

Line 11, **rejected**: the currency is `EUR` but the report is in `USD`. Reason: `currency EUR is not the report currency USD`.

```csv
2026-01-16,Cinema,24.00,USD,Fun
```

Line 12, good: `24.00`, `Fun`.

```csv
2026-01-18,Hardware Store,sixty,USD,Home
```

Line 13, **rejected**: the amount is the word `sixty`. Reason: `amount is not a number: 'sixty'`.

```csv
2026-01-20,Corner Grocer,48.60,USD,Groceries
```

Line 14, good: `48.60`. With lines 2 and 9, January groceries are 42.17 + 55.03 + 48.60 = 145.80.

```csv
2026-02-01,Coffee Cart,4.50,USD,Eating Out
```

Line 15, good: the first February row.

```csv
2026-02-02,Utility Bill,89.10,USD
```

Line 16, **rejected**: only four fields, the category is missing altogether. Reason: `expected 5 columns, found 4`.

```csv
2026-02-03,Corner Grocer,61.25,USD,Groceries
```

Line 17, good: `61.25`.

```csv
2026-02-05,Rail Pass,1,204.50,USD,Travel
```

Line 18, **rejected**: the same amount as line 4, but *without* quotes, so the comma splits it and the row has six fields. Reason: `expected 5 columns, found 6`.

```csv
2026-02-08,Book Shop,22.40,USD,Books
```

Line 19, good: `22.40`.

```csv
2026-13-10,Pharmacy,16.15,USD,Health
```

Line 20, **rejected**: month 13. Reason: `date must look like 2026-01-31, got '2026-13-10'`.

```csv
2026-02-12,Ride Share,19.30,USD,Travel
```

Line 21, good: `19.30`.

```csv
2026-02-14,Restaurant,120.00,USD,Eating Out
```

Line 22, good: `120.00`, `Eating Out`.

```csv
2026-02-20,Gym Fees,35.00,USD,Health
```

Line 23, good: `35.00`.

```csv
2026-02-22,Cinema,24.00,USD,Fun
```

Line 24, good: `24.00`.

```csv
2026-02-25,Storage Unit,45.00,US$,Home
```

Line 25, **rejected**: the currency `US$` is not a real code. Reason: `unknown currency code 'US$'`.

```csv
2026-02-27,Corner Grocer,,USD,Groceries
```

Line 26, **rejected**: the amount field is empty (two commas in a row). Reason: `amount is not a number: ''`. The file has 26 lines: 1 header, 15 good rows and 10 rejected rows, which is the "15 rows read, 10 rejected" of the report.

### `sample/expected-report.txt`, section by section

```text
Spending report (USD): 15 rows read, 10 rejected
```

The first line of the report: the currency, the number of good rows (15) and the number of rejected rows (10).

```text
2026-01
  Travel        1,204.50
  Groceries       145.80
  Fun              24.00
  Books            18.99
  Health           12.40
  Eating Out        4.50
  total         1,410.19
```

One section per month, in calendar order. Under the month: one line per category, the biggest total first, with the amounts right-aligned and shown with two decimals and a thousands comma; then the `total` line, the exact sum rounded once. January: 1,204.50 + 145.80 + 24.00 + 18.99 + 12.40 + 4.50 = 1,410.19.

```text
2026-02
  Eating Out      124.50
  Groceries        61.25
  Health           35.00
  Fun              24.00
  Books            22.40
  Travel           19.30
  total           286.45
```

February in the same form: Eating Out is 4.50 + 120.00 = 124.50; the total is 286.45.

```text
Five largest purchases
    1,204.50  2026-01-05  Rail Pass (Travel)
      120.00  2026-02-14  Restaurant (Eating Out)
       61.25  2026-02-03  Corner Grocer (Groceries)
       55.03  2026-01-12  Corner Grocer (Groceries)
       48.60  2026-01-20  Corner Grocer (Groceries)
```

The five biggest single purchases of the whole file, biggest first; a tie would be broken by the earlier date, then by description.

```text
Rejected rows (10)
  line 6: date must look like 2026-01-31, got '31/02/2026'
  line 8: amount must not be null or negative: -8.00
  line 10: category must not be blank
  line 11: currency EUR is not the report currency USD
  line 13: amount is not a number: 'sixty'
  line 16: expected 5 columns, found 4
  line 18: expected 5 columns, found 6
  line 20: date must look like 2026-01-31, got '2026-13-10'
  line 25: unknown currency code 'US$'
  line 26: amount is not a number: ''
```

The rejected rows with their line numbers in the file (the header is line 1) and their reasons: the same ten lines explained above. The count in brackets is exact even when fewer rows are shown.

### `sample/cents.csv`

```csv
date,description,amount,currency,category
```

The header line.

```csv
2026-03-01,Penny Sweets,0.10,USD,Treats
2026-03-02,Penny Sweets,0.20,USD,Treats
```

Two rows of 0.10 and 0.20 in the same category. In `double` arithmetic they would add up to 0.30000000000000004; with `BigDecimal` the total is exactly 0.30. `make cents` shows it.

### `sample/bom-and-accent.csv`

The file has two lines. Its first three bytes, shown by `od -c` as `357 273 277` (hexadecimal `EF BB BF`), are the UTF-8 **byte-order mark**; they are followed by the header `date,description,...`. The second line is `2026-03-01,Café Noir,3.20,USD,Eating Out`; the `é` is stored as the two bytes `303 251` (`C3 A9`), which is how UTF-8 writes it. Read with the wrong encoding the description would show as `CafÃ© Noir`; read without removing the mark, the first header would be `\ufeffdate`. `make bom` shows the correct result.

## 6. Try it yourself

- `head -5 sample/transactions.csv`. Expected: the header and four rows; the third data row has `"1,204.50"`.
- `awk -F, '{print NF}' sample/transactions.csv | sort | uniq -c`. Expected: `   1 4`, `  23 5`, `   2 6`: one line with 4 fields, 23 with 5 and two with 6. The two six-field lines are line 4 (the *good* row with the quoted `"1,204.50"`) and line 18 (the bad row with the same amount unquoted): a naive comma count cannot tell them apart, which is the lesson's first trap.
- `make bom`. Expected: the line `Spending report (USD): 1 rows read, 0 rejected` and a purchase line ending `Café Noir (Eating Out)`.
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
