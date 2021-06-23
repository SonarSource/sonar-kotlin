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

import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.ParenthesizedExpressionTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.junit.jupiter.api.Test;

import static org.sonarsource.slang.impl.TextRanges.range;
import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;
import static org.sonarsource.slang.utils.TreeCreationUtils.binary;
import static org.sonarsource.slang.utils.TreeCreationUtils.identifier;
import static org.sonarsource.slang.utils.TreeCreationUtils.integerLiteral;
import static org.sonarsource.slang.utils.TreeCreationUtils.literal;
import static org.assertj.core.api.Assertions.assertThat;

class ParenthesizedExpressionTreeImplTest {

  @Test
  void test() {
    Tree identifier = identifier("value");
    Tree literalInt2 = integerLiteral("2");
    Tree literalTrue = literal("true");
    Tree binary1 = binary(BinaryExpressionTree.Operator.GREATER_THAN, identifier, literalInt2);
    Token leftParenthesis1 = new TokenImpl(range(5, 1, 5, 2), "(", Token.Type.OTHER);
    Token rightParenthesis1 = new TokenImpl(range(5, 6, 5, 7), ")", Token.Type.OTHER);
    Token leftParenthesis2 = new TokenImpl(range(5, 0, 5, 1), "(", Token.Type.OTHER);
    Token rightParenthesis2 = new TokenImpl(range(5, 10, 5, 11), ")", Token.Type.OTHER);

    ParenthesizedExpressionTree parenthesisExpression1 = new ParenthesizedExpressionTreeImpl(null, binary1, leftParenthesis1, rightParenthesis1);
    Tree binary2 = binary(BinaryExpressionTree.Operator.EQUAL_TO, literalTrue, parenthesisExpression1);
    ParenthesizedExpressionTree parenthesisExpression2 = new ParenthesizedExpressionTreeImpl(null, binary2, leftParenthesis2, rightParenthesis2);

    assertThat(parenthesisExpression1.children()).hasSize(1);
    assertThat(parenthesisExpression2.children()).hasSize(1);
    assertThat(areEquivalent(parenthesisExpression1, parenthesisExpression1)).isTrue();
    assertThat(areEquivalent(parenthesisExpression1, new ParenthesizedExpressionTreeImpl(null, binary1, null, null))).isTrue();
    assertThat(areEquivalent(parenthesisExpression1, parenthesisExpression2)).isFalse();
    assertThat(parenthesisExpression1.expression()).isEqualTo(binary1);
    assertThat(parenthesisExpression2.expression()).isEqualTo(binary2);
  }

}
