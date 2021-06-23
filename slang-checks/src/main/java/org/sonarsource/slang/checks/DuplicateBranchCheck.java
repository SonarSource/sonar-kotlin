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

import javax.annotation.Nullable;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.utils.SyntacticEquivalence;
import java.util.List;
import org.sonar.check.Rule;

@Rule(key = "S1871")
public class DuplicateBranchCheck extends AbstractBranchDuplicationCheck {

  @Override
  protected void checkDuplicatedBranches(CheckContext ctx, Tree tree, List<Tree> branches) {
    for (List<Tree> group : SyntacticEquivalence.findDuplicatedGroups(branches)) {
      Tree original = group.get(0);
      group.stream().skip(1)
        .filter(DuplicateBranchCheck::spansMultipleLines)
        .forEach(duplicated -> {
          TextRange originalRange = original.metaData().textRange();
          ctx.reportIssue(
            duplicated,
            "This branch's code block is the same as the block for the branch on line " + originalRange.start().line() + ".",
            new SecondaryLocation(originalRange, "Original"));
        });
    }

  }

  @Override
  protected void onAllIdenticalBranches(CheckContext ctx, Tree tree) {
    // handled by S3923
  }

  protected static boolean spansMultipleLines(@Nullable Tree tree) {
    if (tree == null) {
      return false;
    }
    if (tree instanceof BlockTree) {
      BlockTree block = (BlockTree) tree;
      List<Tree> statements = block.statementOrExpressions();
      if (statements.isEmpty()) {
        return false;
      }
      Tree firstStatement = statements.get(0);
      Tree lastStatement = statements.get(statements.size() - 1);
      return firstStatement.metaData().textRange().start().line() != lastStatement.metaData().textRange().end().line();
    }
    TextRange range = tree.metaData().textRange();
    return range.start().line() < range.end().line();
  }

}
