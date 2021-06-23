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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.visitors.TreeContext;
import org.sonarsource.slang.visitors.TreeVisitor;

import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;

@Rule(key = "S4144")
public class DuplicatedFunctionImplementationCheck implements SlangCheck {

  private static final String MESSAGE = "Update this function so that its implementation is not identical to \"%s\" on line %s.";
  private static final String MESSAGE_NO_NAME = "Update this function so that its implementation is not identical to the one on line %s.";
  private static final int MINIMUM_STATEMENTS_COUNT = 2;

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) -> {
      Map<Tree, List<FunctionDeclarationTree>> functionsByParents = new HashMap<>();
      TreeVisitor<TreeContext> functionVisitor = new TreeVisitor<>();
      functionVisitor.register(FunctionDeclarationTree.class, (functionCtx, functionDeclarationTree) -> {
        if (!functionDeclarationTree.isConstructor()) {
          functionsByParents
            .computeIfAbsent(functionCtx.ancestors().peek(), key -> new ArrayList<>())
            .add(functionDeclarationTree);
        }
      });
      functionVisitor.scan(new TreeContext(), tree);

      for (Map.Entry<Tree, List<FunctionDeclarationTree>> entry : functionsByParents.entrySet()) {
        check(ctx, entry.getValue());
      }
    });
  }

  private static void check(CheckContext ctx, List<FunctionDeclarationTree> functionDeclarations) {
    Set<FunctionDeclarationTree> reportedDuplicates = new HashSet<>();
    IntStream.range(0, functionDeclarations.size()).forEach(i -> {
      FunctionDeclarationTree original = functionDeclarations.get(i);
      functionDeclarations.stream()
        .skip(i + 1L)
        .filter(f -> !reportedDuplicates.contains(f))
        .filter(DuplicatedFunctionImplementationCheck::hasMinimumSize)
        .filter(f -> areDuplicatedImplementation(original, f))
        .forEach(duplicate -> {
          reportDuplicate(ctx, original, duplicate);
          reportedDuplicates.add(duplicate);
        });
    });

  }

  private static boolean hasMinimumSize(FunctionDeclarationTree function) {
    BlockTree functionBody = function.body();
    if (functionBody == null) {
      return false;
    }
    return functionBody.statementOrExpressions().size() >= MINIMUM_STATEMENTS_COUNT;
  }

  private static boolean areDuplicatedImplementation(FunctionDeclarationTree original, FunctionDeclarationTree possibleDuplicate) {
    return areEquivalent(original.nativeChildren(), possibleDuplicate.nativeChildren())
      && areEquivalent(original.formalParameters(), possibleDuplicate.formalParameters())
      && areEquivalent(original.body(), possibleDuplicate.body());
  }

  private static void reportDuplicate(CheckContext ctx, FunctionDeclarationTree original, FunctionDeclarationTree duplicate) {
    IdentifierTree identifier = original.name();
    int line = original.metaData().textRange().start().line();
    String message;
    Tree secondaryTree;
    if (identifier != null) {
      secondaryTree = identifier;
      message = String.format(MESSAGE, identifier.name(), line);
    } else {
      secondaryTree = original;
      message = String.format(MESSAGE_NO_NAME, line);
    }
    SecondaryLocation secondaryLocation = new SecondaryLocation(secondaryTree, "original implementation");
    IdentifierTree duplicateIdentifier = duplicate.name();
    Tree primaryTree = duplicateIdentifier != null ? duplicateIdentifier : duplicate;
    ctx.reportIssue(primaryTree, message, secondaryLocation);
  }

}
