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

import javax.annotation.Nullable;
import org.assertj.core.api.AbstractAssert;
import org.sonar.api.batch.fs.TextRange;

import static org.assertj.core.api.Assertions.assertThat;

public class TextRangeAssert extends AbstractAssert<TextRangeAssert, TextRange> {

  public TextRangeAssert(@Nullable TextRange actual) {
    super(actual, TextRangeAssert.class);
  }

  public TextRangeAssert hasRange(int startLine, int startLineOffset, int endLine, int endLineOffset) {
    isNotNull();
    assertThat(actual.start().line()).isEqualTo(startLine);
    assertThat(actual.start().lineOffset()).isEqualTo(startLineOffset);
    assertThat(actual.end().line()).isEqualTo(endLine);
    assertThat(actual.end().lineOffset()).isEqualTo(endLineOffset);
    return this;
  }

  public static TextRangeAssert assertTextRange(@Nullable TextRange actual) {
    return new TextRangeAssert(actual);
  }

}
