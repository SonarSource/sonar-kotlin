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
package org.sonarsource.kotlin.checks;

import java.util.Arrays;
import java.util.List;

import org.sonar.check.Rule;
import org.sonarsource.slang.api.Annotation;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.checks.TooManyParametersCheck;

/**
 * @deprecated replaced by {@link org.sonarsource.kotlin.checks.TooManyParametersCheck}
 */
@Deprecated
@Rule(key = "S107")
public class TooManyParametersKotlinCheck extends TooManyParametersCheck {

  private static final List<String> EXCEPTIONS_LIST = Arrays.asList(
    "RequestMapping",
    "GetMapping",
    "PostMapping",
    "PutMapping",
    "DeleteMapping",
    "PatchMapping",
    "JsonCreator");

  @Override
  protected boolean isCandidateMethod(FunctionDeclarationTree functionDeclarationTree) {
    return super.isCandidateMethod(functionDeclarationTree) &&
      functionDeclarationTree.metaData().annotations().stream()
        .map(Annotation::shortName)
        .noneMatch(EXCEPTIONS_LIST::contains);
  }
}
