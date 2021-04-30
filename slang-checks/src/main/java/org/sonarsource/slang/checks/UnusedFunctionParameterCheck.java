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

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;

import static org.sonarsource.slang.checks.utils.FunctionUtils.isOverrideMethod;
import static org.sonarsource.slang.checks.utils.FunctionUtils.isPrivateMethod;
import static org.sonarsource.slang.utils.SyntacticEquivalence.areEquivalent;

@Rule(key = "S1172")
public class UnusedFunctionParameterCheck implements SlangCheck {

  // Currently we ignore all functions named "main", however this should be configurable based on the analyzed language in the future.
  protected static final Pattern IGNORED_PATTERN = Pattern.compile("main", Pattern.CASE_INSENSITIVE);

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, functionDeclarationTree) -> {
      if (functionDeclarationTree.isConstructor() || shouldBeIgnored(ctx, functionDeclarationTree)) {
        return;
      }

      List<ParameterTree> unusedParameters = getUnusedParameters(functionDeclarationTree);

      if (unusedParameters.isEmpty()) {
        return;
      }

      reportUnusedParameters(ctx, unusedParameters);
    });
  }

  protected static List<ParameterTree> getUnusedParameters(FunctionDeclarationTree functionDeclarationTree) {
    return functionDeclarationTree.formalParameters().stream()
      .filter(ParameterTree.class::isInstance)
      .map(ParameterTree.class::cast)
      .filter(parameterTree -> parameterTree.modifiers().isEmpty() && functionDeclarationTree.descendants()
        .noneMatch(tree -> !tree.equals(parameterTree.identifier()) && areEquivalent(tree, parameterTree.identifier())))
      .collect(Collectors.toList());
  }

  protected void reportUnusedParameters(CheckContext ctx, List<ParameterTree> unusedParameters) {
    List<SecondaryLocation> secondaryLocations = unusedParameters.stream()
      .map(unusedParameter ->
        new SecondaryLocation(unusedParameter.identifier(), "Remove this unused method parameter " + unusedParameter.identifier().name() + "\"."))
      .collect(Collectors.toList());

    IdentifierTree firstUnused = unusedParameters.get(0).identifier();
    String msg;

    if (unusedParameters.size() > 1) {
      msg = "Remove these unused function parameters.";
    } else {
      msg = "Remove this unused function parameter \"" + firstUnused.name() + "\".";
    }

    ctx.reportIssue(firstUnused, msg, secondaryLocations);
  }

  protected boolean isValidFunctionForRule(CheckContext ctx, FunctionDeclarationTree tree) {
    return ctx.parent() instanceof TopLevelTree || (isPrivateMethod(tree) && !isOverrideMethod(tree));
  }

  protected boolean shouldBeIgnored(CheckContext ctx, FunctionDeclarationTree tree) {
    IdentifierTree name = tree.name();
    boolean validFunctionForRule = isValidFunctionForRule(ctx, tree);
    return !validFunctionForRule
      || tree.body() == null
      || (name != null && IGNORED_PATTERN.matcher(name.name()).matches());
  }

}
