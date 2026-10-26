package com.plainround.spend.application;

import com.plainround.spend.domain.SpendingAccumulator;
import com.plainround.spend.domain.SpendingReport;
import java.util.Currency;

/** Use case: stream every row from a {@link RowSource} into a {@link SpendingReport}. */
public final class BuildSpendingReport {

  private final RowSource source;
  private final Currency currency;
  private final int largestCount;
  private final int maxRejectedKept;

  /**
   * Creates the use case.
   *
   * @param source where the rows come from
   * @param currency the report currency
   * @param largestCount how many largest purchases to list
   * @param maxRejectedKept how many rejected rows to keep for display
   */
  public BuildSpendingReport(
      RowSource source, Currency currency, int largestCount, int maxRejectedKept) {
    this.source = source;
    this.currency = currency;
    this.largestCount = largestCount;
    this.maxRejectedKept = maxRejectedKept;
  }

  /** Reads the whole source and returns the report. */
  public SpendingReport run() {
    SpendingAccumulator accumulator =
        new SpendingAccumulator(currency, largestCount, maxRejectedKept);
    source.readRows(accumulator::accept);
    return accumulator.report();
  }
}
