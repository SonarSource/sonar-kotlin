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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.getContent
import org.sonarsource.kotlin.converter.KotlinCodeVerifier
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.impl.TextRanges

/**
 * Replacement for [org.sonarsource.slang.checks.CommentedCodeCheck]
 */
@Rule(key = "S125")
class CommentedCodeCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, kotlinFileContext: KotlinFileContext) {
        val groupedComments = mutableListOf<MutableList<PsiComment>>()
        var currentGroup = mutableListOf<PsiComment>()
        groupedComments.add(currentGroup)
        file.accept(object : KtTreeVisitorVoid() {
            /** Note that [visitComment] not called for [org.jetbrains.kotlin.kdoc.psi.api.KDoc] */
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element !is PsiComment) return
                if (element.tokenType == KtTokens.DOC_COMMENT) return
                if (currentGroup.isNotEmpty() && !areAdjacent(currentGroup.last(), element)) {
                    currentGroup = mutableListOf()
                    groupedComments.add(currentGroup)
                }
                currentGroup.add(element)
            }
        })

        val codeVerifier = KotlinCodeVerifier()
        groupedComments.forEach { comments ->
            val content = comments.joinToString("\n") { it.getContent() }
            if (codeVerifier.containsCode(content)) {
                val textRanges = comments.map { kotlinFileContext.textRange(it) }
                kotlinFileContext.reportIssue(TextRanges.merge(textRanges), "Remove this commented out code.")
            }
        }
    }

    /**
     * Replacement for [org.sonarsource.slang.checks.CommentedCodeCheck.areAdjacent]
     */
    private fun areAdjacent(c1: PsiComment, c2: PsiComment): Boolean {
        val document = c1.containingFile.viewProvider.document!!
        return document.getLineNumber(c1.textRange.startOffset) + 1 == document.getLineNumber(c2.textRange.startOffset)
    }

}
