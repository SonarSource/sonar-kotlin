/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.plugin

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
