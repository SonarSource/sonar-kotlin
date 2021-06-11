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
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.checks.UnusedPrivateMethodCheck;
import org.sonarsource.slang.impl.NativeTreeImpl;

/**
 * @deprecated replaced by {@link org.sonarsource.kotlin.checks.UnusedPrivateMethodCheck}
 */
@Deprecated
@Rule(key = "S1144")
public class UnusedPrivateMethodKotlinCheck extends UnusedPrivateMethodCheck {

  // Serializable method should not raise any issue in Kotlin.
  private static final Set<String> IGNORED_METHODS = new HashSet<>(Arrays.asList(
    "writeObject",
    "readObject",
    "writeReplace",
    "readResolve",
    "readObjectNoData"));

  @Override
  protected boolean isValidPrivateMethod(FunctionDeclarationTree method) {
    return super.isValidPrivateMethod(method) &&
      // Functions with "operator" modifier will not be used by identifiers, resulting in false positives.
      // This modifier is not mapped to Slang, we have to use the native Kotlin AST to detect it.
      method.modifiers().stream().filter(mod -> mod instanceof NativeTreeImpl)
        .map(m -> ((NativeTreeImpl) m).nativeKind().toString())
        .noneMatch("LeafPsiElement[operator]"::equals);
  }

  @Override
  protected boolean isUnusedMethod(@Nullable IdentifierTree identifier, Set<String> usedIdentifierNames) {
    return identifier != null && super.isUnusedMethod(identifier, usedIdentifierNames) && !IGNORED_METHODS.contains(identifier.name());
  }

}
