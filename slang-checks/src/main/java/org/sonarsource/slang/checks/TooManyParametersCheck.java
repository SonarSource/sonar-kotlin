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
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.ModifierTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SecondaryLocation;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S107")
public class TooManyParametersCheck implements SlangCheck {

  private static final int DEFAULT_MAX = 7;

  @RuleProperty(
    key = "Max",
    description = "Maximum authorized number of parameters",
    defaultValue = "" + DEFAULT_MAX
  )
  public int max = DEFAULT_MAX;

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, tree) -> {
      if (isCandidateMethod(tree)) {
        String message = String.format(
          "This function has %s parameters, which is greater than the %s authorized.",
          tree.formalParameters().size(),
          max);
        List<SecondaryLocation> secondaryLocations = tree.formalParameters().stream()
          .skip(max)
          .map(SecondaryLocation::new)
          .collect(Collectors.toList());

        if (tree.name() == null) {
          ctx.reportIssue(tree, message, secondaryLocations);
        } else {
          ctx.reportIssue(tree.name(), message, secondaryLocations);
        }
      }
    });
  }

  protected boolean isCandidateMethod(FunctionDeclarationTree functionDeclarationTree) {
    return !functionDeclarationTree.isConstructor()
      && !isOverrideMethod(functionDeclarationTree)
      && functionDeclarationTree.formalParameters().size() > max;
  }

  private static boolean isOverrideMethod(FunctionDeclarationTree tree) {
    return tree.modifiers().stream().anyMatch(mod -> {
      if (!(mod instanceof ModifierTree)) {
        return false;
      }
      return ((ModifierTree) mod).kind() == ModifierTree.Kind.OVERRIDE;
    });
  }

}
