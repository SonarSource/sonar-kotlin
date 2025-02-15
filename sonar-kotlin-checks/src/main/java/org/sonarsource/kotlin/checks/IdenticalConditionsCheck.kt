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
        checkConditions(kotlinFileContext, collectConditions(expression))
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
    }

    private fun collectConditions(whenExpression: KtWhenExpression): List<KtElement> =
        whenExpression.entries.mapNotNull {
            it.conditions.map { whenCondition ->
                if (whenCondition is KtWhenConditionWithExpression) {
                    whenCondition.expression!!.skipParentheses()
                } else {
                    whenCondition
                }
            }
        }.filter { it.size == 1 }.map { it.single() }

    private fun collectConditions(ifTree: KtIfExpression, list: MutableList<KtExpression>): List<KtElement> {
        list.add(ifTree.condition!!.skipParentheses())
        val elseBranch = ifTree.`else`
        return if (elseBranch is KtIfExpression) {
            collectConditions(elseBranch, list)
        } else list
    }
}
