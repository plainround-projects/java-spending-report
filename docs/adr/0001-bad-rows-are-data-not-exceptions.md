# 1. A bad row is data, not an exception

## Context

The input is deliberately messy: in the sample, 10 of 25 rows are bad. The lesson asks for a report that lists every bad row with its line number and why, and for the run to go on.

## Decision

`CsvRowParser.parse` returns a sealed `ParsedRow`: `Ok(Transaction)` or `Rejected(lineNumber, line, reason)`. Exceptions are used inside the parser to detect a problem and are converted at the edge of `parse`, never thrown out of it. Only a problem with the file as a whole (missing, not UTF-8) is an exception, `InputUnreadableException`, because nothing useful can continue.

## Alternatives

- Throw and catch per row in the caller: the reason would travel as an exception, and a forgotten catch would end the run.
- `Optional<Transaction>`: loses the reason, which is the whole point of the report.
- A generic `Result<T, E>` library type: one more dependency for a single use; the sealed interface gives the same exhaustive `switch` with the JDK alone.

## Consequences

The compiler forces `SpendingAccumulator.accept` to handle both cases (a `switch` over a sealed type with no `default`). A new outcome (say `Skipped`) would fail compilation until handled. Rejected rows cost one small record each, which is why only the first 1,000 are kept (ADR 0003).
