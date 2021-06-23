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
package org.sonarsource.slang.testing;

import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.impl.TextRanges;
import org.assertj.core.api.AbstractAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class RangeAssert extends AbstractAssert<RangeAssert, TextRange> {

  public RangeAssert(TextRange actual) {
    super(actual, RangeAssert.class);
  }

  public RangeAssert hasRange(int startLine, int startLineOffset, int endLine, int endLineOffset) {
    isNotNull();
    assertThat(actual).isEqualTo(TextRanges.range(startLine, startLineOffset, endLine, endLineOffset));
    return this;
  }

  public static RangeAssert assertRange(TextRange actual) {
    return new RangeAssert(actual);
  }

}
