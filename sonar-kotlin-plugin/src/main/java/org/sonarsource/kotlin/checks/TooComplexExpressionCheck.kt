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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

/**
 * Replacement for [org.sonarsource.slang.checks.TooComplexExpressionCheck]
 */
@Rule(key = "S1067")
class TooComplexExpressionCheck : AbstractCheck() {

    companion object {
        private const val DEFAULT_MAX_COMPLEXITY = 3
    }

    @RuleProperty(
        key = "max",
        description = "Maximum number of allowed conditional operators in an expression",
        defaultValue = "" + DEFAULT_MAX_COMPLEXITY)
    var max = DEFAULT_MAX_COMPLEXITY

    override fun visitBinaryExpression(expression: KtBinaryExpression, kotlinFileContext: KotlinFileContext) {
        if (!isParentExpression(expression)) return
        val complexity = computeExpressionComplexity(expression)
        if (complexity > max) {
            kotlinFileContext.reportIssue(
                expression,
                "Reduce the number of conditional operators ($complexity) used in the expression (maximum allowed $max).",
                gap = complexity.toDouble() - max,
            )
        }
    }

    /**
     * Replacement for [org.sonarsource.slang.checks.TooComplexExpressionCheck.isParentExpression]
     */
    private fun isParentExpression(expression: KtBinaryExpression): Boolean {
        for (parent in expression.parents) {
            if (parent is KtBinaryExpression) {
                return false
            } else if (parent !is KtUnaryExpression
                // TODO(Godin): seems that instead of logical-or should be logical-and
                || parent !is KtParenthesizedExpression
            ) {
                return true
            }
        }
        return true
    }

    private fun computeExpressionComplexity(expression: KtExpression): Int {
        return when (val e = expression.skipParentheses()) {
            is KtBinaryExpression -> {
                val operator = e.operationReference.operationSignTokenType
                val complexity = if (operator == KtTokens.OROR || operator == KtTokens.ANDAND) 1 else 0
                complexity +
                    computeExpressionComplexity(e.left!!) +
                    computeExpressionComplexity(e.right!!)
            }
            is KtUnaryExpression -> computeExpressionComplexity(e.baseExpression!!)
            else -> 0
        }
    }

}
