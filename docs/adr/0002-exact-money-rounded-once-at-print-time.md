# 2. Exact money, rounded once, half-even, at print time

## Context

A `double` stores binary fractions, so 0.10 + 0.20 is 0.30000000000000004 and errors add up over many rows. Rounding in the middle of a sum adds a rounding error per row.

## Decision

Every amount is a `BigDecimal` built from its text (`new BigDecimal(String)`, never from a `double`). Sums use `BigDecimal.add` and are never rounded. `ReportPrinter` rounds once, to the currency's minor units (`Currency.getDefaultFractionDigits`), with `RoundingMode.HALF_EVEN` (banker's rounding: ties go to the even digit, so rounding errors do not lean one way over a long column), then formats with `Locale.US` so the output is the same on every machine. Amount text is validated as a plain decimal first, so an exponent such as `1E+999999999` cannot allocate a huge number.

## Alternatives

- Whole cents in a `long`: exact and fast, but wrong for currencies with other minor units and awkward for amounts with more than two decimals in the input.
- `HALF_UP`: what `String.format` does with a `BigDecimal` by default, and what many people expect; it biases a long column of ties upward. The rule is stated in a comment where it is applied.

## Consequences

Totals can be compared with `compareTo`, not `equals` (`new BigDecimal("0.30").equals(new BigDecimal("0.3"))` is false because the scale differs). Tests assert with `isEqualByComparingTo`.
