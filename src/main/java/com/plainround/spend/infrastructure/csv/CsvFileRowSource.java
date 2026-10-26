package com.plainround.spend.infrastructure.csv;

import com.plainround.spend.application.InputUnreadableException;
import com.plainround.spend.application.RowSource;
import com.plainround.spend.domain.ParsedRow;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a UTF-8 CSV file lazily: the header is skipped, a byte-order mark is stripped, blank lines
 * are ignored and line numbers count the header as line 1.
 */
public final class CsvFileRowSource implements RowSource {

  private static final Logger LOG = LoggerFactory.getLogger(CsvFileRowSource.class);
  private static final String BYTE_ORDER_MARK = "﻿";

  private final Path path;
  private final CsvRowParser parser;

  /**
   * Creates a source.
   *
   * @param path the CSV file
   * @param parser parses one data line
   */
  public CsvFileRowSource(Path path, CsvRowParser parser) {
    this.path = path;
    this.parser = parser;
  }

  @Override
  public void readRows(Consumer<ParsedRow> consumer) {
    LOG.info("reading file path={}", path);
    // The charset is spelled out: the platform default differs between machines.
    try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
      emit(lines.iterator(), consumer);
    } catch (UncheckedIOException problem) {
      throw unreadable(problem.getCause());
    } catch (IOException problem) {
      throw unreadable(problem);
    }
  }

  private void emit(Iterator<String> lines, Consumer<ParsedRow> consumer) {
    int lineNumber = 0;
    while (lines.hasNext()) {
      String line = lines.next().replace(BYTE_ORDER_MARK, "");
      lineNumber++;
      if (lineNumber > 1 && !line.isBlank()) {
        consumer.accept(parser.parse(lineNumber, line));
      }
    }
  }

  private InputUnreadableException unreadable(IOException cause) {
    String detail =
        cause instanceof CharacterCodingException
            ? "the file is not valid UTF-8"
            : cause.getClass().getSimpleName() + ": " + cause.getMessage();
    return new InputUnreadableException("cannot read " + path + " (" + detail + ")", cause);
  }
}
