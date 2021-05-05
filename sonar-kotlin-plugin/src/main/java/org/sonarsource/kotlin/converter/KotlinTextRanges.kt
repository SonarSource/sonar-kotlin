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
package org.sonarsource.kotlin.converter

import org.jetbrains.kotlin.com.intellij.openapi.editor.Document
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.sonarsource.slang.api.TextPointer
import org.sonarsource.slang.api.TextRange
import org.sonarsource.slang.impl.TextPointerImpl
import org.sonarsource.slang.impl.TextRangeImpl

object KotlinTextRanges {
    @JvmStatic
    fun textRange(psiDocument: Document, element: PsiElement): TextRange {
        val startPointer = textPointerAtOffset(psiDocument, element.textRange.startOffset)
        val endPointer = textPointerAtOffset(psiDocument, element.textRange.endOffset)
        return TextRangeImpl(startPointer, endPointer)
    }

    fun textPointerAtOffset(psiDocument: Document, startOffset: Int): TextPointer {
        val startLineNumber = psiDocument.getLineNumber(startOffset)
        val startLineNumberOffset = psiDocument.getLineStartOffset(startLineNumber)
        val startLineOffset = startOffset - startLineNumberOffset
        return TextPointerImpl(startLineNumber + 1, startLineOffset)
    }

}
