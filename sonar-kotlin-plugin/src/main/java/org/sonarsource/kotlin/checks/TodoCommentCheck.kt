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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.converter.KotlinTextRanges.textPointerAtOffset
import org.sonarsource.kotlin.plugin.KotlinFileContext

val todoPattern = Regex("(?i)(^|[[^\\p{L}]&&\\D])(todo)($|[[^\\p{L}]&&\\D])")

@Rule(key = "S1135")
class TodoCommentCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, kotlinFileContext: KotlinFileContext) {
        file.accept(object : KtTreeVisitorVoid() {
            /** Note that [visitComment] not called for [org.jetbrains.kotlin.kdoc.psi.api.KDoc] */
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element !is PsiComment) {
                    return
                }
                todoPattern.find(element.text)?.let { matchResult ->
                    val todoOffset = element.textOffset + matchResult.groups[2]!!.range.first
                    val document = kotlinFileContext.ktFile.viewProvider.document!!
                    val inputFile = kotlinFileContext.inputFileContext.inputFile
                    val todoRange = inputFile.newRange(
                        inputFile.textPointerAtOffset(document, todoOffset),
                        inputFile.textPointerAtOffset(document, todoOffset + 4)
                    )
                    kotlinFileContext.reportIssue(todoRange, "Complete the task associated to this TODO comment.")
                }
            }
        })
    }

}
