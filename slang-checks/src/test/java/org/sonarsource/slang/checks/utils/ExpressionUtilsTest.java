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
package org.sonarsource.slang.checks.utils;

import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.ParenthesizedExpressionTreeImpl;
import org.sonarsource.slang.impl.UnaryExpressionTreeImpl;
import org.junit.jupiter.api.Test;

import static org.sonarsource.slang.api.BinaryExpressionTree.Operator.CONDITIONAL_AND;
import static org.sonarsource.slang.api.BinaryExpressionTree.Operator.CONDITIONAL_OR;
import static org.sonarsource.slang.api.BinaryExpressionTree.Operator.EQUAL_TO;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.isBinaryOperation;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.isBooleanLiteral;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.isFalseValueLiteral;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.isLogicalBinaryExpression;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.isNegation;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.isTrueValueLiteral;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.skipParentheses;
import static org.assertj.core.api.Assertions.assertThat;

class ExpressionUtilsTest {
  private static Tree TRUE_LITERAL = new LiteralTreeImpl(null, "true");
  private static Tree FALSE_LITERAL = new LiteralTreeImpl(null, "false");
  private static Tree NUMBER_LITERAL = new LiteralTreeImpl(null, "34");
  private static Tree TRUE_NEGATED = new UnaryExpressionTreeImpl(null, UnaryExpressionTree.Operator.NEGATE, TRUE_LITERAL);
  private static Tree FALSE_NEGATED = new UnaryExpressionTreeImpl(null, UnaryExpressionTree.Operator.NEGATE, FALSE_LITERAL);

  @Test
  void test_boolean_literal() {
    assertThat(isBooleanLiteral(TRUE_LITERAL)).isTrue();
    assertThat(isBooleanLiteral(FALSE_LITERAL)).isTrue();
    assertThat(isBooleanLiteral(NUMBER_LITERAL)).isFalse();
    assertThat(isBooleanLiteral(TRUE_NEGATED)).isFalse();
  }

  @Test
  void test_false_literal_value() {
    assertThat(isFalseValueLiteral(TRUE_LITERAL)).isFalse();
    assertThat(isFalseValueLiteral(FALSE_LITERAL)).isTrue();
    assertThat(isFalseValueLiteral(NUMBER_LITERAL)).isFalse();
    assertThat(isFalseValueLiteral(TRUE_NEGATED)).isTrue();
    assertThat(isFalseValueLiteral(FALSE_NEGATED)).isFalse();
  }

  @Test
  void test_true_literal_value() {
    assertThat(isTrueValueLiteral(TRUE_LITERAL)).isTrue();
    assertThat(isTrueValueLiteral(FALSE_LITERAL)).isFalse();
    assertThat(isTrueValueLiteral(NUMBER_LITERAL)).isFalse();
    assertThat(isTrueValueLiteral(TRUE_NEGATED)).isFalse();
    assertThat(isTrueValueLiteral(FALSE_NEGATED)).isTrue();
  }

  @Test
  void test_negation() {
    assertThat(isNegation(FALSE_LITERAL)).isFalse();
    assertThat(isNegation(NUMBER_LITERAL)).isFalse();
    assertThat(isNegation(TRUE_NEGATED)).isTrue();
  }

  @Test
  void test_binary_operation() {
    Tree binaryAnd = new BinaryExpressionTreeImpl(null, CONDITIONAL_AND, null, TRUE_LITERAL, FALSE_LITERAL);

    assertThat(isBinaryOperation(binaryAnd, CONDITIONAL_AND)).isTrue();
    assertThat(isBinaryOperation(binaryAnd, CONDITIONAL_OR)).isFalse();
  }

  @Test
  void test_logical_binary_operation() {
    Tree binaryAnd = new BinaryExpressionTreeImpl(null, CONDITIONAL_AND, null, TRUE_LITERAL, FALSE_LITERAL);
    Tree binaryOr = new BinaryExpressionTreeImpl(null, CONDITIONAL_OR, null, TRUE_LITERAL, FALSE_LITERAL);
    Tree binaryEqual = new BinaryExpressionTreeImpl(null, EQUAL_TO, null, TRUE_LITERAL, FALSE_LITERAL);

    assertThat(isLogicalBinaryExpression(binaryAnd)).isTrue();
    assertThat(isLogicalBinaryExpression(binaryOr)).isTrue();
    assertThat(isLogicalBinaryExpression(binaryEqual)).isFalse();
    assertThat(isLogicalBinaryExpression(TRUE_NEGATED)).isFalse();
  }

  @Test
  void test_skip_parentheses() {
    Tree parenthesizedExpression1 = new ParenthesizedExpressionTreeImpl(null, TRUE_LITERAL, null, null);
    Tree parenthesizedExpression2 = new ParenthesizedExpressionTreeImpl(null, parenthesizedExpression1, null, null);

    assertThat(skipParentheses(parenthesizedExpression1)).isEqualTo(TRUE_LITERAL);
    assertThat(skipParentheses(parenthesizedExpression2)).isEqualTo(TRUE_LITERAL);
    assertThat(skipParentheses(TRUE_LITERAL)).isEqualTo(TRUE_LITERAL);
  }

}
