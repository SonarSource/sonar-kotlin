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

import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.HasTextRange;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.visitors.TreeContext;
import org.sonarsource.slang.visitors.TreeVisitor;
import java.util.ArrayList;
import java.util.List;

public class CyclomaticComplexityVisitor extends TreeVisitor<TreeContext> {

  private List<HasTextRange> complexityTrees = new ArrayList<>();

  public CyclomaticComplexityVisitor() {

    register(FunctionDeclarationTree.class, (ctx, tree) -> {
      if (tree.name() != null && tree.body() != null) {
        complexityTrees.add(tree);
      }
    });

    register(IfTree.class, (ctx, tree) -> complexityTrees.add(tree.ifKeyword()));

    register(LoopTree.class, (ctx, tree) -> complexityTrees.add(tree));

    register(MatchCaseTree.class, (ctx, tree) -> {
      if (tree.expression() != null) {
        complexityTrees.add(tree);
      }
    });

    register(BinaryExpressionTree.class, (ctx, tree) -> {
      if (tree.operator() == BinaryExpressionTree.Operator.CONDITIONAL_AND ||
        tree.operator() == BinaryExpressionTree.Operator.CONDITIONAL_OR) {
        complexityTrees.add(tree);
      }
    });
  }

  public List<HasTextRange> complexityTrees(Tree tree) {
    this.complexityTrees = new ArrayList<>();
    this.scan(new TreeContext(), tree);
    return this.complexityTrees;
  }

  @Override
  protected void before(TreeContext ctx, Tree root) {
    complexityTrees = new ArrayList<>();
  }
}
