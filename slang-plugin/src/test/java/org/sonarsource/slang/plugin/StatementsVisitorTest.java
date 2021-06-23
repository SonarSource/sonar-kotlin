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

import org.junit.jupiter.api.Test;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.parser.SLangConverter;

import static org.assertj.core.api.Assertions.assertThat;

class StatementsVisitorTest {

  private static int statements(String content) {
    Tree root = new SLangConverter().parse(content);
    return new StatementsVisitor().statements(root);
  }

  @Test
  void should_count_top_level_without_natives_and_blocks() throws Exception {
    String content = "" +
      "package abc;" +
      "import xyz;" +
      "native[] { };" +
      "foo;" + // +1
      "class A{};" +
      "if (a) {};" + // +1
      "fun foo() {};" +
      "{};";
    assertThat(statements(content)).isEqualTo(2);
  }

  @Test
  void should_count_statements_inside_blocks() throws Exception {
    String content = "fun foo() {" +
      "native[] { };" + // +1
      "foo;" + // +1
      "if (a) { foo; bar; };" + // +1 +1 +1
      "class A{};" +
      "fun bar(){};" +
      "};" +
      "{ 2; 3; };"; // +1 +1
    assertThat(statements(content)).isEqualTo(7);
  }

}
