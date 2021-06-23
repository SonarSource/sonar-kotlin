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
package org.sonarsource.slang.checks;

import org.sonarsource.slang.api.BinaryExpressionTree.Operator;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.checks.utils.ExpressionUtils;
import java.util.function.Predicate;
import org.sonar.check.Rule;

import static org.sonarsource.slang.api.BinaryExpressionTree.Operator.CONDITIONAL_AND;
import static org.sonarsource.slang.api.BinaryExpressionTree.Operator.CONDITIONAL_OR;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.isBinaryOperation;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.isBooleanLiteral;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.isFalseValueLiteral;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.isNegation;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.isTrueValueLiteral;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.skipParentheses;

@Rule(key = "S1145")
public class IfConditionalAlwaysTrueOrFalseCheck implements SlangCheck {

  public static final String MESSAGE_TEMPLATE = "Remove this useless \"%s\" statement.";

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, (ctx, ifTree) -> {
      Tree condition = ifTree.condition();
      if (isAlwaysTrueOrFalse(condition)) {
        String message = String.format(MESSAGE_TEMPLATE, ifTree.ifKeyword().text());
        ctx.reportIssue(condition, message);
      }
    });
  }

  private static boolean isAlwaysTrueOrFalse(Tree originalCondition) {
    Tree condition = skipParentheses(originalCondition);
    return isBooleanLiteral(condition)
      || isTrueValueLiteral(condition)
      || isFalseValueLiteral(condition)
      || isSimpleExpressionWithLiteral(condition, CONDITIONAL_AND, ExpressionUtils::isFalseValueLiteral)
      || isSimpleExpressionWithLiteral(condition, CONDITIONAL_OR, ExpressionUtils::isTrueValueLiteral);
  }

  private static boolean isSimpleExpressionWithLiteral(Tree condition, Operator operator, Predicate<? super Tree> hasLiteralValue) {
    boolean simpleExpression = isBinaryOperation(condition, operator)
      && condition.descendants()
        .map(ExpressionUtils::skipParentheses)
        .allMatch(tree -> tree instanceof IdentifierTree
          || tree instanceof LiteralTree
          || isNegation(tree)
          || isBinaryOperation(tree, operator));

    return simpleExpression && condition.descendants().anyMatch(hasLiteralValue);
  }

}
