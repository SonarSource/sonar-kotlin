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

import org.jetbrains.kotlin.KtNodeTypes
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtIfExpression
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.frontend.secondaryOf

const val DEFAULT_THRESHOLD = 3

@Rule(key = "S6511")
class MergeIfElseIntoWhenCheck : AbstractCheck() {

    @RuleProperty(
        key = "threshold",
        description = """Number of "if" after which the chain should be replaced by a "when" statement.""",
        defaultValue = DEFAULT_THRESHOLD.toString(),
    )
    var threshold: Int = DEFAULT_THRESHOLD

    override fun visitIfExpression(expression: KtIfExpression, context: KotlinFileContext) {
        if (!isElseElement(expression.parent) && isIfChainLongerThanOrEqualThreshold(expression, threshold)) {
            context.reportIssue(
                expression.ifKeyword,
                """Merge chained "if" statements into a single "when" statement.""",
                collectSecondaryIfStatements(expression).map {
                    context.secondaryOf(it.ifKeyword, """Merge with first "if" statement.""")
                }
            )
        }
    }
}

private fun isElseElement(element: PsiElement) = element.node.elementType === KtNodeTypes.ELSE

private fun isIfChainLongerThanOrEqualThreshold(expression: KtIfExpression, threshold: Int): Boolean {
    var elseBranch = expression.`else`
    var length = 1

    while (elseBranch is KtIfExpression && ++length < threshold) {
        elseBranch = elseBranch.`else`
    }
    return length >= threshold
}

private fun collectSecondaryIfStatements(expression: KtIfExpression) =
    buildList {
        var elseBranch = expression.`else`
        while (elseBranch is KtIfExpression) {
            add(elseBranch)
            elseBranch = elseBranch.`else`
        }
    }
