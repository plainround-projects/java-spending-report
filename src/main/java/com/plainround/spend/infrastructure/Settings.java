package com.plainround.spend.infrastructure;

import java.util.Currency;
import java.util.Map;

/**
 * Configuration read from the environment (twelve-factor) with validated defaults.
 *
 * @param reportCurrency the currency of the report; rows in other currencies are rejected
 * @param largestCount how many largest purchases to list
 * @param maxRejectedKept how many rejected rows to keep for display
 */
public record Settings(Currency reportCurrency, int largestCount, int maxRejectedKept) {

  private static final String DEFAULT_CURRENCY = "USD";

  /** Validates the values. */
  public Settings {
    if (largestCount < 1) {
      throw new IllegalArgumentException("SPEND_TOP must be at least 1: " + largestCount);
    }
    if (maxRejectedKept < 0) {
      throw new IllegalArgumentException(
          "SPEND_MAX_REJECTED must be 0 or more: " + maxRejectedKept);
    }
  }

  /**
   * Reads {@code SPEND_CURRENCY} (default USD), {@code SPEND_TOP} (default 5) and {@code
   * SPEND_MAX_REJECTED} (default 1000).
   *
   * @param environment the variables, normally {@link System#getenv()}
   * @return validated settings
   * @throws IllegalArgumentException when a value is malformed
   */
  public static Settings fromEnvironment(Map<String, String> environment) {
    return new Settings(
        currency(environment.getOrDefault("SPEND_CURRENCY", DEFAULT_CURRENCY)),
        integer(environment, "SPEND_TOP", 5),
        integer(environment, "SPEND_MAX_REJECTED", 1000));
  }

  /** Returns these settings with another currency. */
  public Settings withCurrency(String code) {
    return new Settings(currency(code), largestCount, maxRejectedKept);
  }

  /** Returns these settings with another largest-purchase count. */
  public Settings withLargestCount(int count) {
    return new Settings(reportCurrency, count, maxRejectedKept);
  }

  private static Currency currency(String code) {
    try {
      return Currency.getInstance(code);
    } catch (IllegalArgumentException problem) {
      throw new IllegalArgumentException("unknown currency code '" + code + "'", problem);
    }
  }

  private static int integer(Map<String, String> environment, String name, int fallback) {
    String text = environment.get(name);
    try {
      return text == null ? fallback : Integer.parseInt(text.trim());
    } catch (NumberFormatException problem) {
      throw new IllegalArgumentException(name + " is not a whole number: '" + text + "'", problem);
    }
  }
}
