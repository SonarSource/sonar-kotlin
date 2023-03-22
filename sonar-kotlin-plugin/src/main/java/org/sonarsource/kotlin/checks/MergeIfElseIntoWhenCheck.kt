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

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtIfExpression
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

const val DEFAULT_THRESHOLD = 2

@Rule(key = "S6511")
class MergeIfElseIntoWhenCheck : AbstractCheck() {

    @RuleProperty(
        key = "threshold",
        description = """Number of chained "if" statements after which should be merged.""",
        defaultValue = DEFAULT_THRESHOLD.toString(),
    )
    var threshold: Int = DEFAULT_THRESHOLD

    override fun visitIfExpression(expression: KtIfExpression, context: KotlinFileContext) {
        val isHeadOfIfChain = !isElseElement(expression.parent)
        if (isHeadOfIfChain && isIfChainLongerThanOrEqualThreshold(expression, threshold)) {
            context.reportIssue(expression.ifKeyword, """Merge chained "if" statements into a single "when" statement.""")
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
