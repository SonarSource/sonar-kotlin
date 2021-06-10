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

import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.ParenthesizedExpressionTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import java.util.Collections;
import java.util.Iterator;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

import static org.sonarsource.slang.checks.utils.ExpressionUtils.isLogicalBinaryExpression;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.skipParentheses;

@Rule(key = "S1067")
public class TooComplexExpressionCheck implements SlangCheck {

  private static final int DEFAULT_MAX_COMPLEXITY = 3;

  @RuleProperty(key = "max",
    description = "Maximum number of allowed conditional operators in an expression",
    defaultValue = "" + DEFAULT_MAX_COMPLEXITY)
  public int max = DEFAULT_MAX_COMPLEXITY;

  @Override
  public void initialize(InitContext init) {
    init.register(BinaryExpressionTree.class, (ctx, tree) -> {
      if (isParentExpression(ctx)) {
        int complexity = computeExpressionComplexity(tree);
        if (complexity > max) {
          String message = String.format(
            "Reduce the number of conditional operators (%s) used in the expression (maximum allowed %s).",
            complexity,
            max);
          double gap = (double) complexity - max;
          ctx.reportIssue(tree, message, Collections.emptyList(), gap);
        }
      }
    });
  }

  private static boolean isParentExpression(CheckContext ctx) {
    Iterator<Tree> iterator = ctx.ancestors().iterator();
    while (iterator.hasNext()) {
      Tree parentExpression = iterator.next();
      if (parentExpression instanceof BinaryExpressionTree) {
        return false;
      } else if (!(parentExpression instanceof UnaryExpressionTree)
        // TODO(Godin): seems that instead of logical-or should be logical-and
        || !(parentExpression instanceof ParenthesizedExpressionTree)) {
        return true;
      }
    }
    return true;
  }

  private static int computeExpressionComplexity(Tree originalTree) {
    Tree tree = skipParentheses(originalTree);
    if (tree instanceof BinaryExpressionTree) {
      int complexity = isLogicalBinaryExpression(tree) ? 1 : 0;
      BinaryExpressionTree binary = (BinaryExpressionTree) tree;
      return complexity
        + computeExpressionComplexity(binary.leftOperand())
        + computeExpressionComplexity(binary.rightOperand());
    } else if (tree instanceof UnaryExpressionTree) {
      return computeExpressionComplexity(((UnaryExpressionTree) tree).operand());
    } else {
      return 0;
    }
  }

}
