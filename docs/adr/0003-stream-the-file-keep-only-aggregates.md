# 3. Stream the file; keep only aggregates and a capped list of rejects

## Context

The lesson's interview question is "how would this change for a 2 GB file?". The sample is tiny, but the design should not need to change.

## Decision

`CsvFileRowSource` reads with `Files.lines` inside a try-with-resources block and pushes each `ParsedRow` to a consumer. `SpendingAccumulator` folds it into running totals (one map entry per month and category), a bounded priority queue for the largest purchases, and a list of rejects capped at `SPEND_MAX_REJECTED` (default 1,000) while the count stays exact. Memory is therefore bounded by the number of distinct months and categories, not by the number of rows.

## Alternatives

- `Files.readAllLines`, then stream: simpler, but the heap grows with the file.
- Keep every rejected row: a file full of bad rows would exhaust the heap, so the report would be lost exactly when it matters.

## Consequences

`make stream-check` reads 3,000,000 rows with a 32 MB heap. The printed reject list can be shorter than the count; the report says "showing the first N". The port is `RowSource.readRows(Consumer)`, a push interface, so a database or a socket could implement it the same way.
