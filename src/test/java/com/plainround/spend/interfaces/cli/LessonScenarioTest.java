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

/** End to end: the lesson's 25-row file through the command line, compared with its output. */
class LessonScenarioTest {

  private static final Path SAMPLE = Path.of("sample/transactions.csv");
  private static final Path EXPECTED = Path.of("sample/expected-report.txt");

  @Test
  void theSampleFileProducesTheLessonsReportAndExitStatusTwo() throws IOException {
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8);

    int status = SpendCli.run(List.of(SAMPLE.toString()), Map.of(), out, System.err);

    assertThat(status).isEqualTo(2);
    assertThat(stdout.toString(StandardCharsets.UTF_8))
        .isEqualTo(Files.readString(EXPECTED, StandardCharsets.UTF_8));
  }

  @Test
  void januaryGroceriesMatchTheHandCalculationToTheCent() throws IOException {
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    SpendCli.run(
        List.of(SAMPLE.toString()),
        Map.of(),
        new PrintStream(stdout, true, StandardCharsets.UTF_8),
        System.err);

    // 42.17 + 55.03 + 48.60 = 145.80
    assertThat(stdout.toString(StandardCharsets.UTF_8)).contains("  Groceries       145.80");
  }
}
