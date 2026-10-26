package com.plainround.spend.domain;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Folds parsed rows into a {@link SpendingReport} one at a time. Memory is bounded by the number of
 * month and category pairs, the few largest purchases and a capped list of rejects, so a file of
 * any size streams through. Amounts are added exactly and are never rounded here.
 */
public final class SpendingAccumulator {

  /** Largest first; equal amounts fall back to the earlier date, then the description. */
  private static final Comparator<Transaction> LARGEST_FIRST =
      Comparator.comparing(Transaction::amount)
          .reversed()
          .thenComparing(Transaction::date)
          .thenComparing(Transaction::description);

  private final Currency currency;
  private final int largestCount;
  private final int maxRejectedKept;
  private final Map<YearMonth, Map<String, BigDecimal>> totals = new TreeMap<>();
  private final PriorityQueue<Transaction> largest = new PriorityQueue<>(LARGEST_FIRST.reversed());
  private final List<ParsedRow.Rejected> rejected = new ArrayList<>();
  private int rowsRead;
  private int rejectedCount;

  /**
   * Creates an empty accumulator.
   *
   * @param currency the report currency
   * @param largestCount how many of the largest purchases to keep, at least 1
   * @param maxRejectedKept how many rejected rows to keep for display, at least 0
   */
  public SpendingAccumulator(Currency currency, int largestCount, int maxRejectedKept) {
    if (largestCount < 1 || maxRejectedKept < 0) {
      throw new IllegalArgumentException(
          "largestCount must be at least 1, maxRejectedKept 0 or more");
    }
    this.currency = currency;
    this.largestCount = largestCount;
    this.maxRejectedKept = maxRejectedKept;
  }

  /** Adds one parsed row to the running totals. */
  public void accept(ParsedRow row) {
    switch (row) {
      case ParsedRow.Ok ok -> add(ok.transaction());
      case ParsedRow.Rejected bad -> reject(bad);
    }
  }

  private void add(Transaction transaction) {
    rowsRead++;
    totals
        .computeIfAbsent(YearMonth.from(transaction.date()), month -> new TreeMap<>())
        .merge(transaction.category(), transaction.amount(), BigDecimal::add);
    largest.add(transaction);
    if (largest.size() > largestCount) {
      largest.poll();
    }
  }

  private void reject(ParsedRow.Rejected bad) {
    rejectedCount++;
    if (rejected.size() < maxRejectedKept) {
      rejected.add(bad);
    }
  }

  /** Returns the report for everything accepted so far. */
  public SpendingReport report() {
    List<MonthlyTotals> months =
        totals.entrySet().stream().map(SpendingAccumulator::month).toList();
    List<Transaction> top = largest.stream().sorted(LARGEST_FIRST).toList();
    return new SpendingReport(currency, rowsRead, rejectedCount, months, top, rejected);
  }

  private static MonthlyTotals month(Map.Entry<YearMonth, Map<String, BigDecimal>> entry) {
    List<CategoryTotal> categories =
        entry.getValue().entrySet().stream()
            .map(category -> new CategoryTotal(category.getKey(), category.getValue()))
            .sorted(
                Comparator.comparing(CategoryTotal::total)
                    .reversed()
                    .thenComparing(CategoryTotal::category))
            .toList();
    BigDecimal sum =
        categories.stream().map(CategoryTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new MonthlyTotals(entry.getKey(), categories, sum);
  }
}
