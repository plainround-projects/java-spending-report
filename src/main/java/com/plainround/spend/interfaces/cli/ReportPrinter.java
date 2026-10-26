package com.plainround.spend.interfaces.cli;

import com.plainround.spend.domain.CategoryTotal;
import com.plainround.spend.domain.MonthlyTotals;
import com.plainround.spend.domain.ParsedRow;
import com.plainround.spend.domain.SpendingReport;
import com.plainround.spend.domain.Transaction;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Renders a {@link SpendingReport} as text. This is the only place money is rounded: once, at print
 * time, half-even, so that the rounding error of a long column of values does not lean one way.
 */
final class ReportPrinter {

  private final PrintStream out;
  private final int fractionDigits;
  private final String currencyCode;

  ReportPrinter(PrintStream out, SpendingReport report) {
    this.out = out;
    this.fractionDigits = Math.max(report.currency().getDefaultFractionDigits(), 0);
    this.currencyCode = report.currency().getCurrencyCode();
  }

  void print(SpendingReport report) {
    out.printf(
        "Spending report (%s): %d rows read, %d rejected%n",
        currencyCode, report.rowsRead(), report.rejectedCount());
    report.months().forEach(this::month);
    if (!report.largest().isEmpty()) {
      largest(report);
    }
    if (report.hasRejects()) {
      rejected(report);
    }
  }

  private void month(MonthlyTotals month) {
    out.printf("%n%s%n", month.month());
    month.categories().forEach(this::categoryLine);
    out.printf("  %-12s%10s%n", "total", money(month.total()));
  }

  private void categoryLine(CategoryTotal category) {
    out.printf("  %-12s%10s%n", category.category(), money(category.total()));
  }

  private void largest(SpendingReport report) {
    int count = report.largest().size();
    out.printf("%n%s largest purchases%n", count == 5 ? "Five" : String.valueOf(count));
    report.largest().forEach(this::purchaseLine);
  }

  private void purchaseLine(Transaction transaction) {
    out.printf(
        "%12s  %s  %s (%s)%n",
        money(transaction.amount()),
        transaction.date(),
        transaction.description(),
        transaction.category());
  }

  private void rejected(SpendingReport report) {
    int shown = report.rejected().size();
    String suffix = shown < report.rejectedCount() ? ", showing the first " + shown : "";
    out.printf("%nRejected rows (%d%s)%n", report.rejectedCount(), suffix);
    for (ParsedRow.Rejected row : report.rejected()) {
      out.printf("  line %d: %s%n", row.lineNumber(), row.reason());
    }
  }

  /** Rounds half-even to the currency's minor units, then formats with US grouping everywhere. */
  private String money(BigDecimal amount) {
    BigDecimal rounded = amount.setScale(fractionDigits, RoundingMode.HALF_EVEN);
    return String.format(Locale.US, "%,." + fractionDigits + "f", rounded);
  }
}
