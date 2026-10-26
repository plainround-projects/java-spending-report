# 4. A hand-written CSV splitter behind a port

## Context

The lesson teaches that `line.split(",")` breaks on `"1,204.50"` and asks the learner to write the splitter (a flag that flips on each quote character).

## Decision

`CsvFieldSplitter` is a package-private, 15-line state machine used by `CsvRowParser`. It handles quoted fields. It does not handle an escaped quote (`""`) or a newline inside a field; the reader is line-based.

## Alternatives

- A CSV library (OpenCSV, Commons CSV): handles all of RFC 4180, at the price of a dependency for a teaching project whose first lesson is why the naive split fails.

## Consequences

The limits are listed in the README. Swapping in a library means writing one new `RowSource` adapter in `infrastructure/`; nothing in `domain` or `application` changes, which is why the format knowledge lives only in `infrastructure.csv`.
