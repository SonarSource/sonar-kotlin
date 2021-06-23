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
package org.sonarsource.slang.plugin;

import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.ImportDeclarationTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.PackageDeclarationTree;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.visitors.TreeContext;
import org.sonarsource.slang.visitors.TreeVisitor;

public class StatementsVisitor extends TreeVisitor<TreeContext> {

  private int statements;

  public StatementsVisitor() {

    register(BlockTree.class, (ctx, tree) ->
      tree.statementOrExpressions().forEach(stmt -> {
        if (!isDeclaration(stmt)) {
          statements++;
        }
      }));

    register(TopLevelTree.class, (ctx, tree) ->
      tree.declarations().forEach(decl -> {
        if (!isDeclaration(decl) && !isNative(decl) && !isBlock(decl)) {
          statements++;
        }
      }));
  }

  public int statements(Tree tree) {
    statements = 0;
    scan(new TreeContext(), tree);
    return statements;
  }

  @Override
  protected void before(TreeContext ctx, Tree root) {
    statements = 0;
  }

  private static boolean isDeclaration(Tree tree) {
    return tree instanceof ClassDeclarationTree
      || tree instanceof FunctionDeclarationTree
      || tree instanceof PackageDeclarationTree
      || tree instanceof ImportDeclarationTree;
  }

  private static boolean isNative(Tree tree) {
    return tree instanceof NativeTree;
  }

  private static boolean isBlock(Tree tree) {
    return tree instanceof BlockTree;
  }

}
