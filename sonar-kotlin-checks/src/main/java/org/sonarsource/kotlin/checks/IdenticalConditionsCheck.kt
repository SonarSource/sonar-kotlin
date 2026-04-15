/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.reporting.SecondaryLocation

@Rule(key = "S1862")
class IdenticalConditionsCheck : AbstractCheck() {

    override fun visitWhenExpression(expression: KtWhenExpression, kotlinFileContext: KotlinFileContext) {
        checkWhenConditions(kotlinFileContext, expression)
    }

    override fun visitIfExpression(expression: KtIfExpression, kotlinFileContext: KotlinFileContext) {
        /**
         * Parent of `else if` is [KtContainerNodeForControlStructureBody]
         * whose parent is [KtIfExpression]
         */
        if (expression.parent !is KtIfExpression && expression.parent.parent !is KtIfExpression) {
            checkConditions(kotlinFileContext, collectConditions(expression, mutableListOf()))
        }
    }

    private fun checkConditions(ctx: KotlinFileContext, conditions: List<KtElement>) {
        for (group in SyntacticEquivalence.findDuplicatedGroups(conditions)) {
            reportDuplicates(ctx, group)
        }
    }

    /**
     * Two `when` entries are considered duplicates only if and only if both their primary conditions and their
     * guard expressions (if any) are syntactically equivalent.
     */
    private fun checkWhenConditions(ctx: KotlinFileContext, whenExpression: KtWhenExpression) {
        val conditionsWithGuards = whenExpression.entries
            .filter { !it.isElse && it.conditions.size == 1 }
            .map { entry ->
                val whenCondition = entry.conditions.single()
                val condition = if (whenCondition is KtWhenConditionWithExpression) {
                    whenCondition.expression!!.skipParentheses()
                } else {
                    whenCondition as KtElement
                }
                ConditionWithGuard(condition, entry.guard?.getExpression())
            }

        conditionsWithGuards.groupEquivalent { it1, it2 ->
            SyntacticEquivalence.areEquivalent(it1.condition, it2.condition) &&
                when {
                    it1.guard == null && it2.guard == null -> true
                    it1.guard != null && it2.guard != null -> SyntacticEquivalence.areEquivalent(it1.guard, it2.guard)
                    else -> false
                }
        }
            .filter { it.size > 1 }
            .forEach { group ->
                reportDuplicates(ctx, group.map { it.condition })
            }
    }

    private fun reportDuplicates(ctx: KotlinFileContext, group: List<KtElement>) {
        val original = group[0]
        group.stream().skip(1)
            .forEach { duplicated ->
                val originalRange = ctx.textRange(original)
                ctx.reportIssue(
                    duplicated,
                    "This condition duplicates the one on line ${originalRange.start().line()}.",
                    listOf(SecondaryLocation(originalRange, "Original")),
                )
            }
    }

    private fun collectConditions(ifTree: KtIfExpression, list: MutableList<KtExpression>): List<KtElement> {
        list.add(ifTree.condition!!.skipParentheses())
        val elseBranch = ifTree.`else`
        return if (elseBranch is KtIfExpression) {
            collectConditions(elseBranch, list)
        } else list
    }
}

private data class ConditionWithGuard(val condition: KtElement, val guard: KtExpression?)

private fun <T> Iterable<T>.groupEquivalent(predicate: (T, T) -> Boolean): Iterable<List<T>> {
    val groups = mutableListOf<MutableList<T>>()

    forEach { element ->
        val bucket = groups.firstOrNull { group -> group.any { predicate(element, it) } }
            ?: mutableListOf<T>().apply { groups.add(this) }
        bucket.add(element)
    }

    return groups.map { it.toList() }
}
