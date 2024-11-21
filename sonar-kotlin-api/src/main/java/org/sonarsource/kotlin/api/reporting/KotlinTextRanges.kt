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
package org.sonarsource.kotlin.api.reporting

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextPointer
import org.sonar.api.batch.fs.TextRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

object KotlinTextRanges {
    fun InputFile.textRange(psiDocument: Document, element: PsiElement): TextRange =
        textRange(psiDocument, element.textRange.startOffset, element.textRange.endOffset)

    fun InputFile.textRange(psiDocument: Document, startOffset: Int, endOffset: Int): TextRange {
        val startPointer = textPointerAtOffset(psiDocument, startOffset)
        val endPointer = textPointerAtOffset(psiDocument, endOffset)
        return newRange(startPointer.line(), startPointer.lineOffset(), endPointer.line(), endPointer.lineOffset())
    }

    fun InputFile.textPointerAtOffset(psiDocument: Document, startOffset: Int): TextPointer {
        val startLineNumber = psiDocument.getLineNumber(startOffset)
        val startLineNumberOffset = psiDocument.getLineStartOffset(startLineNumber)
        val startLineOffset = startOffset - startLineNumberOffset

        return newPointer(startLineNumber + 1, startLineOffset)
    }

    fun KotlinFileContext.textRange(startOffset: Int, endOffset: Int): TextRange =
        inputFileContext.inputFile.textRange(ktFile.viewProvider.document!!, startOffset, endOffset)

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
