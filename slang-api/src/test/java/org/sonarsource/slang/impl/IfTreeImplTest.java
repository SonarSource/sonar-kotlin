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

import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IfTreeImplTest {

  @Test
  void test() {
    TreeMetaData meta = null;
    Tree condition = new LiteralTreeImpl(meta, "42");
    Tree thenBranch = new IdentifierTreeImpl(meta, "x");
    Tree elseBranch = new IdentifierTreeImpl(meta, "y");
    TokenImpl ifToken = new TokenImpl(new TextRangeImpl(1, 0, 1, 2), "if", Token.Type.KEYWORD);
    TokenImpl elseToken = new TokenImpl(new TextRangeImpl(2, 0, 1, 4), "else", Token.Type.KEYWORD);
    IfTreeImpl tree = new IfTreeImpl(meta, condition, thenBranch, elseBranch, ifToken, elseToken);
    assertThat(tree.children()).containsExactly(condition, thenBranch, elseBranch);
    assertThat(tree.condition()).isEqualTo(condition);
    assertThat(tree.thenBranch()).isEqualTo(thenBranch);
    assertThat(tree.elseBranch()).isEqualTo(elseBranch);

    assertThat(new IfTreeImpl(meta, condition, thenBranch, null, ifToken, null)
      .children()).containsExactly(condition, thenBranch);
  }

}
