/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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
