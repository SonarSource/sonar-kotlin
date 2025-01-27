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

import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.determineSignature
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S1871")
class DuplicateBranchCheck : AbstractBranchDuplication() {

    override fun checkDuplicatedBranches(ctx: KotlinFileContext, tree: KtElement, branches: List<KtElement>) {
        for (group in SyntacticEquivalence.findDuplicatedGroups(branches)) {
            val original = group[0]
            group.asSequence()
                .drop(1)
                .filter { spansMultipleLines(it, ctx) }
                .filter { it !is KtQualifiedExpression || it.hasSameSignature(original as KtQualifiedExpression) }
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

private fun KtQualifiedExpression.hasSameSignature(other: KtQualifiedExpression): Boolean =
    this@hasSameSignature.determineSignature() == other.determineSignature()

private fun spansMultipleLines(tree: KtElement, ctx: KotlinFileContext): Boolean {
    if (tree is KtBlockExpression) {
        val statements = tree.statements
        if (statements.isEmpty()) {
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
