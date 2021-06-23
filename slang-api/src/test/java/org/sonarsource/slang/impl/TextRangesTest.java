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

import org.sonarsource.slang.api.TextRange;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonarsource.slang.impl.TextRanges.range;
import static org.assertj.core.api.Assertions.assertThat;

class TextRangesTest {

  @Test
  void merge_not_empty_list() {
    assertThat(merge(range(1, 2, 3, 4))).isEqualTo(range(1, 2, 3, 4));
    assertThat(merge(range(1, 2, 3, 4), range(5, 1, 5, 7))).isEqualTo(range(1, 2, 5, 7));
    assertThat(merge(range(1, 2, 3, 4), range(1, 3, 1, 5))).isEqualTo(range(1, 2, 3, 4));
  }

  @Test
  void merge_empty_list() {
    assertThrows(IllegalArgumentException.class,
      TextRangesTest::merge);
  }

  private static TextRange merge(TextRange... ranges) {
    return TextRanges.merge(Arrays.asList(ranges));
  }

}
