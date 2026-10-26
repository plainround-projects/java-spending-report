package com.plainround.spend.infrastructure.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.plainround.spend.domain.ParsedRow;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CsvRowParserEdgeCasesTest {

  private final CsvRowParser parser = new CsvRowParser(Currency.getInstance("USD"));

  private String reasonFor(String line) {
    return ((ParsedRow.Rejected) parser.parse(9, line)).reason();
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "2026-01-11,Ride Share,-8.00,USD,Travel|amount must not be null or negative: -8.00",
        "2026-01-14,Streaming Plan,15.99,USD,|category must not be blank",
        "2026-01-18,Hardware Store,sixty,USD,Home|amount is not a number: 'sixty'",
        "2026-02-05,Rail Pass,1,204.50,USD,Travel|expected 5 columns, found 6",
        "2026-13-10,Pharmacy,16.15,USD,Health|date must look like 2026-01-31, got '2026-13-10'",
        "2026-02-25,Storage Unit,45.00,US$,Home|unknown currency code 'US$'",
        "2026-02-27,Corner Grocer,,USD,Groceries|amount is not a number: ''",
      })
  void everyPlantedBadRowOfTheLessonGetsAnActionableReason(String line, String reason) {
    assertThat(reasonFor(line)).isEqualTo(reason);
  }

  @Test
  void anExponentAmountIsNotANumberSoItCannotBlowUpMemory() {
    assertThat(reasonFor("2026-01-01,Odd,1E+999999999,USD,Misc"))
        .isEqualTo("amount is not a number: '1E+999999999'");
  }

  @Test
  void aCategoryOfSpacesIsBlank() {
    assertThat(reasonFor("2026-01-01,Odd,1.00,USD,   ")).isEqualTo("category must not be blank");
  }

  @Test
  void aZeroAmountIsAGoodRow() {
    assertThat(parser.parse(2, "2026-01-01,Waived,0.00,USD,Fees")).isInstanceOf(ParsedRow.Ok.class);
  }

  @Test
  void theSplitterKeepsAQuotedCommaInsideOneField() {
    assertThat(CsvFieldSplitter.split("a,\"b,c\",d")).containsExactly("a", "b,c", "d");
  }

  @Test
  void anEmptyLineIsOneEmptyField() {
    assertThat(CsvFieldSplitter.split("")).containsExactly("");
  }
}
