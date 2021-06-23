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
package org.sonarsource.slang.impl.literal;

import org.junit.jupiter.api.Test;
import org.sonarsource.slang.impl.StringLiteralTreeImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringLiteralTreeImplTest {

  @Test
  void test() {
    StringLiteralTreeImpl stringLiteral = new StringLiteralTreeImpl(null, "\"abc\"");
    assertThat(stringLiteral.value()).isEqualTo("\"abc\"");
    assertThat(stringLiteral.content()).isEqualTo("abc");
    assertThat(stringLiteral.children()).isEmpty();
  }

  @Test
  void test_failure() {
    assertThrows(IllegalArgumentException.class,
      () -> new StringLiteralTreeImpl(null, "abc"));
  }

  @Test
  void test_explicit_content() {
    StringLiteralTreeImpl stringLiteral = new StringLiteralTreeImpl(null, "abc", "abc");
    assertThat(stringLiteral.value()).isEqualTo("abc");
    assertThat(stringLiteral.content()).isEqualTo("abc");
    assertThat(stringLiteral.children()).isEmpty();
  }

}
