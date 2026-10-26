package com.plainround.spend.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

/**
 * One card transaction. Money is an exact {@link BigDecimal}; a zero amount is allowed (a waived
 * fee is still a real row), a negative one is not.
 *
 * @param date the day the purchase was posted
 * @param description the merchant text as exported
 * @param amount the exact amount, never negative
 * @param currency the currency of the amount
 * @param category the spending category, never blank
 */
public record Transaction(
    LocalDate date, String description, BigDecimal amount, Currency currency, String category) {

  /** Rejects missing or invalid fields, naming the field first in each message. */
  public Transaction {
    if (date == null) {
      throw new IllegalArgumentException("date must not be null");
    }
    if (amount == null || amount.signum() < 0) {
      throw new IllegalArgumentException("amount must not be null or negative: " + amount);
    }
    if (currency == null) {
      throw new IllegalArgumentException("currency must not be null");
    }
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("category must not be blank");
    }
  }
}
