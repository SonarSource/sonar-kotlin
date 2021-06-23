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
package org.sonarsource.slang.checks.utils;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageTest {

  @Test
  void default_scala_function_name() {
    Pattern pattern = Pattern.compile(Language.SCALA_FUNCTION_OR_OPERATOR_NAMING_DEFAULT);
    assertThat(pattern.matcher("print").matches()).isTrue();
    assertThat(pattern.matcher("printLn").matches()).isTrue();
    assertThat(pattern.matcher("method_=").matches()).isTrue();
    assertThat(pattern.matcher("parse_!").matches()).isTrue();
    assertThat(pattern.matcher("+").matches()).isTrue();
    assertThat(pattern.matcher("<<").matches()).isTrue();
    assertThat(pattern.matcher("print_ln").matches()).isFalse();
    assertThat(pattern.matcher("PRINT").matches()).isFalse();
    assertThat(pattern.matcher("_print").matches()).isFalse();
    assertThat(pattern.matcher("+print").matches()).isFalse();
  }
}
