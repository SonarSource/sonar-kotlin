/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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

val fixMePattern = Regex("(?i)(^|[[^\\p{L}]&&\\D])(fixme)($|[[^\\p{L}]&&\\D])")

@Rule(key = "S1134")
class FixMeCommentCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, kotlinFileContext: KotlinFileContext) {
        file.accept(object : KtTreeVisitorVoid() {
            /** Note that [visitComment] not called for [org.jetbrains.kotlin.kdoc.psi.api.KDoc] */
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element !is PsiComment) {
                    return
                }
                fixMePattern.find(element.text)?.let { matchResult ->
                    val fixmeOffset = element.textOffset + matchResult.groups[2]!!.range.first
                    val document = kotlinFileContext.ktFile.viewProvider.document!!
                    val inputFile = kotlinFileContext.inputFileContext.inputFile
                    val fixmeRange = inputFile.newRange(
                        inputFile.textPointerAtOffset(document, fixmeOffset),
                        inputFile.textPointerAtOffset(document, fixmeOffset + 5)
                    )
                    kotlinFileContext.reportIssue(
                        fixmeRange,
                        """Take the required action to fix the issue indicated by this "FIXME" comment."""
                    )
                }
            }
        })
    }

}
