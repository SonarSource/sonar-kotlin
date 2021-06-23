/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.slang.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonarsource.slang.parser.SLangConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.COMMENT;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.CONSTANT;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.KEYWORD;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.STRING;

@EnableRuleMigrationSupport
class SyntaxHighlighterTest {

  private SyntaxHighlighter highlightingVisitor = new SyntaxHighlighter();

  private SensorContextTester sensorContext;

  private SLangConverter parser = new SLangConverter();

  private DefaultInputFile inputFile;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @BeforeEach
  void setUp() {
    sensorContext = SensorContextTester.create(tempFolder.getRoot());
  }

  private void highlight(String code) throws IOException {
    inputFile = new TestInputFileBuilder("moduleKey", tempFolder.newFile().getName())
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata(code).build();
    InputFileContext ctx = new InputFileContext(sensorContext, inputFile);
    highlightingVisitor.scan(ctx, parser.parse(code));
  }

  private void assertHighlighting(int columnFirst, int columnLast, @Nullable TypeOfText type) {
    assertHighlighting(1, columnFirst, columnLast, type);
  }

  private void assertHighlighting(int line, int columnFirst, int columnLast, @Nullable TypeOfText type) {
    for (int i = columnFirst; i <= columnLast; i++) {
      List<TypeOfText> typeOfTexts = sensorContext.highlightingTypeAt(inputFile.key(), line, i);
      if (type != null) {
        assertThat(typeOfTexts).as("Expect highlighting " + type + " at line " + line + " lineOffset " + i).containsExactly(type);
      } else {
        assertThat(typeOfTexts).as("Expect no highlighting at line " + line + " lineOffset " + i).containsExactly();
      }
    }
  }

  @Test
  void empty_input() throws Exception {
    highlight("");
    assertHighlighting(1, 0, 0, null);
  }

  @Test
  void single_line_comment() throws Exception {
    highlight("  // Comment ");
    assertHighlighting(0, 1, null);
    assertHighlighting(2, 12, COMMENT);
  }

  @Test
  void comment() throws Exception {
    highlight("  /*Comment*/ ");
    assertHighlighting(0, 1, null);
    assertHighlighting(2, 12, COMMENT);
    assertHighlighting(13, 13, null);
  }

  @Test
  void multiline_comment() throws Exception {
    highlight("/*\nComment\n*/ ");
    assertHighlighting(1, 0, 1, COMMENT);
    assertHighlighting(2, 0, 6, COMMENT);
    assertHighlighting(3, 0, 1, COMMENT);
    assertHighlighting(3, 2, 2, null);
  }

  @Test
  void keyword() throws Exception {
    highlight("fun foo() { if(x) y; }");
    assertHighlighting(0, 2, KEYWORD);
    assertHighlighting(3, 11, null);
    assertHighlighting(12, 13, KEYWORD);
    assertHighlighting(14, 22, null);
  }

  @Test
  void string_literal() throws Exception {
    highlight("x + \"abc\" + y;");
    assertHighlighting(1, 3, null);
    assertHighlighting(4, 8, STRING);
    assertHighlighting(9, 9, null);
  }

  @Test
  void numeric_literal() throws Exception {
    highlight("x + 123 + y;");
    assertHighlighting(1, 3, null);
    assertHighlighting(4, 6, CONSTANT);
    assertHighlighting(7, 7, null);
  }

}
