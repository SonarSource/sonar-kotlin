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

import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.HasTextRange;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.parser.SLangConverter;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CyclomaticComplexityVisitorTest {

  private static List<HasTextRange> getComplexityTrees(String content) {
    Tree root = new SLangConverter().parse(content);
    return new CyclomaticComplexityVisitor().complexityTrees(root);
  }

  @Test
  void test_matchCases() throws Exception {
    String content = "match (a) {" +
      "      0 -> return \"none\";" +
      "      1 -> return \"one\";" +
      "      2 -> return \"many\";" +
      "      else -> return \"it's complicated\";" +
      "    };";
    List<HasTextRange> trees = getComplexityTrees(content);
    assertThat(trees)
      .hasSize(3)
      .allMatch(tree -> tree instanceof MatchCaseTree);
  }

  @Test
  void test_functions_with_conditional() throws Exception {
    String content = "void fun foo (a) {" +
      "      if (a == 2) {" +
      "        print(a + 1);" +
      "      } else {" +
      "        print(a);" +
      "      };" +
      "    }";
    List<HasTextRange> trees = getComplexityTrees(content);
    assertThat(trees).hasSize(2);
    assertThat(trees.get(0)).isInstanceOf(FunctionDeclarationTree.class);
    assertThat(trees.get(1)).isInstanceOf(Token.class);
  }

  @Test
  void test_loops() throws Exception {
    String content =
      "for (var x = list) { " +
      "  while (x > y) { " +
      "    x = x-1;" +
      "  };" +
      "};";
    List<HasTextRange> trees = getComplexityTrees(content);
    assertThat(trees)
      .hasSize(2)
      .allMatch(tree -> tree instanceof LoopTree);

    content = "do { x = x-1; } while (x > y);";
    trees = getComplexityTrees(content);
    assertThat(trees)
      .hasSize(1)
      .allMatch(tree -> tree instanceof LoopTree);

  }

}
