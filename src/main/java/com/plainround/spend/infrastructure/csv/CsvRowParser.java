package com.plainround.spend.infrastructure.csv;

import com.plainround.spend.domain.ParsedRow;
import com.plainround.spend.domain.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Currency;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Turns one CSV line into a {@link ParsedRow}. Never throws for bad input: every failure becomes a
 * {@link ParsedRow.Rejected} whose reason a person can act on.
 */
public final class CsvRowParser {

  private static final int COLUMNS = 5;

  /** Plain decimals only: BigDecimal also accepts exponents such as 1E+999999999, a memory bomb. */
  private static final Pattern PLAIN_DECIMAL = Pattern.compile("[+-]?\\d+(\\.\\d+)?");

  private final Currency reportCurrency;

  /**
   * Creates a parser.
   *
   * @param reportCurrency the only currency accepted; other currencies are rejected
   */
  public CsvRowParser(Currency reportCurrency) {
    this.reportCurrency = reportCurrency;
  }

  /**
   * Parses one data line.
   *
   * @param lineNumber the line number in the file, header counted as line 1
   * @param line the raw line
   * @return {@link ParsedRow.Ok} or {@link ParsedRow.Rejected}
   */
  public ParsedRow parse(int lineNumber, String line) {
    List<String> fields = CsvFieldSplitter.split(line);
    if (fields.size() != COLUMNS) {
      return reject(lineNumber, line, "expected 5 columns, found " + fields.size());
    }
    // The order of the catch blocks matters: NumberFormatException is an
    // IllegalArgumentException, and the broadest one must come last.
    try {
      return new ParsedRow.Ok(transaction(fields));
    } catch (DateTimeParseException problem) {
      return reject(
          lineNumber, line, "date must look like 2026-01-31, got '" + fields.get(0) + "'");
    } catch (NumberFormatException problem) {
      return reject(lineNumber, line, "amount is not a number: '" + fields.get(2) + "'");
    } catch (IllegalArgumentException problem) {
      return reject(lineNumber, line, problem.getMessage());
    }
  }

  private Transaction transaction(List<String> fields) {
    LocalDate date = LocalDate.parse(fields.get(0), DateTimeFormatter.ISO_LOCAL_DATE);
    // The String constructor keeps the digits typed; new BigDecimal(double) would not.
    BigDecimal amount = amount(fields.get(2));
    Currency currency = currency(fields.get(3));
    return new Transaction(date, fields.get(1), amount, currency, fields.get(4));
  }

  private static BigDecimal amount(String text) {
    String digits = text.replace(",", "");
    if (!PLAIN_DECIMAL.matcher(digits).matches()) {
      throw new NumberFormatException("not a plain decimal: " + text);
    }
    return new BigDecimal(digits);
  }

  private Currency currency(String code) {
    Currency currency;
    try {
      currency = Currency.getInstance(code);
    } catch (IllegalArgumentException problem) {
      // Currency.getInstance throws with no message at all, so say what is wrong.
      throw new IllegalArgumentException("unknown currency code '" + code + "'", problem);
    }
    if (!currency.equals(reportCurrency)) {
      throw new IllegalArgumentException(
          "currency " + currency + " is not the report currency " + reportCurrency);
    }
    return currency;
  }

  private static ParsedRow reject(int lineNumber, String line, String reason) {
    return new ParsedRow.Rejected(lineNumber, line, reason);
  }
}
