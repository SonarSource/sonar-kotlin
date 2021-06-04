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
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext
import java.util.BitSet

/**
 * Replacement for [org.sonarsource.slang.checks.TooManyLinesOfCodeFileCheck]
 */
@Rule(key = "S104")
class TooManyLinesOfCodeFileCheck : AbstractCheck() {
    companion object {
        const val DEFAULT_MAX = 1000
    }

    @RuleProperty(
        key = "max",
        description = "Maximum authorized lines of code in a file.",
        defaultValue = "" + DEFAULT_MAX,
    )
    var max: Int = DEFAULT_MAX

    override fun visitKtFile(file: KtFile, kotlinFileContext: KotlinFileContext) {
        val numberOfLinesOfCode = numberOfLinesOfCode(file, kotlinFileContext)
        if (numberOfLinesOfCode > max) {
            kotlinFileContext.reportIssue(
                null,
                // TODO add filename to the message
                "File has $numberOfLinesOfCode lines, which is greater than $max authorized. Split it into smaller files.")
        }
    }

    /**
     * Replacement for [org.sonarsource.slang.impl.TreeMetaDataProvider.TreeMetaDataImpl.computeLinesOfCode]
     */
    private fun numberOfLinesOfCode(element: PsiElement, kotlinFileContext: KotlinFileContext): Int {
        val lines = BitSet()
        val document = kotlinFileContext.ktFile.viewProvider.document!!
        element.accept(object : KtTreeVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element is LeafPsiElement && element !is PsiWhiteSpace && element !is PsiComment) {
                    lines.set(document.getLineNumber(element.textRange.startOffset))
                }
            }
        })
        return lines.cardinality()
    }

}
