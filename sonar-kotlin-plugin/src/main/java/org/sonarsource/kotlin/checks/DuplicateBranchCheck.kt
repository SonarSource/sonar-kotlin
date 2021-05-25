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

import org.jetbrains.kotlin.com.intellij.openapi.editor.Document
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtElement
import org.sonar.check.Rule
import org.sonarsource.kotlin.converter.KotlinTextRanges
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.checks.api.SecondaryLocation

/**
 * Replacement for [org.sonarsource.slang.checks.DuplicateBranchCheck]
 */
@Rule(key = "S1871")
class DuplicateBranchCheck : AbstractBranchDuplication() {

    override fun checkDuplicatedBranches(ctx: KotlinFileContext, tree: KtElement, branches: List<KtElement>) {
        val document = ctx.ktFile.viewProvider.document!!
        for (group in SyntacticEquivalence.findDuplicatedGroups(branches)) {
            val original = group[0]
            group.asSequence()
                .drop(1)
                .filter { spansMultipleLines(it, document) }
                .forEach { duplicated ->
                    val originalRange = KotlinTextRanges.textRange(document, original)
                    ctx.reportIssue(
                        duplicated,
                        "This branch's code block is the same as the block for the branch on line ${originalRange.start().line()}.",
                        listOf(SecondaryLocation(originalRange, "Original"))
                    )
                }
        }
    }

    override fun onAllIdenticalBranches(ctx: KotlinFileContext, tree: KtElement) {
        // handled by S3923
    }
}

/**
 * Replacement for [org.sonarsource.slang.checks.DuplicateBranchCheck.spansMultipleLines]
 */
private fun spansMultipleLines(tree: KtElement, document: Document): Boolean {
    if (tree is KtBlockExpression) {
        val statements = tree.statements
        if (statements.isNullOrEmpty()) {
            return false
        }
        val firstStatement = statements[0]
        val lastStatement = statements[statements.size - 1]

        val firstTextRange = KotlinTextRanges.textRange(document, firstStatement)
        val lastTextRange = KotlinTextRanges.textRange(document, lastStatement)

        return firstTextRange.start().line() != lastTextRange.end().line()
    }
    val range = KotlinTextRanges.textRange(document, tree)
    return range.start().line() < range.end().line()
}
