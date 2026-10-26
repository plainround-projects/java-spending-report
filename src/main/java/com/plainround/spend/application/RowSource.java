package com.plainround.spend.application;

import com.plainround.spend.domain.ParsedRow;
import java.util.function.Consumer;

/** Port: where transaction rows come from. Rows are pushed one at a time so nothing is held. */
public interface RowSource {

  /**
   * Reads every data row in file order and hands each to the consumer as it is parsed.
   *
   * @param consumer receives one {@link ParsedRow} per non-blank data line
   * @throws InputUnreadableException when the source as a whole cannot be read
   */
  void readRows(Consumer<ParsedRow> consumer);
}
