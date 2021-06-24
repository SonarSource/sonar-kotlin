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
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.sonar.api.batch.fs.TextPointer
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.batch.fs.internal.DefaultTextPointer
import org.sonar.api.batch.fs.internal.DefaultTextRange

class CommentAnnotationsAndTokenVisitor(
    private val psiDocument: Document,
) : KtTreeVisitorVoid() {

    companion object {
        private const val MIN_BLOCK_COMMENT_LENGTH = 4
        private const val MIN_DOC_COMMENT_LENGTH = 5
        private const val MIN_LINE_COMMENT_LENGTH = 2
        private const val BLOCK_COMMENT_PREFIX_LENGTH = 2
        private const val BLOCK_COMMENT_SUFFIX_LENGTH = 2
        private const val DOC_COMMENT_PREFIX_LENGTH = 3
        private const val DOC_COMMENT_SUFFIX_LENGTH = 2
        private const val LINE_COMMENT_PREFIX_LENGTH = 2
    }

    val allComments = mutableListOf<Comment>()
    val allAnnotations = mutableListOf<Annotation>()
    val tokens = mutableListOf<Token>()

    override fun visitElement(element: PsiElement) {
        if (element is PsiComment) {
            allComments.add(createComment(element))
        } else if (element is LeafPsiElement && element !is PsiWhiteSpace) {
            when {
                element.elementType is KtKeywordToken -> Token.Type.KEYWORD
                element.elementType === KtTokens.REGULAR_STRING_PART -> Token.Type.STRING_LITERAL
                else -> Token.Type.OTHER
            }.let { type ->
                tokens.add(Token(range(element), element.text, type))
            }
        } else if (element is KtAnnotationEntry) {
            element.shortName?.let { shortName ->
                val argumentsText = element.valueArguments
                    .filterIsInstance<KtValueArgument>()
                    .map { obj: KtValueArgument -> obj.text }
                allAnnotations.add(Annotation(shortName.asString(), argumentsText, range(element)))
            }
        }
        super.visitElement(element)
    }

    private fun createComment(element: PsiComment): Comment {
        val text = element.text
        val tokenType = element.tokenType
        val length = text.length
        val (prefixLength: Int, suffixLength: Int) =
            if (KtTokens.BLOCK_COMMENT == tokenType && length >= MIN_BLOCK_COMMENT_LENGTH) {
                BLOCK_COMMENT_PREFIX_LENGTH to BLOCK_COMMENT_SUFFIX_LENGTH
            } else if (KtTokens.DOC_COMMENT == tokenType && length >= MIN_DOC_COMMENT_LENGTH) {
                DOC_COMMENT_PREFIX_LENGTH to DOC_COMMENT_SUFFIX_LENGTH
            } else if ((KtTokens.EOL_COMMENT == tokenType || KtTokens.SHEBANG_COMMENT == tokenType) && length >= MIN_LINE_COMMENT_LENGTH) {
                LINE_COMMENT_PREFIX_LENGTH to 0
            } else {
                // FIXME error message: unknown comment type
                0 to 0
            }
        val contentText = text.substring(prefixLength, length - suffixLength)
        val range = range(element)
        val contentStart: TextPointer = DefaultTextPointer(range.start().line(), range.start().lineOffset() + prefixLength)
        val contentEnd: TextPointer = DefaultTextPointer(range.end().line(), range.end().lineOffset() - suffixLength)
        val contentRange: TextRange = DefaultTextRange(contentStart, contentEnd)
        return Comment(text, contentText, range, contentRange)
    }

    private fun range(element: PsiElement): TextRange {
        val start = KotlinTextRanges.textPointerAtOffset(psiDocument, element.startOffset)
        val end = KotlinTextRanges.textPointerAtOffset(psiDocument, element.endOffset)
        return DefaultTextRange(start, end)
    }
}

data class Comment(val text: String, val contentText: String, val range: TextRange, val contentRange: TextRange)
data class Token(var textRange: TextRange, val text: String, val type: Type) {
    enum class Type {
        KEYWORD, STRING_LITERAL, OTHER
    }
}

data class Annotation(val shortName: String, val argumentsText: List<String>, val range: TextRange)
