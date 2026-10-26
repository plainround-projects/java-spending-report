package com.plainround.spend.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingsTest {

  @Test
  void defaultsAreUsdTopFiveAndAThousandKeptRejects() {
    Settings settings = Settings.fromEnvironment(Map.of());

    assertThat(settings.reportCurrency().getCurrencyCode()).isEqualTo("USD");
    assertThat(settings.largestCount()).isEqualTo(5);
    assertThat(settings.maxRejectedKept()).isEqualTo(1000);
  }

  @Test
  void environmentVariablesOverrideTheDefaults() {
    Settings settings =
        Settings.fromEnvironment(
            Map.of("SPEND_CURRENCY", "EUR", "SPEND_TOP", " 3 ", "SPEND_MAX_REJECTED", "0"));

    assertThat(settings.reportCurrency().getCurrencyCode()).isEqualTo("EUR");
    assertThat(settings.largestCount()).isEqualTo(3);
    assertThat(settings.maxRejectedKept()).isZero();
  }

  @Test
  void malformedValuesFailWithTheVariableName() {
    assertThatThrownBy(() -> Settings.fromEnvironment(Map.of("SPEND_TOP", "many")))
        .hasMessageContaining("SPEND_TOP");
    assertThatThrownBy(() -> Settings.fromEnvironment(Map.of("SPEND_CURRENCY", "US$")))
        .hasMessageContaining("US$");
    assertThatThrownBy(() -> Settings.fromEnvironment(Map.of("SPEND_TOP", "0")))
        .hasMessageContaining("SPEND_TOP");
    assertThatThrownBy(() -> Settings.fromEnvironment(Map.of("SPEND_MAX_REJECTED", "-1")))
        .hasMessageContaining("SPEND_MAX_REJECTED");
  }

  @Test
  void withersReplaceOneValue() {
    Settings base = Settings.fromEnvironment(Map.of());

    assertThat(base.withCurrency("JPY").reportCurrency().getCurrencyCode()).isEqualTo("JPY");
    assertThat(base.withLargestCount(9).largestCount()).isEqualTo(9);
  }
}
