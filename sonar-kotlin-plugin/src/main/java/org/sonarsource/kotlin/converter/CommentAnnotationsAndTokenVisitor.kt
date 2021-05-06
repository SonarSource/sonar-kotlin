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
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.slang.api.Annotation
import org.sonarsource.slang.api.Comment
import org.sonarsource.slang.api.TextPointer
import org.sonarsource.slang.api.TextRange
import org.sonarsource.slang.api.Token
import org.sonarsource.slang.impl.AnnotationImpl
import org.sonarsource.slang.impl.CommentImpl
import org.sonarsource.slang.impl.TextPointerImpl
import org.sonarsource.slang.impl.TextRangeImpl
import org.sonarsource.slang.impl.TokenImpl

class CommentAnnotationsAndTokenVisitor(private val psiDocument: Document) : KtTreeVisitorVoid() {

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
                tokens.add(TokenImpl(textRange(psiDocument, element), element.text, type))
            }
        } else if (element is KtAnnotationEntry) {
            element.shortName?.let { shortName ->
                val argumentsText = element.valueArguments
                    .filterIsInstance<KtValueArgument>()
                    .map { obj: KtValueArgument -> obj.text }
                val range = textRange(psiDocument, element)
                allAnnotations.add(AnnotationImpl(shortName.asString(), argumentsText, range))
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
        val range = textRange(psiDocument, element)
        val contentStart: TextPointer = TextPointerImpl(range.start().line(), range.start().lineOffset() + prefixLength)
        val contentEnd: TextPointer = TextPointerImpl(range.end().line(), range.end().lineOffset() - suffixLength)
        val contentRange: TextRange = TextRangeImpl(contentStart, contentEnd)
        return CommentImpl(text, contentText, range, contentRange)
    }
}
