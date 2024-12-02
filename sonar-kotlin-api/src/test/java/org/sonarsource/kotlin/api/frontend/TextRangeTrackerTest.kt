/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.api.frontend

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.testapi.DummyInputFile
import org.sonarsource.kotlin.testapi.kotlinTreeOf
import org.assertj.core.api.Assertions
import org.assertj.core.api.ObjectAssert
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.junit.jupiter.api.AfterEach
import java.util.TreeMap

private const val TQ = "\"\"\""

private data class Range(val startIndex: Int, val endIndex: Int)

private class TextRangeTrackerAssert(actual: TextRangeTracker) : ObjectAssert<TextRangeTracker>(actual) {
    fun hasRangesAtIndexes(vararg expected: Pair<Int, Range>): TextRangeTrackerAssert {
        val expectedNavigable = TreeMap(expected.toMap())
        for (i in expectedNavigable.firstKey()..expectedNavigable.lastKey()) {
            val (actualRealIndex, actualRange) = actual.rangeAtIndex(i)
            val flooredExpectedKey = expectedNavigable.floorKey(i)

            val expectedRange = expectedNavigable[flooredExpectedKey]!!

            Assertions.assertThat(actualRealIndex)
                .withFailMessage("Expected floored index of $flooredExpectedKey for index $i but was $actualRealIndex")
                .isEqualTo(flooredExpectedKey)
            Assertions.assertThat(actualRange.start().line())
                .withFailMessage("Expected start line for range @ $i ($actualRealIndex) to be 2 but was ${actualRange.start().line()}")
                .isEqualTo(2)
            Assertions.assertThat(actualRange.start().lineOffset())
                .withFailMessage(
                    "Expected start offset for range @ $i ($actualRealIndex) to be ${expectedRange.startIndex} " +
                        "but was ${actualRange.start().lineOffset()}"
                )
                .isEqualTo(expectedRange.startIndex)
            Assertions.assertThat(actualRange.end().line())
                .withFailMessage(
                    "Expected end line for range @ $i ($actualRealIndex) to be 2 but was ${actualRange.end().line()}"
                )
                .isEqualTo(2)
            Assertions.assertThat(actualRange.end().lineOffset())
                .withFailMessage(
                    "Expected end offset for range @ $i ($actualRealIndex) to be ${expectedRange.endIndex} " +
                        "but was ${actualRange.end().lineOffset()}"
                )
                .isEqualTo(expectedRange.endIndex)
        }

        return this
    }
}

private fun assertThat(actual: TextRangeTracker) = TextRangeTrackerAssert(actual)

internal class TextRangeTrackerTest {
    private val disposer = Disposer.newDisposable()

    @AfterEach
    fun dispose() {
        Disposer.dispose(disposer)
    }

    @Test
    fun `single-quoted with new line`() {
        assertThat(textRangeTrackerOf(""""hello\nworld"""")).hasRangesAtIndexes(
            0 to Range(1, 6),
            5 to Range(6, 8),
            7 to Range(8, 13),
        )
    }

    @Test
    fun `single-quoted with escaped new line`() {
        assertThat(textRangeTrackerOf(""""hello\\nworld"""")).hasRangesAtIndexes(
            0 to Range(1, 6),
            5 to Range(6, 8),
            7 to Range(8, 14),
        )
    }

    @Test
    fun `triple-quoted with new line`() {
        assertThat(textRangeTrackerOf("""${TQ}hello\nworld$TQ""")).hasRangesAtIndexes(
            0 to Range(3, 8),
            5 to Range(8, 9),
            7 to Range(9, 15),
        )
    }

    @Test
    fun `triple-quoted with escaped new line`() {
        assertThat(textRangeTrackerOf("""${TQ}hello\\nworld$TQ""")).hasRangesAtIndexes(
            0 to Range(3, 8),
            5 to Range(8, 9),
            7 to Range(9, 10),
            9 to Range(10, 16),
        )
    }

    @Test
    fun `single-quoted with concatination`() {
        assertThat(textRangeTrackerOf(""""hello" + "world"""")).hasRangesAtIndexes(
            0 to Range(1, 6),
            5 to Range(11, 16),
        )
    }

    @Test
    fun `triple-quoted with concatination`() {
        assertThat(textRangeTrackerOf("""${TQ}hello$TQ + ${TQ}world$TQ""")).hasRangesAtIndexes(
            0 to Range(3, 8),
            5 to Range(17, 22),
        )
    }

    private fun textRangeTrackerOf(regex: String): TextRangeTracker {
        val tree = kotlinTreeOf(
            """
            val x = 
            $regex
        """.trimIndent(),
            environment = Environment(disposer, emptyList(), LanguageVersion.LATEST_STABLE),
            inputFile = DummyInputFile()
        )

        val entries = tree.psiFile.collectDescendantsOfType<KtStringTemplateExpression>()
            .flatMap { it.entries.asSequence() }

        return TextRangeTracker.of(entries, DummyInputFile(), tree.document)
    }

}
