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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S1940")
class BooleanInversionCheck : AbstractCheck() {
    companion object {
        private val OPERATORS = mapOf(
            KtTokens.EQEQ to "!=",
            KtTokens.EXCLEQ to "==",
            KtTokens.EQEQEQ to "!==",
            KtTokens.EXCLEQEQEQ to "===",
            KtTokens.LT to ">=",
            KtTokens.GT to "<=",
            KtTokens.LTEQ to ">",
            KtTokens.GTEQ to "<",
        )
    }

    override fun visitUnaryExpression(expression: KtUnaryExpression, context: KotlinFileContext) {
        if (expression.operationToken != KtTokens.EXCL) return
        when (val innerExpression = expression.baseExpression!!.skipParentheses()) {
            is KtBinaryExpression -> {
                val oppositeOperator = OPERATORS[innerExpression.operationToken]
                if (oppositeOperator != null) {
                    context.reportIssue(expression, """Use the opposite operator ("$oppositeOperator") instead.""")
                }
            }
            is KtIsExpression -> {
                val oppositeOperator = if (innerExpression.isNegated) "is" else "!is"
                context.reportIssue(expression, """Use the opposite operator ("$oppositeOperator") instead.""")
            }
        }
    }
}
