package com.plainround.spend.domain;

/**
 * The outcome of reading one input line: a valid {@link Transaction} or a {@link Rejected} reason.
 * A bad row is expected input, so it is data and not an exception; the sealed type makes the
 * compiler force every caller to handle both cases.
 */
public sealed interface ParsedRow permits ParsedRow.Ok, ParsedRow.Rejected {

  /**
   * A row that became a valid transaction.
   *
   * @param transaction the parsed transaction
   */
  record Ok(Transaction transaction) implements ParsedRow {}

  /**
   * A row that could not be used.
   *
   * @param lineNumber the line in the file, counting the header as line 1
   * @param line the raw text of the line
   * @param reason what is wrong, written for the person who has to fix the file
   */
  record Rejected(int lineNumber, String line, String reason) implements ParsedRow {}
}
