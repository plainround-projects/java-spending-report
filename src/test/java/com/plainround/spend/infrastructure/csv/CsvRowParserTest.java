package com.plainround.spend.infrastructure.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.plainround.spend.domain.ParsedRow;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

/**
 * The five parser tests the lesson asks for, one fault each. {@code scripts/prove-quote-test.sh}
 * deletes the quote handling and expects exactly one of these to fail.
 */
class CsvRowParserTest {

  private final CsvRowParser parser = new CsvRowParser(Currency.getInstance("USD"));

  @Test
  void aGoodRowBecomesATransactionWithTheExactAmount() {
    ParsedRow result = parser.parse(2, "2026-01-03,Corner Grocer,42.17,USD,Groceries");

    assertThat(result)
        .isInstanceOfSatisfying(
            ParsedRow.Ok.class,
            ok -> {
              assertThat(ok.transaction().amount()).isEqualTo(new BigDecimal("42.17"));
              assertThat(ok.transaction().category()).isEqualTo("Groceries");
            });
  }

  @Test
  void aQuotedAmountWithAThousandsCommaStaysOneField() {
    ParsedRow result = parser.parse(4, "2026-01-05,Rail Pass,\"1,204.50\",USD,Travel");

    assertThat(result)
        .isInstanceOfSatisfying(
            ParsedRow.Ok.class,
            ok -> assertThat(ok.transaction().amount()).isEqualTo(new BigDecimal("1204.50")));
  }

  @Test
  void aDateInTheWrongFormatIsRejectedWithTheTextThatWasRead() {
    ParsedRow result = parser.parse(6, "31/02/2026,Gym Fees,35.00,USD,Health");

    assertThat(result)
        .isEqualTo(
            new ParsedRow.Rejected(
                6,
                "31/02/2026,Gym Fees,35.00,USD,Health",
                "date must look like 2026-01-31, got '31/02/2026'"));
  }

  @Test
  void aMissingColumnIsRejectedWithTheColumnCount() {
    ParsedRow result = parser.parse(16, "2026-02-02,Utility Bill,89.10,USD");

    assertThat(result)
        .isInstanceOfSatisfying(
            ParsedRow.Rejected.class,
            rejected -> assertThat(rejected.reason()).isEqualTo("expected 5 columns, found 4"));
  }

  @Test
  void aCurrencyOtherThanTheReportCurrencyIsRejectedByName() {
    ParsedRow result = parser.parse(11, "2026-01-15,Hotel Stay,310.00,EUR,Travel");

    assertThat(result)
        .isInstanceOfSatisfying(
            ParsedRow.Rejected.class,
            rejected ->
                assertThat(rejected.reason())
                    .isEqualTo("currency EUR is not the report currency USD"));
  }
}
