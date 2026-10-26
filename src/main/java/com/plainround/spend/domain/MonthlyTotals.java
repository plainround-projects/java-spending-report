package com.plainround.spend.domain;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Everything spent in one month.
 *
 * @param month the calendar month
 * @param categories the category totals, largest first, ties by name
 * @param total the exact sum over all categories
 */
public record MonthlyTotals(YearMonth month, List<CategoryTotal> categories, BigDecimal total) {

  /** Copies the list so the value cannot change after construction. */
  public MonthlyTotals {
    categories = List.copyOf(categories);
  }
}
