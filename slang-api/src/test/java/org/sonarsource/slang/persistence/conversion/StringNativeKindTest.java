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
package org.sonarsource.slang.persistence.conversion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringNativeKindTest {

  @Test
  void constructor() {
    assertThat(new StringNativeKind("ast.Element")).isNotNull();
    assertThat(StringNativeKind.of("ast.Element")).isNotNull();
    assertThat(StringNativeKind.of(null)).isNull();
  }

  @Test
  void to_string() {
    assertThat(new StringNativeKind("ast.Element")).hasToString("ast.Element");
    assertThat(StringNativeKind.of("ast.Element")).hasToString("ast.Element");
    assertThat(StringNativeKind.toString(null)).isNull();
    assertThat(StringNativeKind.toString(new StringNativeKind("ast.Element"))).isEqualTo("ast.Element");
  }

  @Test
  void test_equals() {
    assertThat(new StringNativeKind("ast.Element")).isEqualTo(new StringNativeKind("ast.Element"));
    assertThat(new StringNativeKind("ast.Element").hashCode()).isEqualTo(new StringNativeKind("ast.Element").hashCode());
  }
  
}
