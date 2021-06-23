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
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonar.check.Rule;

import static org.sonarsource.slang.checks.utils.ExpressionUtils.containsPlaceHolder;
import static org.sonarsource.slang.checks.utils.ExpressionUtils.skipParentheses;
import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;

@Rule(key = "S1764")
public class IdenticalBinaryOperandCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(BinaryExpressionTree.class, (ctx, tree) -> {
      if (tree.operator() != BinaryExpressionTree.Operator.PLUS
        && tree.operator() != BinaryExpressionTree.Operator.TIMES
        && !containsPlaceHolder(tree)
        && areEquivalent(skipParentheses(tree.leftOperand()), skipParentheses(tree.rightOperand()))) {
        ctx.reportIssue(
          tree.rightOperand(),
          "Correct one of the identical sub-expressions on both sides this operator",
          new SecondaryLocation(tree.leftOperand()));
      }
    });
  }

}
