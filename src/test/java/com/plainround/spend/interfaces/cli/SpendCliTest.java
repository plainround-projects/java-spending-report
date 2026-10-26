package com.plainround.spend.interfaces.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpendCliTest {

  private static final String HEADER = "date,description,amount,currency,category\n";

  @TempDir Path folder;

  private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
  private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

  private int run(Map<String, String> environment, String... args) {
    return SpendCli.run(
        List.of(args),
        environment,
        new PrintStream(stdout, true, StandardCharsets.UTF_8),
        new PrintStream(stderr, true, StandardCharsets.UTF_8));
  }

  private Path csv(String rows) throws IOException {
    return Files.writeString(folder.resolve("t.csv"), HEADER + rows);
  }

  private String out() {
    return stdout.toString(StandardCharsets.UTF_8);
  }

  @Test
  void aCleanFileExitsZeroAndPrintsNoRejectSection() throws IOException {
    int status = run(Map.of(), csv("2026-01-03,Grocer,10.005,USD,Food\n").toString());

    assertThat(status).isZero();
    assertThat(out()).contains("1 rows read, 0 rejected").doesNotContain("Rejected rows");
  }

  @Test
  void halfEvenRoundingIsAppliedOnlyWhenPrinting() throws IOException {
    run(Map.of(), csv("2026-01-03,A,10.005,USD,Food\n2026-01-04,B,10.015,USD,Fun\n").toString());

    assertThat(out())
        .containsPattern("Food\\s+10\\.00\\n")
        .containsPattern("Fun\\s+10\\.02\\n")
        .containsPattern("total\\s+20\\.02\\n");
  }

  @Test
  void aRejectedRowMakesTheExitStatusTwo() throws IOException {
    int status = run(Map.of(), csv("2026-01-03,Grocer,oops,USD,Food\n").toString());

    assertThat(status).isEqualTo(SpendCli.EXIT_REJECTS);
    assertThat(out()).contains("line 2: amount is not a number: 'oops'");
  }

  @Test
  void rejectsBeyondTheKeptLimitAreCountedAndTheListSaysSo() throws IOException {
    String rows = "bad\nbad\nbad\n";

    run(Map.of("SPEND_MAX_REJECTED", "1"), csv(rows).toString());

    assertThat(out()).contains("Rejected rows (3, showing the first 1)");
  }

  @Test
  void aMissingFileExitsOneWithOneLineOnStandardError() {
    int status = run(Map.of(), folder.resolve("missing.csv").toString());

    assertThat(status).isEqualTo(SpendCli.EXIT_UNREADABLE);
    assertThat(stderr.toString(StandardCharsets.UTF_8))
        .contains("cannot read")
        .doesNotContain("at ");
  }

  @Test
  void usageMistakesExitSixtyFourWithTheUsageLine() {
    assertThat(run(Map.of())).isEqualTo(SpendCli.EXIT_USAGE);
    assertThat(run(Map.of(), "a.csv", "b.csv")).isEqualTo(SpendCli.EXIT_USAGE);
    assertThat(run(Map.of(), "--top")).isEqualTo(SpendCli.EXIT_USAGE);
    assertThat(run(Map.of(), "--top", "x", "a.csv")).isEqualTo(SpendCli.EXIT_USAGE);
    assertThat(run(Map.of("SPEND_TOP", "x"), "a.csv")).isEqualTo(SpendCli.EXIT_USAGE);
    assertThat(stderr.toString(StandardCharsets.UTF_8)).contains("usage: spend");
  }

  @Test
  void helpPrintsTheUsageLineAndExitsZero() {
    assertThat(run(Map.of(), "--help")).isZero();
    assertThat(out()).startsWith("usage: spend");
  }

  @Test
  void theCurrencyAndTopFlagsChangeTheReport() throws IOException {
    Path file = csv("2026-01-03,A,5,EUR,Food\n2026-01-04,B,6,EUR,Food\n");

    int status = run(Map.of(), "--currency", "EUR", "--top", "1", file.toString());

    assertThat(status).isZero();
    assertThat(out()).contains("Spending report (EUR)").contains("1 largest purchases");
  }

  @Test
  void aCurrencyWithoutMinorUnitsPrintsWholeNumbers() throws IOException {
    run(Map.of(), "--currency", "JPY", csv("2026-01-03,A,1500,JPY,Food\n").toString());

    assertThat(out()).containsPattern("Food\\s+1,500\\n");
  }

  @Test
  void anEmptyFileStillPrintsTheHeaderLine() throws IOException {
    int status = run(Map.of(), csv("").toString());

    assertThat(status).isZero();
    assertThat(out()).isEqualTo("Spending report (USD): 0 rows read, 0 rejected\n");
  }
}
