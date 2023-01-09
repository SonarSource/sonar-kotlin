/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
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
package org.sonarsource.kotlin.testing

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import org.sonar.api.batch.fs.TextRange

class TextRangeAssert(actual: TextRange?) : AbstractAssert<TextRangeAssert?, TextRange?>(actual, TextRangeAssert::class.java) {
    fun hasRange(startLine: Int, startLineOffset: Int, endLine: Int, endLineOffset: Int): TextRangeAssert {
        isNotNull
        actual!!.let { actualNotNull: TextRange ->
            assertThat(actualNotNull.start().line()).isEqualTo(startLine)
            assertThat(actualNotNull.start().lineOffset()).isEqualTo(startLineOffset)
            assertThat(actualNotNull.end().line()).isEqualTo(endLine)
            assertThat(actualNotNull.end().lineOffset()).isEqualTo(endLineOffset)
        }
        return this
    }
}

fun assertTextRange(actual: TextRange?): TextRangeAssert {
    return TextRangeAssert(actual)
}
