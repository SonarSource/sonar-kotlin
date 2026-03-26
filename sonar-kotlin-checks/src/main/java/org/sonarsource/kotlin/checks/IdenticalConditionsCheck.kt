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

import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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
     * Checks for duplicate conditions in `when` expressions, taking guard conditions into account.
     * Two `when` entries are considered duplicates only if both their primary conditions and their
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
                ConditionWithGuard(condition, entry.getGuard()?.getExpression())
            }

        // Group entries that have the same (condition, guard) pair
        conditionsWithGuards
            .groupBy { ConditionGuardKey(it.condition, it.guard) }
            .values
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

/**
 * Holds a condition expression together with its optional guard expression from a `when` entry.
 */
private data class ConditionWithGuard(val condition: KtElement, val guard: KtExpression?)

/**
 * Key for grouping `when` entry conditions by syntactic equivalence of both the condition and the guard.
 * Two keys are equal if and only if both the condition and the guard are syntactically equivalent.
 */
private data class ConditionGuardKey(val condition: KtElement, val guard: KtExpression?) {
    private val conditionKey = ComparableTree(condition)
    private val guardKey = guard?.let { ComparableTree(it) }

    override fun equals(other: Any?): Boolean {
        if (other !is ConditionGuardKey) return false
        return conditionKey == other.conditionKey && guardKey == other.guardKey
    }

    override fun hashCode(): Int {
        var result = conditionKey.hashCode()
        result = 31 * result + (guardKey?.hashCode() ?: 0)
        return result
    }
}
