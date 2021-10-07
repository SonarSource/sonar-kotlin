/*
 * SonarSource Kotlin
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
package org.sonarsource.kotlin.api.regex

import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.sonar.api.batch.fs.TextRange
import org.sonarsource.analyzer.commons.regex.java.JavaRegexSource
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext
import java.util.NavigableMap
import java.util.TreeMap

/**
 * This class will translate a list of Kotlin AST nodes (string template expressions) to something the regex engine will understand.
 *
 * TODO: String interpolation is currently not handled
 * TODO: multi-line strings with trimMargin() and trimIndent() are currently not handled.
 */
class KotlinAnalyzerRegexSource(
    sourceTemplates: Iterable<KtStringTemplateExpression>,
    kotlinFileContext: KotlinFileContext,
) : JavaRegexSource(templatesAsString(sourceTemplates)) {
    val textRangeTracker = TextRangeTracker.of(sourceTemplates, kotlinFileContext)
}

private fun templatesAsString(templates: Iterable<KtStringTemplateExpression>) = templates.joinToString("") { template ->
    template.entries.joinToString("") {
        if (isUnescapedEscapeChar(it)) """\\""" else it.text
    }
}

fun isUnescapedEscapeChar(entry: KtStringTemplateEntry) = entry is KtLiteralStringTemplateEntry && entry.text == """\"""

class TextRangeTracker private constructor(
    private val regexIndexToTextRange: NavigableMap<Int, TextRange>,
    private val textRangeToKtNode: Map<TextRange, KtStringTemplateEntry>,
) {
    companion object {
        fun of(
            stringTemplates: Iterable<KtStringTemplateExpression>,
            kotlinFileContext: KotlinFileContext
        ): TextRangeTracker {
            var endIndex = 0
            val regexIndexToTextRange = TreeMap<Int, TextRange>()
            val textRangeToKtNode = stringTemplates
                .flatMap { it.entries.asSequence() }
                .associateBy { entry ->
                    val textRange = kotlinFileContext.textRange(entry)
                    regexIndexToTextRange[endIndex] = textRange

                    endIndex += if(isUnescapedEscapeChar(entry)) 2 else entry.textLength
                    textRange
                }
            return TextRangeTracker(regexIndexToTextRange, textRangeToKtNode)
        }
    }

    fun rangeAtIndex(index: Int) = regexIndexToTextRange.floorEntry(index)?.toPair()

    fun textRangesBetween(startIndex: Int, endIndex: Int): Collection<TextRange> = regexIndexToTextRange.subMap(startIndex, endIndex).values
}
