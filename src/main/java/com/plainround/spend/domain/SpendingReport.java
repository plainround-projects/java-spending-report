package com.plainround.spend.domain;

import java.util.Currency;
import java.util.List;

/**
 * The finished report: totals, the largest purchases and the rows that could not be read.
 *
 * @param currency the currency every amount is in
 * @param rowsRead how many rows became transactions
 * @param rejectedCount how many rows were rejected, including those not kept in {@code rejected}
 * @param months one entry per month with spending, oldest first
 * @param largest the largest purchases, biggest first
 * @param rejected the rejected rows kept for display, in file order
 */
public record SpendingReport(
    Currency currency,
    int rowsRead,
    int rejectedCount,
    List<MonthlyTotals> months,
    List<Transaction> largest,
    List<ParsedRow.Rejected> rejected) {

  /** Copies the lists so the value cannot change after construction. */
  public SpendingReport {
    months = List.copyOf(months);
    largest = List.copyOf(largest);
    rejected = List.copyOf(rejected);
  }

  /** Returns true when at least one row was rejected. */
  public boolean hasRejects() {
    return rejectedCount > 0;
  }
}
