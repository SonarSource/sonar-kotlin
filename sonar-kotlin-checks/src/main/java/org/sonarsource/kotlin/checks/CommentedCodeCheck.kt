/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.getContent
import org.sonarsource.kotlin.api.frontend.KotlinCodeVerifier
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.merge
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S125")
class CommentedCodeCheck : AbstractCheck() {


    override fun visitKtFile(file: KtFile, kotlinFileContext: KotlinFileContext) {
        val groupedComments = mutableListOf<MutableList<PsiComment>>()
        var currentGroup = mutableListOf<PsiComment>()
        groupedComments.add(currentGroup)
        file.accept(object : KtTreeVisitorVoid() {
            /** Note that [visitComment] not called for [org.jetbrains.kotlin.kdoc.psi.api.KDoc] */
            override fun visitComment(element: PsiComment) {
                if (currentGroup.isNotEmpty() && !areAdjacent(currentGroup.last(), element)) {
                    currentGroup = mutableListOf()
                    groupedComments.add(currentGroup)
                }
                currentGroup.add(element)
            }
        })

        groupedComments.forEach { comments ->
            if (file.firstChild !in comments) {
                val content = comments.joinToString("\n") { it.getContent() }
                if (KotlinCodeVerifier.containsCode(content)) {
                    val textRanges = comments.map { kotlinFileContext.textRange(it) }
                    kotlinFileContext.reportIssue(kotlinFileContext.merge(textRanges), "Remove this commented out code.")
                }
            }
        }
    }

    private fun areAdjacent(c1: PsiComment, c2: PsiComment): Boolean {
        val document = c1.containingFile.viewProvider.document!!
        return document.getLineNumber(c1.textRange.startOffset) + 1 == document.getLineNumber(c2.textRange.startOffset)
    }
}
