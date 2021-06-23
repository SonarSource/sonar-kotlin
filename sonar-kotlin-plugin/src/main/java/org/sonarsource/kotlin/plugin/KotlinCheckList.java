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
package org.sonarsource.kotlin.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonarsource.kotlin.checks.TooManyParametersKotlinCheck;
import org.sonarsource.kotlin.checks.UnusedPrivateMethodKotlinCheck;
import org.sonarsource.slang.checks.CheckList;
import org.sonarsource.slang.checks.MatchWithoutElseCheck;
import org.sonarsource.slang.checks.OctalValuesCheck;
import org.sonarsource.slang.checks.TooManyParametersCheck;
import org.sonarsource.slang.checks.UnusedPrivateMethodCheck;

public final class KotlinCheckList {

  private KotlinCheckList() {
    // utility class
  }

  static final Class[] KOTLIN_CHECK_BLACK_LIST = {
    // FP rate too high for now in Kotlin on 'when' statements due to enum/sealed class that have all branches covered
    MatchWithoutElseCheck.class,
    // Rule does not apply here as octal values do not exist in Kotlin
    OctalValuesCheck.class,
    // Language specific implementation is provided.
    UnusedPrivateMethodCheck.class,
    TooManyParametersCheck.class
  };

  static final List<Class<?>> KOTLIN_LANGUAGE_SPECIFIC_CHECKS = Arrays.asList(
    UnusedPrivateMethodKotlinCheck.class,
    TooManyParametersKotlinCheck.class
  );

  public static List<Class<?>> checks() {
    List<Class<?>> list = new ArrayList<>(CheckList.excludeChecks(KOTLIN_CHECK_BLACK_LIST));
    list.addAll(KOTLIN_LANGUAGE_SPECIFIC_CHECKS);
    return list;
  }
}
