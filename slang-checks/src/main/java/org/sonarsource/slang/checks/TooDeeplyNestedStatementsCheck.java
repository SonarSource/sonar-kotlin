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

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonarsource.slang.api.ExceptionHandlingTree;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.MatchTree;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.checks.utils.ExpressionUtils;
import org.sonarsource.slang.checks.utils.Language;
import org.sonarsource.slang.checks.utils.PropertyDefaultValue;

@Rule(key = "S134")
public class TooDeeplyNestedStatementsCheck implements SlangCheck {
  private static final int DEFAULT_MAX_DEPTH = 3;
  private static final String DEFAULT_MAX_DEPTH_VALUE = "" + DEFAULT_MAX_DEPTH;

  @RuleProperty(
    key = "max",
    description = "Maximum allowed control flow statement nesting depth"
  )
  @PropertyDefaultValue(language = Language.KOTLIN, defaultValue = DEFAULT_MAX_DEPTH_VALUE)
  @PropertyDefaultValue(language = Language.RUBY, defaultValue = DEFAULT_MAX_DEPTH_VALUE)
  @PropertyDefaultValue(language = Language.SCALA, defaultValue = DEFAULT_MAX_DEPTH_VALUE)
  @PropertyDefaultValue(language = Language.GO, defaultValue = "" + Language.GO_NESTED_STATEMENT_MAX_DEPTH)
  public int max = DEFAULT_MAX_DEPTH;

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, this::checkNestedDepth);
    init.register(LoopTree.class, this::checkNestedDepth);
    init.register(MatchTree.class, this::checkNestedDepth);
    init.register(ExceptionHandlingTree.class, this::checkNestedDepth);
  }

  private void checkNestedDepth(CheckContext ctx, Tree tree) {
    if (isElseIfStatement(ctx.parent(), tree)) {
      // Ignore 'else-if' statements since the issue would already be raised on the first 'if' statement
      return;
    }
    if (ExpressionUtils.isTernaryOperator(ctx.ancestors(), tree)) {
      return;
    }

    Iterator<Tree> iterator = ctx.ancestors().iterator();
    Deque<Token> nestedParentNodes = new LinkedList<>();
    Tree last = tree;

    while (iterator.hasNext()) {
      Tree parent = iterator.next();
      if (isElseIfStatement(parent, last) && !nestedParentNodes.isEmpty()) {
        // Only the 'if' parent of the chained 'else-if' statements should be highlighted
        nestedParentNodes.removeLast();
      }
      if (parent instanceof LoopTree || parent instanceof ExceptionHandlingTree || parent instanceof IfTree || parent instanceof MatchTree) {
        nestedParentNodes.addLast(getNodeToHighlight(parent));
      }
      if (nestedParentNodes.size() > max) {
        return;
      }
      last = parent;
    }

    if (nestedParentNodes.size() == max) {
      reportIssue(ctx, tree, nestedParentNodes);
    }
  }

  private static boolean isElseIfStatement(@Nullable Tree parent, @Nullable Tree tree) {
    return tree instanceof IfTree && parent instanceof IfTree && tree.equals(((IfTree) parent).elseBranch());
  }

  private void reportIssue(CheckContext ctx, Tree statement, Deque<Token> nestedStatements) {
    String message = String.format("Refactor this code to not nest more than %s control flow statements.", max);
    List<SecondaryLocation> secondaryLocations = new ArrayList<>(nestedStatements.size());
    int nestedDepth = 0;

    while (!nestedStatements.isEmpty()) {
      nestedDepth++;
      String secondaryLocationMessage = String.format("Nesting depth %s", nestedDepth);
      secondaryLocations.add(new SecondaryLocation(nestedStatements.removeLast().textRange(), secondaryLocationMessage));
    }

    Token nodeToHighlight = getNodeToHighlight(statement);
    ctx.reportIssue(nodeToHighlight, message, secondaryLocations);
  }

  private static Token getNodeToHighlight(Tree tree) {
    if (tree instanceof IfTree) {
      return ((IfTree) tree).ifKeyword();
    } else if (tree instanceof MatchTree) {
      return ((MatchTree) tree).keyword();
    } else if (tree instanceof ExceptionHandlingTree) {
      return ((ExceptionHandlingTree) tree).tryKeyword();
    } else {
      return ((LoopTree) tree).keyword();
    }
  }

}
