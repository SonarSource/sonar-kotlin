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
package org.sonarsource.kotlin.converter

import org.jetbrains.kotlin.com.intellij.openapi.editor.Document
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextPointer
import org.sonar.api.batch.fs.TextRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

object KotlinTextRanges {
    fun InputFile.textRange(psiDocument: Document, element: PsiElement): TextRange {
        val startPointer = textPointerAtOffset(psiDocument, element.textRange.startOffset)
        val endPointer = textPointerAtOffset(psiDocument, element.textRange.endOffset)
        return newRange(startPointer.line(), startPointer.lineOffset(), endPointer.line(), endPointer.lineOffset())
    }

    fun InputFile.textPointerAtOffset(psiDocument: Document, startOffset: Int): TextPointer {
        val startLineNumber = psiDocument.getLineNumber(startOffset)
        val startLineNumberOffset = psiDocument.getLineStartOffset(startLineNumber)
        val startLineOffset = startOffset - startLineNumberOffset

        return newPointer(startLineNumber + 1, startLineOffset)
    }
    
    fun KotlinFileContext.textRange(psiElement: PsiElement) =
        inputFileContext.inputFile.textRange(ktFile.viewProvider.document!!, psiElement)

    fun KotlinFileContext.textRange(startLine: Int, startOffset: Int, endLine: Int, endOffset: Int): TextRange =
        inputFileContext.inputFile.newRange(startLine, startOffset, endLine, endOffset)

    operator fun TextRange.contains(other: TextRange) = this.start() <= other.start() && this.end() >= other.end()

    fun InputFile.merge(ranges: Iterable<TextRange>): TextRange =
        newRange(
            ranges.map { it.start() }.minOrNull() ?: throw IllegalArgumentException("Can't merge 0 ranges"),
            ranges.map { it.end() }.maxOrNull() ?: throw IllegalArgumentException("Can't merge 0 ranges")
        )

    fun KotlinFileContext.merge(ranges: Iterable<TextRange>) = inputFileContext.inputFile.merge(ranges)
}
