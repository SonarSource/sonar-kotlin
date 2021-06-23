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

import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;
import java.util.Optional;
import org.sonar.check.Rule;

@Rule(key = "S1066")
public class CollapsibleIfStatementsCheck implements SlangCheck {
  private static final String MESSAGE_TEMPLATE = "Merge this \"%s\" statement with the nested one.";
  private static final String SECONDARY_MESSAGE_TEMPLATE = "Nested \"%s\" statement";

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, (ctx, ifTreeStatement) -> {
      if (ifTreeStatement.elseBranch() == null) {
        getCollapsibleIfStatement(ifTreeStatement.thenBranch())
          .ifPresent(innerIfStatement -> {
            TextRange innerIfRange = innerIfStatement.ifKeyword().textRange();
            String message = String.format(MESSAGE_TEMPLATE, ifTreeStatement.ifKeyword().text());
            String secondaryMessage = String.format(SECONDARY_MESSAGE_TEMPLATE, innerIfStatement.ifKeyword().text());
            ctx.reportIssue(ifTreeStatement.ifKeyword(), message, new SecondaryLocation(innerIfRange, secondaryMessage));
          });
      }
    });
  }

  private static Optional<IfTree> getCollapsibleIfStatement(Tree tree) {
    if (tree instanceof BlockTree) {
      BlockTree blockTree = (BlockTree) tree;
      return blockTree.statementOrExpressions().size() == 1
        ? getIfStatementWithoutElse(tree.children().get(0))
        : Optional.empty();
    }
    return getIfStatementWithoutElse(tree);
  }

  private static Optional<IfTree> getIfStatementWithoutElse(Tree tree) {
    return tree instanceof IfTree && ((IfTree) tree).elseBranch() == null
      ? Optional.of((IfTree) tree)
      : Optional.empty();
  }
}
