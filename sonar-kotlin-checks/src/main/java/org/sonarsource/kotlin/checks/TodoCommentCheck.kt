/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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
package org.sonarsource.kotlin.checks

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textPointerAtOffset
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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
