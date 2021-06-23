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
package org.sonarsource.slang.antlr;

import org.sonarsource.slang.parser.SLangLexer;
import org.sonarsource.slang.parser.SLangParser;
import java.io.IOException;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SLangParserTest {

  private void testFile(String file) throws IOException {
    SLangLexer lexer = new SLangLexer(CharStreams.fromFileName(file));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SLangParser parser = new SLangParser(tokens);
    SLangParser.SlangFileContext context = parser.slangFile();
    assertThat(context.children).isNotNull();
    assertThat(context.children).isNotEmpty();
  }

  @Test
  void testBinaryExpressionFile() throws IOException {
    testFile("src/test/resources/binary.slang");
  }

  @Test
  void testConditionalFile() throws IOException {
    testFile("src/test/resources/conditional.slang");
  }

  @Test
  void testAnnotationsFile() throws IOException {
    testFile("src/test/resources/annotations.slang");
  }

  @Test
  void testBinaryExpression() {
    SLangLexer lexer = new SLangLexer(CharStreams.fromString("x = 1;\n//comment\ny = 2 + \"1\";"));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SLangParser parser = new SLangParser(tokens);
    SLangParser.SlangFileContext tree = parser.slangFile();

    assertThat(tree.children)
      .isNotNull()
      .isNotEmpty();

  }
}
