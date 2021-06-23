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

import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonarsource.slang.checks.utils.Language;
import org.sonarsource.slang.checks.utils.PropertyDefaultValue;

@Rule(key = "S138")
public class TooLongFunctionCheck implements SlangCheck {

  private static final int DEFAULT_MAX = 100;
  private static final String DEFAULT_MAX_VALUE = "" + DEFAULT_MAX;

  @RuleProperty(
    key = "max",
    description = "Maximum authorized lines of code in a function"
  )
  @PropertyDefaultValue(language = Language.KOTLIN, defaultValue = DEFAULT_MAX_VALUE)
  @PropertyDefaultValue(language = Language.RUBY, defaultValue = DEFAULT_MAX_VALUE)
  @PropertyDefaultValue(language = Language.SCALA, defaultValue = DEFAULT_MAX_VALUE)
  @PropertyDefaultValue(language = Language.GO, defaultValue = "" + Language.GO_DEFAULT_MAXIMUM_FUNCTION_LENGTH)
  public int max = DEFAULT_MAX;

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, tree) -> {
      BlockTree body = tree.body();
      if (body == null) {
        return;
      }
      int numberOfLinesOfCode = body.metaData().linesOfCode().size();
      if (numberOfLinesOfCode > max) {
        String message = String.format(
          "This function has %s lines of code, which is greater than the %s authorized. Split it into smaller functions.",
          numberOfLinesOfCode,
          max);
        ctx.reportIssue(tree.rangeToHighlight(), message);
      }
    });
  }

}
