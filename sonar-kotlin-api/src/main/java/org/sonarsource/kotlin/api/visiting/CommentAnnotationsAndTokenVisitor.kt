/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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
package org.sonarsource.kotlin.api.visiting

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextRange
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textPointerAtOffset

class CommentAnnotationsAndTokenVisitor(
    private val psiDocument: Document,
    private val inputFile: InputFile,
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
        when {
            element is PsiComment -> {
                allComments.add(createComment(element))
            }
            element is LeafPsiElement && element !is PsiWhiteSpace -> {
                when {
                    element.elementType is KtKeywordToken -> Token.Type.KEYWORD
                    element.elementType === KtTokens.REGULAR_STRING_PART -> Token.Type.STRING_LITERAL
                    else -> Token.Type.OTHER
                }.let { type ->
                    tokens.add(Token(range(element), element.text, type))
                }
            }
            element is KtAnnotationEntry -> {
                element.shortName?.let { shortName ->
                    val argumentsText = element.valueArguments
                        .filterIsInstance<KtValueArgument>()
                        .map { obj: KtValueArgument -> obj.text }
                    allAnnotations.add(Annotation(shortName.asString(), argumentsText, range(element)))
                }
            }
        }
        super.visitElement(element)
    }

    private fun createComment(element: PsiComment): Comment {
        val text = element.text
        val tokenType = element.tokenType
        val length = text.length
        val (prefixLength: Int, suffixLength: Int) =
            when {
                KtTokens.BLOCK_COMMENT == tokenType && length >= MIN_BLOCK_COMMENT_LENGTH -> {
                    BLOCK_COMMENT_PREFIX_LENGTH to BLOCK_COMMENT_SUFFIX_LENGTH
                }
                KtTokens.DOC_COMMENT == tokenType && length >= MIN_DOC_COMMENT_LENGTH -> {
                    DOC_COMMENT_PREFIX_LENGTH to DOC_COMMENT_SUFFIX_LENGTH
                }
                (KtTokens.EOL_COMMENT == tokenType || KtTokens.SHEBANG_COMMENT == tokenType) && length >= MIN_LINE_COMMENT_LENGTH -> {
                    LINE_COMMENT_PREFIX_LENGTH to 0
                }
                else -> {
                    // FIXME error message: unknown comment type
                    0 to 0
                }
            }
        val contentText = text.substring(prefixLength, length - suffixLength)
        val range = range(element)
        return Comment(text, contentText, range)
    }

    private fun range(element: PsiElement): TextRange {
        val start = inputFile.textPointerAtOffset(psiDocument, element.startOffset)
        val end = inputFile.textPointerAtOffset(psiDocument, element.endOffset)
        return inputFile.newRange(start, end)
    }
}

data class Comment(val text: String, val contentText: String, val range: TextRange)
data class Token(var textRange: TextRange, val text: String, val type: Type) {
    enum class Type {
        KEYWORD, STRING_LITERAL, OTHER
    }
}

data class Annotation(val shortName: String, val argumentsText: List<String>, val range: TextRange)
