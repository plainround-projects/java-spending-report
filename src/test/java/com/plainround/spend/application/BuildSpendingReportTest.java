package com.plainround.spend.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.plainround.spend.domain.ParsedRow;
import com.plainround.spend.domain.SpendingReport;
import com.plainround.spend.domain.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuildSpendingReportTest {

  private static final Currency USD = Currency.getInstance("USD");

  /** In-memory fake of the port; no file is touched. */
  private static RowSource rows(ParsedRow... rows) {
    return consumer -> List.of(rows).forEach(consumer);
  }

  @Test
  void everyRowFromTheSourceReachesTheReport() {
    ParsedRow good =
        new ParsedRow.Ok(
            new Transaction(LocalDate.of(2026, 3, 1), "Cafe", new BigDecimal("3.20"), USD, "Food"));
    ParsedRow bad = new ParsedRow.Rejected(3, "x", "broken");

    SpendingReport report = new BuildSpendingReport(rows(good, bad), USD, 5, 10).run();

    assertThat(report.rowsRead()).isEqualTo(1);
    assertThat(report.rejectedCount()).isEqualTo(1);
    assertThat(report.currency()).isEqualTo(USD);
  }
}
