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
package org.sonarsource.slang.checks.complexity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.CatchTree;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.utils.ExpressionUtils;
import org.sonarsource.slang.impl.JumpTreeImpl;
import org.sonarsource.slang.visitors.TreeContext;
import org.sonarsource.slang.visitors.TreeVisitor;

import static org.sonarsource.slang.checks.utils.ExpressionUtils.isLogicalBinaryExpression;

public class CognitiveComplexity {

  private List<Increment> increments = new ArrayList<>();

  public CognitiveComplexity(Tree root) {
    CognitiveComplexityVisitor visitor = new CognitiveComplexityVisitor();
    visitor.scan(new TreeContext(), root);
  }

  public int value() {
    int total = 0;
    for (Increment increment : increments) {
      total += increment.nestingLevel + 1;
    }
    return total;
  }

  public List<Increment> increments() {
    return increments;
  }

  public static class Increment {

    private final Token token;
    private final int nestingLevel;

    private Increment(Token token, int nestingLevel) {
      this.token = token;
      this.nestingLevel = nestingLevel;
    }

    public Token token() {
      return token;
    }

    public int nestingLevel() {
      return nestingLevel;
    }
  }

  private class CognitiveComplexityVisitor extends TreeVisitor<TreeContext> {

    private Set<Token> alreadyConsideredOperators = new HashSet<>();

    private CognitiveComplexityVisitor() {

      // TODO ternary operator
      // TODO "break" or "continue" with label

      register(LoopTree.class, (ctx, tree) -> incrementWithNesting(tree.keyword(), ctx));
      register(MatchTree.class, (ctx, tree) -> incrementWithNesting(tree.keyword(), ctx));
      register(CatchTree.class, (ctx, tree) -> incrementWithNesting(tree.keyword(), ctx));
      register(JumpTreeImpl.class, (ctx, tree) -> {
        if (tree.label() != null) {
          incrementWithoutNesting(tree.keyword());
        }
      });

      register(IfTree.class, (ctx, tree) -> {
        Tree parent = ctx.ancestors().peek();
        boolean isElseIf = parent instanceof IfTree && tree == ((IfTree) parent).elseBranch();
        boolean isTernary = ExpressionUtils.isTernaryOperator(ctx.ancestors(), tree);
        if (!isElseIf || isTernary) {
          incrementWithNesting(tree.ifKeyword(), ctx);
        }
        Token elseKeyword = tree.elseKeyword();
        if (elseKeyword != null && !isTernary) {
          incrementWithoutNesting(elseKeyword);
        }
      });

      register(BinaryExpressionTree.class, (ctx, tree) -> handleBinaryExpressions(tree));
    }

    private void handleBinaryExpressions(BinaryExpressionTree tree) {
      if (!isLogicalBinaryExpression(tree) || alreadyConsideredOperators.contains(tree.operatorToken())) {
        return;
      }

      List<Token> operators = new ArrayList<>();
      flattenOperators(tree, operators);

      Token previous = null;
      for (Token operator : operators) {
        if (previous == null || !previous.text().equals(operator.text())) {
          incrementWithoutNesting(operator);
        }
        previous = operator;
        alreadyConsideredOperators.add(operator);
      }
    }

    // TODO parentheses should probably be skipped
    private void flattenOperators(BinaryExpressionTree tree, List<Token> operators) {
      if (isLogicalBinaryExpression(tree.leftOperand())) {
        flattenOperators((BinaryExpressionTree) tree.leftOperand(), operators);
      }

      operators.add(tree.operatorToken());

      if (isLogicalBinaryExpression(tree.rightOperand())) {
        flattenOperators((BinaryExpressionTree) tree.rightOperand(), operators);
      }
    }

    private void incrementWithNesting(Token token, TreeContext ctx) {
      increment(token, nestingLevel(ctx));
    }

    private void incrementWithoutNesting(Token token) {
      increment(token, 0);
    }

    private void increment(Token token, int nestingLevel) {
      increments.add(new Increment(token, nestingLevel));
    }

    private int nestingLevel(TreeContext ctx) {
      int nestingLevel = 0;
      boolean isInsideFunction = false;
      Iterator<Tree> ancestors = ctx.ancestors().descendingIterator();
      Tree parent = null;
      while (ancestors.hasNext()) {
        Tree t = ancestors.next();
        if (t instanceof FunctionDeclarationTree) {
          if (isInsideFunction || nestingLevel > 0) {
            nestingLevel++;
          }
          isInsideFunction = true;
        } else if ((t instanceof IfTree && !isElseIfBranch(parent, t)) || t instanceof MatchTree || t instanceof LoopTree || t instanceof CatchTree) {
          nestingLevel++;
        } else if (t instanceof ClassDeclarationTree) {
          nestingLevel = 0;
          isInsideFunction = false;
        }
        parent = t;
      }
      return nestingLevel;
    }

    private boolean isElseIfBranch(@Nullable Tree parent, Tree t) {
      return parent instanceof IfTree && ((IfTree) parent).elseBranch() == t;
    }

  }

}
