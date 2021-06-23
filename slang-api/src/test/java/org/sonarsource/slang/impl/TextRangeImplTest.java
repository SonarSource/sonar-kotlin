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
package org.sonarsource.slang.impl;

import org.sonarsource.slang.api.TextPointer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextRangeImplTest {

  private TextPointer p1 = new TextPointerImpl(1, 2);
  private TextPointer p2 = new TextPointerImpl(3, 4);

  @Test
  void test_equals() {
    TextRangeImpl range1 = new TextRangeImpl(p1, p2);
    assertThat(range1)
      .isEqualTo(range1)
      .isEqualTo(new TextRangeImpl(p1, p2))
      .isNotEqualTo(new TextRangeImpl(p1, p1))
      .isNotEqualTo(new TextRangeImpl(p2, p2))
      .isNotEqualTo(null)
      .isNotEqualTo("");
  }

  @Test
  void test_hashCode() {
    assertThat(new TextRangeImpl(p1, p2).hashCode()).isEqualTo(new TextRangeImpl(p1, p2).hashCode());
    assertThat(new TextRangeImpl(p1, p2).hashCode()).isNotEqualTo(new TextRangeImpl(p1, p1).hashCode());
  }

  @Test
  void test_toString() {
    assertThat(new TextRangeImpl(p1, p2)).hasToString("TextRange[1, 2, 3, 4]");
  }
}
