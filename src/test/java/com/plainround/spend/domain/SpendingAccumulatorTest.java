package com.plainround.spend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class SpendingAccumulatorTest {

  private static final Currency USD = Currency.getInstance("USD");

  private static ParsedRow ok(String date, String description, String amount, String category) {
    return new ParsedRow.Ok(
        new Transaction(LocalDate.parse(date), description, new BigDecimal(amount), USD, category));
  }

  private static ParsedRow rejected(int line) {
    return new ParsedRow.Rejected(line, "raw", "reason " + line);
  }

  @Test
  void tenCentsPlusTwentyCentsIsExactlyThirtyCents() {
    SpendingAccumulator accumulator = new SpendingAccumulator(USD, 5, 10);
    accumulator.accept(ok("2026-01-01", "a", "0.10", "Misc"));
    accumulator.accept(ok("2026-01-02", "b", "0.20", "Misc"));

    BigDecimal total = accumulator.report().months().getFirst().total();

    assertThat(total).isEqualByComparingTo("0.30");
  }

  @Test
  void totalsAreGroupedPerMonthAndSortedLargestCategoryFirst() {
    SpendingAccumulator accumulator = new SpendingAccumulator(USD, 5, 10);
    accumulator.accept(ok("2026-02-01", "a", "5.00", "Books"));
    accumulator.accept(ok("2026-01-01", "b", "1.00", "Books"));
    accumulator.accept(ok("2026-01-02", "c", "9.00", "Travel"));
    accumulator.accept(ok("2026-01-03", "d", "2.50", "Books"));

    SpendingReport report = accumulator.report();

    assertThat(report.months())
        .extracting(MonthlyTotals::month)
        .containsExactly(YearMonth.of(2026, 1), YearMonth.of(2026, 2));
    assertThat(report.months().getFirst().categories())
        .extracting(CategoryTotal::category)
        .containsExactly("Travel", "Books");
    assertThat(report.months().getFirst().total()).isEqualByComparingTo("12.50");
  }

  @Test
  void equalCategoryTotalsAreOrderedByName() {
    SpendingAccumulator accumulator = new SpendingAccumulator(USD, 5, 10);
    accumulator.accept(ok("2026-01-01", "a", "3.00", "Zoo"));
    accumulator.accept(ok("2026-01-02", "b", "3.00", "Art"));

    assertThat(accumulator.report().months().getFirst().categories())
        .extracting(CategoryTotal::category)
        .containsExactly("Art", "Zoo");
  }

  @Test
  void onlyTheLargestPurchasesAreKeptBiggestFirstWithTiesByEarlierDate() {
    SpendingAccumulator accumulator = new SpendingAccumulator(USD, 2, 10);
    accumulator.accept(ok("2026-01-05", "late tie", "50.00", "A"));
    accumulator.accept(ok("2026-01-01", "small", "1.00", "A"));
    accumulator.accept(ok("2026-01-02", "early tie", "50.00", "A"));
    accumulator.accept(ok("2026-01-03", "huge", "99.00", "A"));

    assertThat(accumulator.report().largest())
        .extracting(Transaction::description)
        .containsExactly("huge", "early tie");
  }

  @Test
  void rejectedRowsAreCountedAndOnlyTheFirstOnesKept() {
    SpendingAccumulator accumulator = new SpendingAccumulator(USD, 5, 2);
    accumulator.accept(rejected(2));
    accumulator.accept(rejected(3));
    accumulator.accept(rejected(4));

    SpendingReport report = accumulator.report();

    assertThat(report.rejectedCount()).isEqualTo(3);
    assertThat(report.rejected()).extracting(ParsedRow.Rejected::lineNumber).containsExactly(2, 3);
    assertThat(report.hasRejects()).isTrue();
  }

  @Test
  void anEmptyRunHasNoRejectsAndNoMonths() {
    SpendingReport report = new SpendingAccumulator(USD, 5, 10).report();

    assertThat(report.hasRejects()).isFalse();
    assertThat(report.months()).isEmpty();
    assertThat(report.rowsRead()).isZero();
  }

  @Test
  void invalidLimitsAreRefused() {
    assertThatThrownBy(() -> new SpendingAccumulator(USD, 0, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new SpendingAccumulator(USD, 5, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reportListsAreImmutableCopies() {
    SpendingReport report = new SpendingAccumulator(USD, 5, 10).report();

    assertThatThrownBy(() -> report.months().add(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
