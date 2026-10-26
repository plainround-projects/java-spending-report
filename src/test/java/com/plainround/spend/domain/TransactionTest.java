package com.plainround.spend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class TransactionTest {

  private static final Currency USD = Currency.getInstance("USD");
  private static final LocalDate DAY = LocalDate.of(2026, 1, 3);

  @Test
  void aNegativeAmountIsRejectedWithAMessageNamingTheField() {
    assertThatThrownBy(() -> new Transaction(DAY, "Ride", new BigDecimal("-8.00"), USD, "Travel"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("amount must not be null or negative: -8.00");
  }

  @Test
  void aNullAmountIsRejectedWithoutANullPointerException() {
    assertThatThrownBy(() -> new Transaction(DAY, "Ride", null, USD, "Travel"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("amount");
  }

  @Test
  void aBlankCategoryIsRejectedEvenWhenItIsOnlySpaces() {
    assertThatThrownBy(() -> new Transaction(DAY, "Ride", BigDecimal.ONE, USD, "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("category must not be blank");
  }

  @Test
  void aNullCategoryIsRejected() {
    assertThatThrownBy(() -> new Transaction(DAY, "Ride", BigDecimal.ONE, USD, null))
        .hasMessageStartingWith("category");
  }

  @Test
  void aNullDateIsRejected() {
    assertThatThrownBy(() -> new Transaction(null, "Ride", BigDecimal.ONE, USD, "Travel"))
        .hasMessage("date must not be null");
  }

  @Test
  void aNullCurrencyIsRejected() {
    assertThatThrownBy(() -> new Transaction(DAY, "Ride", BigDecimal.ONE, null, "Travel"))
        .hasMessage("currency must not be null");
  }

  @Test
  void aZeroAmountIsAllowedBecauseAWaivedFeeIsStillARow() {
    Transaction waived = new Transaction(DAY, "Fee", BigDecimal.ZERO, USD, "Fees");

    assertThat(waived.amount()).isEqualByComparingTo("0");
  }
}
