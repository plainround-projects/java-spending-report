package com.plainround.spend.infrastructure.csv;

import java.util.ArrayList;
import java.util.List;

/** Splits one CSV line on commas that are not inside double quotes. */
final class CsvFieldSplitter {

  private CsvFieldSplitter() {}

  /**
   * Splits a line into trimmed fields; quote characters are removed from the result.
   *
   * @param line one line of CSV text
   * @return the fields, never empty (an empty line is one empty field)
   */
  static List<String> split(String line) {
    List<String> fields = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean insideQuotes = false;
    for (char symbol : line.toCharArray()) {
      if (symbol == '"') {
        insideQuotes = !insideQuotes;
      } else if (symbol == ',' && !insideQuotes) {
        fields.add(current.toString().trim());
        current.setLength(0);
      } else {
        current.append(symbol);
      }
    }
    fields.add(current.toString().trim());
    return fields;
  }
}
