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
import org.sonar.check.Rule;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S108")
public class EmptyBlockCheck implements SlangCheck {

  private static final String MESSAGE = "Either remove or fill this block of code.";

  @Override
  public void initialize(InitContext init) {
    init.register(BlockTree.class, (ctx, blockTree) -> {
      Tree parent = ctx.parent();
      if (isValidBlock(parent) && blockTree.statementOrExpressions().isEmpty()) {
        checkComments(ctx, blockTree);
      }
    });

    init.register(MatchTree.class, (ctx, matchTree) -> {
      if (matchTree.cases().isEmpty()) {
        checkComments(ctx, matchTree);
      }
    });
  }

  private static boolean isValidBlock(@Nullable Tree parent) {
    return !(parent instanceof FunctionDeclarationTree)
      && !(parent instanceof NativeTree)
      && !isWhileLoop(parent);
  }

  private static boolean isWhileLoop(@Nullable Tree parent) {
    return parent instanceof LoopTree && ((LoopTree) parent).kind() == LoopTree.LoopKind.WHILE;
  }

  private static void checkComments(CheckContext ctx, Tree tree) {
    if (tree.metaData().commentsInside().isEmpty()) {
      ctx.reportIssue(tree, MESSAGE);
    }
  }

}
