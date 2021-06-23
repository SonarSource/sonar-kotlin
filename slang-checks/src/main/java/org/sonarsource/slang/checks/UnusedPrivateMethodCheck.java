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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.checks.utils.FunctionUtils;
import org.sonarsource.slang.utils.SyntacticEquivalence;

import static org.sonarsource.slang.utils.SyntacticEquivalence.getUniqueIdentifier;

@Rule(key = "S1144")
public class UnusedPrivateMethodCheck implements SlangCheck {

  @Override
  public void initialize(InitContext init) {
    init.register(ClassDeclarationTree.class, this::processClassDeclaration);
  }

  protected void processClassDeclaration(CheckContext context, ClassDeclarationTree classDeclarationTree) {
    // only verify the outermost class in the file, to avoid raising the same issue multiple times
    if (context.ancestors().stream().noneMatch(ClassDeclarationTree.class::isInstance)) {
      reportUnusedPrivateMethods(context, classDeclarationTree);
    }
  }

  protected void reportUnusedPrivateMethods(CheckContext context, ClassDeclarationTree classDeclarationTree) {
    MethodAndIdentifierCollector methodAndIdentifierCollector = new MethodAndIdentifierCollector(classDeclarationTree.descendants());
    methodAndIdentifierCollector.getMethodDeclarations().stream()
      .filter(this::isValidPrivateMethod)
      .forEach(tree -> {
        IdentifierTree identifier = tree.name();
        if (identifier != null && isUnusedMethod(identifier, methodAndIdentifierCollector.getUsedUniqueIdentifiers())) {
          String message = String.format("Remove this unused private \"%s\" method.", identifier.name());
          context.reportIssue(tree.rangeToHighlight(), message);
        }
      });
  }

  protected boolean isValidPrivateMethod(FunctionDeclarationTree method) {
    return FunctionUtils.isPrivateMethod(method) && !FunctionUtils.isOverrideMethod(method);
  }

  protected boolean isUnusedMethod(IdentifierTree identifier, Set<String> usedIdentifierNames) {
    return !usedIdentifierNames.contains(getUniqueIdentifier(identifier));
  }

  protected static class MethodAndIdentifierCollector {
    private Set<FunctionDeclarationTree> methodDeclarations = new HashSet<>();
    private Set<String> usedUniqueIdentifiers;

    Set<FunctionDeclarationTree> getMethodDeclarations() {
      return methodDeclarations;
    }
    public Set<String> getUsedUniqueIdentifiers() {
      return usedUniqueIdentifiers;
    }

    public MethodAndIdentifierCollector(Stream<Tree> descendants) {
      Set<IdentifierTree> usedIdentifiers = new HashSet<>();
      descendants.forEach(tree -> {
        if (tree instanceof FunctionDeclarationTree && !((FunctionDeclarationTree)tree).isConstructor()) {
          methodDeclarations.add(((FunctionDeclarationTree) tree));
        } else if (tree instanceof IdentifierTree) {
          usedIdentifiers.add((IdentifierTree) tree);
        }
      });

      usedIdentifiers.removeAll(methodDeclarations.stream()
        .map(FunctionDeclarationTree::name)
        .collect(Collectors.toSet()));

      usedUniqueIdentifiers = usedIdentifiers.stream()
        .filter(Objects::nonNull)
        .map(SyntacticEquivalence::getUniqueIdentifier)
        .collect(Collectors.toCollection(HashSet::new));
    }

  }
}
