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

import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.determineSignature
import org.sonarsource.kotlin.converter.KotlinTextRanges.textRange
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S1871")
class DuplicateBranchCheck : AbstractBranchDuplication() {

    override fun checkDuplicatedBranches(ctx: KotlinFileContext, tree: KtElement, branches: List<KtElement>) {
        for (group in SyntacticEquivalence.findDuplicatedGroups(branches)) {
            val original = group[0]
            group.asSequence()
                .drop(1)
                .filter { spansMultipleLines(it, ctx) }
                .filter { it !is KtQualifiedExpression || it.hasSameSignature(original as KtQualifiedExpression, ctx.bindingContext) }
                .forEach { duplicated ->
                    val originalRange = ctx.textRange(original)
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

private fun KtQualifiedExpression.hasSameSignature(other: KtQualifiedExpression, bindingContext: BindingContext): Boolean =
    this.determineSignature(bindingContext) == other.determineSignature(bindingContext)


private fun spansMultipleLines(tree: KtElement, ctx: KotlinFileContext): Boolean {
    if (tree is KtBlockExpression) {
        val statements = tree.statements
        if (statements.isNullOrEmpty()) {
            return false
        }
        val firstStatement = statements[0]
        val lastStatement = statements[statements.size - 1]

        val firstTextRange = ctx.textRange(firstStatement)
        val lastTextRange = ctx.textRange(lastStatement)

        return firstTextRange.start().line() != lastTextRange.end().line()
    }
    val range = ctx.textRange(tree)
    return range.start().line() < range.end().line()
}
