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
package org.sonarsource.slang.impl;

import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchTreeImplTest {

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree expression = new IdentifierTreeImpl(null, "x");
    MatchCaseTree case1 = new MatchCaseTreeImpl(null, null, new LiteralTreeImpl(meta, "42"));
    Token keywordToken = new TokenImpl(new TextRangeImpl(1,0,1,20), "match", Token.Type.KEYWORD);
    MatchTree tree = new MatchTreeImpl(meta, expression, Collections.singletonList(case1), keywordToken);
    assertThat(tree.children()).containsExactly(expression, case1);
    assertThat(tree.expression()).isEqualTo(expression);
    assertThat(tree.cases()).containsExactly(case1);
    assertThat(tree.keyword().text()).isEqualTo("match");
  }

  @Test
  void without_expression() {
    TreeMetaData meta = null;
    MatchCaseTree case1 = new MatchCaseTreeImpl(null, null, new LiteralTreeImpl(meta, "42"));
    Token keywordToken = new TokenImpl(new TextRangeImpl(1,0,1,20), "match", Token.Type.KEYWORD);
    MatchTree tree = new MatchTreeImpl(meta, null, Collections.singletonList(case1), keywordToken);
    assertThat(tree.children()).containsExactly(case1);
    assertThat(tree.expression()).isNull();
  }

}
