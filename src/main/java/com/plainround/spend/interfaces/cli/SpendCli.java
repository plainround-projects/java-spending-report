package com.plainround.spend.interfaces.cli;

import com.plainround.spend.application.BuildSpendingReport;
import com.plainround.spend.application.InputUnreadableException;
import com.plainround.spend.domain.SpendingReport;
import com.plainround.spend.infrastructure.Settings;
import com.plainround.spend.infrastructure.csv.CsvFileRowSource;
import com.plainround.spend.infrastructure.csv.CsvRowParser;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line entry point: {@code spend [--currency CODE] [--top N] FILE}. Results go to standard
 * output, logs to standard error. Exit status: 0 clean, 1 file unreadable, 2 some rows rejected, 64
 * wrong usage.
 */
public final class SpendCli {

  /** Exit status when every row was read. */
  public static final int EXIT_OK = 0;

  /** Exit status when the file as a whole could not be read. */
  public static final int EXIT_UNREADABLE = 1;

  /** Exit status when at least one row was rejected, so a script can notice. */
  public static final int EXIT_REJECTS = 2;

  /** Exit status for wrong command-line usage or configuration. */
  public static final int EXIT_USAGE = 64;

  private static final Logger LOG = LoggerFactory.getLogger(SpendCli.class);
  private static final String USAGE =
      "usage: spend [--currency CODE] [--top N] FILE   (env: SPEND_CURRENCY, SPEND_TOP,"
          + " SPEND_MAX_REJECTED, LOG_LEVEL)";

  private final Map<String, String> environment;
  private final PrintStream out;
  private final PrintStream err;

  SpendCli(Map<String, String> environment, PrintStream out, PrintStream err) {
    this.environment = environment;
    this.out = out;
    this.err = err;
  }

  /**
   * Runs the tool.
   *
   * @param args the command-line arguments
   * @param environment the environment variables
   * @param out where the report goes
   * @param err where usage errors go
   * @return the exit status
   */
  public static int run(
      List<String> args, Map<String, String> environment, PrintStream out, PrintStream err) {
    return new SpendCli(environment, out, err).execute(args);
  }

  /**
   * Process entry point.
   *
   * @param args the command-line arguments
   */
  public static void main(String[] args) {
    System.exit(run(List.of(args), System.getenv(), System.out, System.err));
  }

  private int execute(List<String> args) {
    if (args.contains("--help") || args.contains("-h")) {
      out.println(USAGE);
      return EXIT_OK;
    }
    try {
      Arguments arguments = Arguments.parse(args, Settings.fromEnvironment(environment));
      return report(arguments);
    } catch (IllegalArgumentException problem) {
      err.println("spend: " + problem.getMessage());
      err.println(USAGE);
      return EXIT_USAGE;
    }
  }

  private int report(Arguments arguments) {
    Settings settings = arguments.settings();
    CsvRowParser parser = new CsvRowParser(settings.reportCurrency());
    CsvFileRowSource source = new CsvFileRowSource(arguments.file(), parser);
    try {
      SpendingReport report =
          new BuildSpendingReport(
                  source,
                  settings.reportCurrency(),
                  settings.largestCount(),
                  settings.maxRejectedKept())
              .run();
      new ReportPrinter(out, report).print(report);
      LOG.info("report done rows={} rejected={}", report.rowsRead(), report.rejectedCount());
      return report.hasRejects() ? EXIT_REJECTS : EXIT_OK;
    } catch (InputUnreadableException problem) {
      err.println("spend: " + problem.getMessage());
      return EXIT_UNREADABLE;
    }
  }

  /** The parsed command line. */
  private record Arguments(Path file, Settings settings) {

    static Arguments parse(List<String> args, Settings defaults) {
      Settings settings = defaults;
      Path file = null;
      for (int index = 0; index < args.size(); index++) {
        String argument = args.get(index);
        switch (argument) {
          case "--currency" -> settings = settings.withCurrency(value(args, ++index, argument));
          case "--top" ->
              settings = settings.withLargestCount(count(value(args, ++index, argument)));
          default -> file = single(file, argument);
        }
      }
      if (file == null) {
        throw new IllegalArgumentException("missing FILE");
      }
      return new Arguments(file, settings);
    }

    private static String value(List<String> args, int index, String option) {
      if (index >= args.size()) {
        throw new IllegalArgumentException(option + " needs a value");
      }
      return args.get(index);
    }

    private static int count(String text) {
      try {
        return Integer.parseInt(text);
      } catch (NumberFormatException problem) {
        throw new IllegalArgumentException("--top is not a whole number: '" + text + "'", problem);
      }
    }

    private static Path single(Path current, String argument) {
      if (current != null || argument.startsWith("--")) {
        throw new IllegalArgumentException("unexpected argument '" + argument + "'");
      }
      return Path.of(argument);
    }
  }
}
