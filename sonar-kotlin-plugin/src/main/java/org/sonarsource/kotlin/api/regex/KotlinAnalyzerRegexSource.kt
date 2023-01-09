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
package org.sonarsource.kotlin.api.regex

import org.jetbrains.kotlin.com.intellij.openapi.editor.Document
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextRange
import org.sonarsource.analyzer.commons.regex.ast.IndexRange
import org.sonarsource.analyzer.commons.regex.java.JavaRegexSource
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import java.util.NavigableMap
import java.util.TreeMap

/**
 * This class will translate a list of Kotlin AST nodes (string template expressions) to something the regex engine will understand.
 *
 * TODO: String interpolation is currently not handled
 * TODO: multi-line strings with trimMargin() and trimIndent() are currently not handled.
 */
class KotlinAnalyzerRegexSource(
    sourceTemplateEntries: Iterable<KtStringTemplateEntry>,
    inputFile: InputFile,
    document: Document,
) : JavaRegexSource(templateEntriesAsString(sourceTemplateEntries)) {
    val textRangeTracker = TextRangeTracker.of(sourceTemplateEntries, inputFile, document)
}

private fun templateEntriesAsString(entries: Iterable<KtStringTemplateEntry>) = entries.joinToString("") {
    if (isUnescapedEscapeChar(it)) """\\""" else it.text
}

fun isUnescapedEscapeChar(entry: KtStringTemplateEntry) = entry is KtLiteralStringTemplateEntry && entry.text == """\"""

class TextRangeTracker private constructor(
    private val regexIndexToTextRange: NavigableMap<Int, TextRange>,
    private val textRangeToKtNode: Map<TextRange, KtStringTemplateEntry>,
    private val textRange: (startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) -> TextRange,
) {
    companion object {
        fun of(
            stringTemplateEntries: Iterable<KtStringTemplateEntry>,
            inputFile: InputFile,
            document: Document,
        ): TextRangeTracker {
            var endIndex = 0
            val regexIndexToTextRange = TreeMap<Int, TextRange>()
            val textRangeToKtNode = stringTemplateEntries
                .associateBy { entry ->
                    val textRange = inputFile.textRange(document, entry)
                    regexIndexToTextRange[endIndex] = textRange

                    endIndex += if (isUnescapedEscapeChar(entry)) 2 else entry.textLength
                    textRange
                }
            return TextRangeTracker(regexIndexToTextRange, textRangeToKtNode) { startLine, startColumn, endLine, endColumn ->
                inputFile.newRange(startLine, startColumn, endLine, endColumn)
            }

        }
    }

    fun rangeAtIndex(index: Int) = regexIndexToTextRange.floorEntry(index).toPair()

    fun rangeBeforeIndex(index: Int) = regexIndexToTextRange.lowerEntry(index).toPair()

    fun textRangesBetween(range: IndexRange) = textRangesBetween(range.beginningOffset, range.endingOffset)

    tailrec fun textRangesBetween(startIndex: Int, endIndex: Int, acc: MutableList<TextRange> = mutableListOf()): Collection<TextRange> {
        if (startIndex >= endIndex) return emptyList()

        val (startRangeIndex, start) = if (startIndex < 0) rangeAtIndex(0) else rangeAtIndex(startIndex)
        val startOffset: Int = startIndex - startRangeIndex
        val (endRangeIndex, end) = if (endIndex > 0) rangeBeforeIndex(endIndex) else startRangeIndex to start
        val endOffset: Int = endIndex - endRangeIndex

        return if (start == end) {
            acc + textRange(
                start.start().line(),
                start.start().lineOffset() + startOffset,
                start.end().line(),
                start.start().lineOffset() + endOffset
            )
        } else {
            textRangesBetween(regexIndexToTextRange.higherKey(startRangeIndex), endIndex, acc.apply {
                add(textRange(start.start().line(), start.start().lineOffset() + startOffset, start.end().line(), start.end().lineOffset()))
            })
        }
    }
}
