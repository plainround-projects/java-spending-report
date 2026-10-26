package com.plainround.spend.infrastructure.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.plainround.spend.application.InputUnreadableException;
import com.plainround.spend.domain.ParsedRow;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvFileRowSourceTest {

  private static final String HEADER = "date,description,amount,currency,category\n";

  @TempDir Path folder;

  private List<ParsedRow> read(Path file) {
    List<ParsedRow> rows = new ArrayList<>();
    new CsvFileRowSource(file, new CsvRowParser(Currency.getInstance("USD"))).readRows(rows::add);
    return rows;
  }

  private Path write(byte[] content) throws IOException {
    return Files.write(folder.resolve("input.csv"), content);
  }

  @Test
  void aByteOrderMarkAndAnAccentedDescriptionBothReadCorrectly() throws IOException {
    String text = "﻿" + HEADER + "2026-03-01,Café Noir,3.20,USD,Eating Out\n";

    List<ParsedRow> rows = read(write(text.getBytes(StandardCharsets.UTF_8)));

    assertThat(rows)
        .singleElement()
        .isInstanceOfSatisfying(
            ParsedRow.Ok.class,
            ok -> assertThat(ok.transaction().description()).isEqualTo("Café Noir"));
  }

  @Test
  void lineNumbersCountTheHeaderAndBlankLinesAreSkipped() throws IOException {
    String text = HEADER + "\n2026-03-01,Cafe,3.20,USD,Food\nbroken line\n";

    List<ParsedRow> rows = read(write(text.getBytes(StandardCharsets.UTF_8)));

    assertThat(rows).hasSize(2);
    assertThat(rows.get(1))
        .isInstanceOfSatisfying(
            ParsedRow.Rejected.class, rejected -> assertThat(rejected.lineNumber()).isEqualTo(4));
  }

  @Test
  void windowsLineEndingsAreAccepted() throws IOException {
    String text = HEADER.replace("\n", "\r\n") + "2026-03-01,Cafe,3.20,USD,Food\r\n";

    assertThat(read(write(text.getBytes(StandardCharsets.UTF_8))))
        .singleElement()
        .isInstanceOf(ParsedRow.Ok.class);
  }

  @Test
  void aMissingFileIsReportedAsAnUnreadableInputNotAStackTrace() {
    assertThatThrownBy(() -> read(folder.resolve("nope.csv")))
        .isInstanceOf(InputUnreadableException.class)
        .hasMessageContaining("cannot read")
        .hasMessageContaining("NoSuchFileException");
  }

  @Test
  void bytesThatAreNotUtf8AreReportedAsAnUnreadableFile() throws IOException {
    byte[] invalid = new byte[] {'d', 'a', 't', 'e', '\n', (byte) 0xC3, (byte) 0x28, '\n'};

    Path file = write(invalid);

    assertThatThrownBy(() -> read(file))
        .isInstanceOf(InputUnreadableException.class)
        .hasMessageContaining("not valid UTF-8");
  }
}
